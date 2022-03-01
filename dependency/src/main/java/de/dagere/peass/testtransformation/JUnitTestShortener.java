package de.dagere.peass.testtransformation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import de.dagere.peass.dependency.ClazzFileFinder;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.changesreading.ClazzFinder;
import de.dagere.peass.dependency.changesreading.JavaParserProvider;

public class JUnitTestShortener implements AutoCloseable {

   private static final Logger LOG = LogManager.getLogger(JUnitTestShortener.class);

   private JUnitTestTransformer transformer;
   private final File module;
   private final ChangedEntity callee;
   private final String method;

   private final Map<File, File> lastShortenedMap = new HashMap<>();
   private final Set<File> superclasses = new HashSet<>();

   public JUnitTestShortener(final JUnitTestTransformer transformer, final File module, final ChangedEntity callee, final String method) {
      this.transformer = transformer;
      this.module = module;
      this.callee = callee;
      this.method = method;
      shortenTest();
   }

   private void shortenTest() {
      if (!lastShortenedMap.isEmpty()) {
         throw new RuntimeException("Only use TestShortener once!");
      }
      ClazzFileFinder finder = new ClazzFileFinder(transformer.getConfig().getExecutionConfig());
      final File calleeClazzFile = finder.getClazzFile(module, callee);
      if (calleeClazzFile != null) {
         try {

            shortenTestClazz(callee, calleeClazzFile);

            for (final File superclass : superclasses) {
               if (!lastShortenedMap.containsValue(superclass)) {
                  // A rather dirty hack..
                  final ChangedEntity callee = new ChangedEntity(superclass.getName().replaceAll(".java", ""), this.callee.getModule());
                  LOG.debug("Shortening: " + callee);
                  shortenTestClazz(callee, superclass);
               }
            }
         } catch (final IOException e1) {
            e1.printStackTrace();
         }
      } else {
         throw new RuntimeException("Could not find " + callee.getModule() + " " + callee.getClazz() + " java file in " + module + " - maybe package missing?");
      }
   }

   private void shortenTestClazz(final ChangedEntity callee, final File calleeClazzFile) throws IOException {
      final int version = transformer.getVersion(calleeClazzFile);

      if (version != 0) {
         saveUnshortened(calleeClazzFile);

         final CompilationUnit calleeUnit = transformer.getLoadedFiles().get(calleeClazzFile);
         final TypeDeclaration<?> clazz = ClazzFinder.findClazz(callee, calleeUnit.getChildNodes());

         // The clazz might be null, if it is
         if (clazz != null && clazz instanceof ClassOrInterfaceDeclaration) {
            shortenParent(module, callee, calleeClazzFile, calleeUnit, (ClassOrInterfaceDeclaration) clazz);
            removeNonWanted(method, version, (ClassOrInterfaceDeclaration) clazz);

            FileUtils.writeStringToFile(calleeClazzFile, calleeUnit.toString(), Charset.defaultCharset());
         }

      }
   }

   private void saveUnshortened(final File calleeClazzFile) throws IOException {
      final File tempFile = Files.createTempFile("Temp", ".java").toFile();
      FileUtils.copyFile(calleeClazzFile, tempFile);
      lastShortenedMap.put(tempFile, calleeClazzFile);
   }

   public void shortenParent(final File module, final ChangedEntity callee, final File calleeClazzFile, final CompilationUnit calleeUnit, final ClassOrInterfaceDeclaration clazz)
         throws IOException {
      LOG.debug("Shortening: {}", callee);
      if (clazz.getExtendedTypes().size() > 0) {
         final ChangedEntity parentEntity = getParentEntity(callee, calleeUnit, clazz);
         ClazzFileFinder finder = new ClazzFileFinder(transformer.getConfig().getExecutionConfig());
         final File parentClazzFile = finder.getClazzFile(module, parentEntity);
         if (parentClazzFile != null) {
            shortenTestClazz(parentEntity, parentClazzFile);
         }

         final int version = transformer.getVersion(calleeClazzFile);
         if (version == 3 || version == 34) {
            final String simpleClazzName = callee.getSimpleClazzName();
            addSuperclasses(simpleClazzName);
         }
      }
   }

   public void addSuperclasses(final String simpleClazzName) {
      final List<File> thisSuperclasses = transformer.getExtensions().get(simpleClazzName);
      if (thisSuperclasses != null) {
         LOG.debug("Must shorten suclasses: {}", thisSuperclasses);
         superclasses.addAll(thisSuperclasses);
         for (final File superclass : thisSuperclasses) {
            final String supersuperclass = superclass.getName().substring(0, superclass.getName().lastIndexOf('.'));
            addSuperclasses(supersuperclass);
         }
      }
   }

