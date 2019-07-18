package de.peass.testtransformation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
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
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import de.peass.dependency.ClazzFinder;
import de.peass.dependency.KiekerResultManager;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.changesreading.FileComparisonUtil;

public class JUnitTestShortener {

   private static final Logger LOG = LogManager.getLogger(JUnitTestShortener.class);

   private JUnitTestTransformer transformer;
   final File module; final ChangedEntity callee; final String method;

   public JUnitTestShortener(JUnitTestTransformer transformer, final File module, final ChangedEntity callee, final String method) {
      this.transformer = transformer;
      this.module = module;
      this.callee = callee;
      this.method = method;
   }

   private final Map<File, File> lastShortenedMap = new HashMap<>();
   private final Set<File> subclasses = new HashSet<>();

   public void shortenTest() {
      if (!lastShortenedMap.isEmpty()) {
         throw new RuntimeException("Only use TestShortener once!");
      }
      final File calleeClazzFile = ClazzFinder.getClazzFile(module, callee);
      if (calleeClazzFile != null) {
         try {
            final File tempFile = Files.createTempFile("Temp", ".java").toFile();
            FileUtils.copyFile(calleeClazzFile, tempFile);
            lastShortenedMap.put(tempFile, calleeClazzFile);

            shortenTestClazz(callee, calleeClazzFile);

            for (File subclass : subclasses) {
               if (!lastShortenedMap.containsValue(subclass)) {
                  // A rather dirty hack..
                  final ChangedEntity callee = new ChangedEntity(subclass.getName().replaceAll(".java", ""), this.callee.getModule());
                  LOG.debug("Shortening: " + callee);
                  shortenTestClazz(callee, subclass);
               }
            }
         } catch (IOException e1) {
            e1.printStackTrace();
         }
      }
   }

   private void shortenTestClazz(ChangedEntity callee, final File calleeClazzFile) throws IOException {
      final int version = transformer.getVersion(calleeClazzFile);

      if (version != 0) {
         final CompilationUnit calleeUnit = transformer.getLoadedFiles().get(calleeClazzFile);
         final ClassOrInterfaceDeclaration clazz = FileComparisonUtil.findClazz(callee, calleeUnit.getChildNodes());

         shortenParent(module, callee, calleeClazzFile, calleeUnit, clazz);
         removeNonWanted(method, version, clazz);

         FileUtils.writeStringToFile(calleeClazzFile, calleeUnit.toString(), Charset.defaultCharset());
      }
   }

   public void shortenParent(final File module, final ChangedEntity callee, final File calleeClazzFile, final CompilationUnit calleeUnit, final ClassOrInterfaceDeclaration clazz) throws IOException {
      LOG.debug("Shortening: {}", callee);
      if (clazz.getExtendedTypes().size() > 0) {
         ChangedEntity parentEntity = getParentEntity(callee, calleeUnit, clazz);
         final File parentClazzFile = ClazzFinder.getClazzFile(module, parentEntity);
         if (parentClazzFile != null) {
            shortenTestClazz(parentEntity, parentClazzFile);
         }
         
         final int version = transformer.getVersion(calleeClazzFile);
         if (version == 3 || version == 34) {
            String simpleClazzName = callee.getSimpleClazzName();
            addSubclasses(simpleClazzName);
         }
      }
   }

   public void addSubclasses(String simpleClazzName) {
      List<File> thisSubclasses = transformer.getExtensions().get(simpleClazzName);
      if (thisSubclasses != null) {
         LOG.debug("Must shorten suclasses: {}", thisSubclasses);
         subclasses.addAll(thisSubclasses);
         for (File subclass : thisSubclasses) {
            String subsubclass = subclass.getName().substring(0, subclass.getName().lastIndexOf('.'));
            addSubclasses(subsubclass);
         }
      }
   }

   public ChangedEntity getParentEntity(final ChangedEntity callee, final CompilationUnit calleeUnit, final ClassOrInterfaceDeclaration clazz) {
      ChangedEntity parentEntity = null;
      for (ClassOrInterfaceType parent : clazz.getExtendedTypes()) {
         LOG.debug("Must also shorten " + parent);
         String parentName = parent.getName().toString();
         String fqn = findFQN(calleeUnit, parentName);
         if (fqn == parentName) {
            fqn = callee.getPackage() + "." + parentName;
         }
         parentEntity = new ChangedEntity(fqn, callee.getModule());
      }
      return parentEntity;
   }

   private void removeNonWanted(final String method, final int version, final ClassOrInterfaceDeclaration clazz) {
      List<Node> remove = new LinkedList<>();
      for (MethodDeclaration methodDeclaration : clazz.getMethods()) {
         if (!methodDeclaration.getNameAsString().equals(method) && methodDeclaration.getModifiers().contains(Modifier.publicModifier()) &&
               methodDeclaration.getParameters().size() == 0) {
            if (version != 4) {
               if (methodDeclaration.getNameAsString().contains("test")) {
                  remove.add(methodDeclaration);
               }
            } else {
               Optional<AnnotationExpr> testAnnotation = methodDeclaration.getAnnotationByName("Test");
               Optional<AnnotationExpr> testAnnotation2 = methodDeclaration.getAnnotationByName("org.junit.Test");
               if (testAnnotation.isPresent()) {
                  methodDeclaration.getAnnotations().remove(testAnnotation.get());
               }
               if (testAnnotation2.isPresent()) {
                  methodDeclaration.getAnnotations().remove(testAnnotation2.get());
               }
            }
         }
      }
      for (Node removeN : remove) {
         clazz.remove(removeN);
      }
   }

   public String findFQN(final CompilationUnit calleeUnit, String parentName) {
      String fqn = parentName;
      for (ImportDeclaration importDecl : calleeUnit.getImports()) {
         if (importDecl.getNameAsString().endsWith(parentName)) {
            fqn = importDecl.getNameAsString();
         }
      }
      return fqn;
   }

   public void resetShortenedFile() {
      if (lastShortenedMap != null) {
         for (Map.Entry<File, File> shortened : lastShortenedMap.entrySet()) {
            try {
//               FileUtils.copyFile(shortened.getKey(), shortened.getValue());
               shortened.getKey().renameTo(shortened.getValue());
               final CompilationUnit unit = FileComparisonUtil.parse(shortened.getValue());
               transformer.getLoadedFiles().put(shortened.getValue(), unit);
            } catch (IOException e) {
               e.printStackTrace();
            }

         }
      }
   }
}
