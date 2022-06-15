package de.dagere.peass.ci;

import java.io.File;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.ClazzFileFinder;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.testtransformation.ParseUtil;

public class NonIncludedByRule {
   public static boolean isTestIncluded(TestCase test, JUnitTestTransformer transformer) {
      ExecutionConfig executionConfig = transformer.getConfig().getExecutionConfig();
      ClazzFileFinder finder = new ClazzFileFinder(executionConfig);
      final File clazzFile = finder.getClazzFile(transformer.getProjectFolder(), test);
      CompilationUnit unit = transformer.getLoadedFiles().get(clazzFile);
      
      boolean includedByRule = isIncluded(executionConfig, unit);
      
      boolean excludeByRule = isExcluded(executionConfig, unit);
      
      return includedByRule && !excludeByRule;
   }

   private static boolean isExcluded(ExecutionConfig executionConfig, CompilationUnit unit) {
      boolean excludeByRule = false;
      if (executionConfig.getExcludeByRule().size() != 0) {
         for (ClassOrInterfaceDeclaration clazz : ParseUtil.getClasses(unit)) {
            for (FieldDeclaration declaration : clazz.getFields()) {
               String fieldType = declaration.getElementType().toString();
               if (executionConfig.getExcludeByRule().contains(fieldType)) {
                  excludeByRule = true;
               }
            }
         }
      }
      return excludeByRule;
   }

   private static boolean isIncluded(ExecutionConfig executionConfig, CompilationUnit unit) {
      boolean includedByRule = executionConfig.getIncludeByRule().size() == 0;
      if (!includedByRule) {
         for (ClassOrInterfaceDeclaration clazz : ParseUtil.getClasses(unit)) {
            for (FieldDeclaration declaration : clazz.getFields()) {
               String fieldType = declaration.getElementType().toString();
               if (executionConfig.getIncludeByRule().contains(fieldType)) {
                  includedByRule = true;
               }
            }
         }
      }
      return includedByRule;
   }
}
