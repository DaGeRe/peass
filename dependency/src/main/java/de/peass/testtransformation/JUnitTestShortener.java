package de.peass.testtransformation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

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

   private File lastShortened = null;
   private File lastFile = null;

   public void shortenClazz(final File module, final ChangedEntity callee, final String method) {
      final File calleeClazzFile = ClazzFinder.getClazzFile(module, callee);
      if (calleeClazzFile != null) {
         final int version = transformer.getVersion(calleeClazzFile);

         try {
            lastShortened = Files.createTempFile("Temp", ".java").toFile();
            FileUtils.copyFile(calleeClazzFile, lastShortened);
            lastFile = calleeClazzFile;
         } catch (IOException e1) {
            e1.printStackTrace();
         }

         final CompilationUnit calleeUnit = transformer.getLoadedFiles().get(calleeClazzFile);
         final ClassOrInterfaceDeclaration clazz = FileComparisonUtil.findClazz(callee, calleeUnit.getChildNodes());

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

         try {
            FileUtils.writeStringToFile(calleeClazzFile, calleeUnit.toString(), Charset.defaultCharset());
         } catch (final IOException e) {
            e.printStackTrace();
         }
      } else {
         LOG.error("Class {} does not exist in current version", callee);
      }

   }

   public void resetShortenedFile() {
      if (lastShortened != null) {
         try {
            FileUtils.copyFile(lastShortened, lastFile);
            final CompilationUnit unit = FileComparisonUtil.parse(lastFile);
            transformer.getLoadedFiles().put(lastFile, unit);
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      lastFile = null;
      lastShortened = null;
   }
}
