package de.dagere.peass.ci;

import java.awt.image.AffineTransformOp;
import java.io.File;
import java.util.List;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.ClazzFileFinder;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.changesreading.FQNDeterminer;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.testtransformation.ParseUtil;

public class NonIncludedByRule {
   
   public static boolean isTestIncluded(TestCase test, JUnitTestTransformer transformer) {
      ExecutionConfig executionConfig = transformer.getConfig().getExecutionConfig();
      CompilationUnit unit = getUnit(test, transformer, executionConfig);

      boolean includedByRule = isIncluded(executionConfig, unit);

      boolean excludeByRule = isExcluded(executionConfig, unit);

      boolean currentTestSelected = includedByRule && !excludeByRule;

      boolean parentExisting = false;
      boolean parentTestIncluded = false;
      boolean parentTestExcluded = false;
      for (ClassOrInterfaceDeclaration clazz : ParseUtil.getClasses(unit)) {
         if (clazz.getExtendedTypes().size() == 1) {
            parentExisting = true;
            String extendType = clazz.getExtendedTypes(0).getNameAsString();
            String fqn = FQNDeterminer.getParameterFQN(unit, extendType);
            ChangedEntity entity = new ChangedEntity(fqn);
            TestCase parentTest = new TestCase(entity);
            CompilationUnit parentUnit = getUnit(parentTest, transformer, executionConfig);
            if (isIncluded(executionConfig, parentUnit)) {
               parentTestIncluded = true;
            }
            if (isExcluded(executionConfig, parentUnit)) {
               parentTestExcluded = true;
            }
         }
      }
      if (!parentExisting) {
         return currentTestSelected;
      } else {
         return (currentTestSelected && !parentTestExcluded) || (parentTestIncluded && !parentTestExcluded);
      }
   }

   private static CompilationUnit getUnit(TestCase test, JUnitTestTransformer transformer, ExecutionConfig executionConfig) {
      ClazzFileFinder finder = new ClazzFileFinder(executionConfig);
      final File clazzFile = finder.getClazzFile(transformer.getProjectFolder(), test);

      CompilationUnit unit = transformer.getLoadedFiles().get(clazzFile);
      return unit;
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
