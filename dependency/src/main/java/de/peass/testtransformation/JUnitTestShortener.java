package de.peass.testtransformation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import de.peass.dependency.ClazzFinder;
import de.peass.dependency.TestResultManager;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.changesreading.FileComparisonUtil;

public class JUnitTestShortener {

   private static final Logger LOG = LogManager.getLogger(JUnitTestShortener.class);

   private JUnitTestTransformer transformer;

   public JUnitTestShortener(JUnitTestTransformer transformer) {
      this.transformer = transformer;
   }

   private Map<File, File> lastShortenedMap = null;

   public void shortenTest(final File module, final ChangedEntity callee, final String method) {
      lastShortenedMap = new HashMap<>();

      final File calleeClazzFile = ClazzFinder.getClazzFile(module, callee);
      if (calleeClazzFile != null) {
         try {
            File tempFile = Files.createTempFile("Temp", ".java").toFile();
            FileUtils.copyFile(calleeClazzFile, tempFile);
            lastShortenedMap.put(tempFile, calleeClazzFile);
            
            shortenTestClazz(module, callee, method, calleeClazzFile);
            
         } catch (IOException e1) {
            e1.printStackTrace();
         }

      }
   }

   private void shortenTestClazz(final File module, final ChangedEntity callee, final String method, final File calleeClazzFile) throws IOException {
      final int version = transformer.getVersion(calleeClazzFile);
      
      final CompilationUnit calleeUnit = transformer.getLoadedFiles().get(calleeClazzFile);
      final ClassOrInterfaceDeclaration clazz = FileComparisonUtil.findClazz(callee, calleeUnit.getChildNodes());

      shortenParent(module, callee, calleeUnit, clazz); 
      removeNonWanted(method, version, clazz);

      FileUtils.writeStringToFile(calleeClazzFile, calleeUnit.toString(), Charset.defaultCharset());
   }

   public void shortenParent(final File module, final ChangedEntity callee, final CompilationUnit calleeUnit, final ClassOrInterfaceDeclaration clazz) throws IOException {
      if (clazz.getExtendedTypes().size() > 0) {
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
         final File parentClazzFile = ClazzFinder.getClazzFile(module, parentEntity);
         if (parentClazzFile != null) {
            shortenTestClazz(module, parentEntity, "", parentClazzFile);
         }
      }
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
               if (methodDeclaration.getAnnotationByName("Test").isPresent() || methodDeclaration.getAnnotationByName("org.junit.Test").isPresent()) {
                  remove.add(methodDeclaration);
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
               FileUtils.copyFile(shortened.getKey(), shortened.getValue());
               final CompilationUnit unit = FileComparisonUtil.parse(shortened.getValue());
               transformer.getLoadedFiles().put(shortened.getValue(), unit);
            } catch (IOException e) {
               e.printStackTrace();
            }
            
         }
      }
      
//      if (lastShortened != null) {
//         try {
//            FileUtils.copyFile(lastShortened, lastFile);
//            final CompilationUnit unit = FileComparisonUtil.parse(lastFile);
//            transformer.getLoadedFiles().put(lastFile, unit);
//         } catch (IOException e) {
//            e.printStackTrace();
//         }
//      }
      lastShortenedMap = null;
//      lastShortened = null;
   }
}