   public ChangedEntity getParentEntity(final ChangedEntity callee, final CompilationUnit calleeUnit, final ClassOrInterfaceDeclaration clazz) {
      ChangedEntity parentEntity = null;
      for (final ClassOrInterfaceType parent : clazz.getExtendedTypes()) {
         LOG.debug("Must also shorten {} Package: {}", parent, callee.getPackage());
         final String parentName = parent.getName().toString();
         String fqn = findFQN(calleeUnit, parentName);
         if (fqn == parentName) {
            if (callee.getPackage().equals("")) {
               fqn = parentName;
            } else {
               fqn = callee.getPackage() + "." + parentName;
            }
         }
         parentEntity = new ChangedEntity(fqn, callee.getModule());
      }
      return parentEntity;
   }

   private void removeNonWanted(final String method, final int version, final ClassOrInterfaceDeclaration clazz) {
      final List<Node> remove = new LinkedList<>();
      for (final MethodDeclaration methodDeclaration : clazz.getMethods()) {
         if (!methodDeclaration.getNameAsString().equals(method)) {
            if (methodDeclaration.getModifiers().contains(Modifier.publicModifier()) &&
                  methodDeclaration.getParameters().size() == 0) {
               if (version == 3) {
                  if (methodDeclaration.getNameAsString().contains("test")) {
                     remove.add(methodDeclaration);
                  }
               } else {
                  removeTestAnnotations(methodDeclaration);
               }
            }
            removeParameterizedTestAnnotations(methodDeclaration);
         }
      }
      for (final Node removeN : remove) {
         clazz.remove(removeN);
      }
   }

   private void removeParameterizedTestAnnotations(final MethodDeclaration methodDeclaration) {
      final Optional<AnnotationExpr> parameterizedTestAnnotation = methodDeclaration.getAnnotationByName("ParameterizedTest");
      final Optional<AnnotationExpr> testAnnotationFQNJunit5Parameterized = methodDeclaration.getAnnotationByName("org.junit.jupiter.api.ParameterizedTest");
      if (parameterizedTestAnnotation.isPresent()) {
         methodDeclaration.getAnnotations().remove(parameterizedTestAnnotation.get());
      }
      if (testAnnotationFQNJunit5Parameterized.isPresent()) {
         methodDeclaration.getAnnotations().remove(testAnnotationFQNJunit5Parameterized.get());
      }
   }

   private void removeTestAnnotations(final MethodDeclaration methodDeclaration) {
      final Optional<AnnotationExpr> testAnnotation = methodDeclaration.getAnnotationByName("Test");
      final Optional<AnnotationExpr> testAnnotationFQNJUnit4 = methodDeclaration.getAnnotationByName("org.junit.Test");
      final Optional<AnnotationExpr> testAnnotationFQNJunit5 = methodDeclaration.getAnnotationByName("org.junit.jupiter.api.Test");
      if (testAnnotation.isPresent()) {
         methodDeclaration.getAnnotations().remove(testAnnotation.get());
      }
      if (testAnnotationFQNJUnit4.isPresent()) {
         methodDeclaration.getAnnotations().remove(testAnnotationFQNJUnit4.get());
      }
      if (testAnnotationFQNJunit5.isPresent()) {
         methodDeclaration.getAnnotations().remove(testAnnotationFQNJunit5.get());
      }
   }

   public String findFQN(final CompilationUnit calleeUnit, final String parentName) {
      String fqn = parentName;
      for (final ImportDeclaration importDecl : calleeUnit.getImports()) {
         if (importDecl.getNameAsString().endsWith(parentName)) {
            fqn = importDecl.getNameAsString();
         }
      }
      return fqn;
   }

   private void resetShortenedFile() {
      if (lastShortenedMap != null) {
         for (final Map.Entry<File, File> shortened : lastShortenedMap.entrySet()) {
            try {
               final File destFile = shortened.getValue();
               destFile.delete();
               LOG.debug("File to reset: {} Exists: {} Parent exists: {}", destFile, destFile.exists(), destFile.getParentFile().exists());
               Path dest = destFile.toPath();
               Path source = shortened.getKey().toPath();
               Files.move(source, dest);
               final CompilationUnit unit = JavaParserProvider.parse(destFile);
               transformer.getLoadedFiles().put(destFile, unit);
            } catch (final IOException e) {
               e.printStackTrace();
            }

         }
      }
   }

   @Override
   public void close() throws Exception {
      resetShortenedFile();
   }
}
