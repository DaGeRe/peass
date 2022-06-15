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

   private static class IncludeExcludeInfo {
      private final boolean included, excluded;

      public IncludeExcludeInfo(boolean included, boolean excluded) {
         this.included = included;
         this.excluded = excluded;
      }

      public boolean isExcluded() {
         return excluded;
      }

      public boolean isIncluded() {
         return included;
      }

      public boolean isSelected() {
         return included && !excluded;
      }

   }

   public static boolean isTestIncluded(TestCase test, JUnitTestTransformer transformer) {
      ExecutionConfig executionConfig = transformer.getConfig().getExecutionConfig();
      CompilationUnit unit = getUnit(test, transformer, executionConfig);

      IncludeExcludeInfo testInfo = getIncludeExcludeInfo(executionConfig, unit);

      IncludeExcludeInfo parentInfo = getParentInfo(transformer, executionConfig, unit);

      return (testInfo.isSelected() && !parentInfo.isExcluded()) || (parentInfo.isIncluded() && !parentInfo.isExcluded());
   }

   private static IncludeExcludeInfo getParentInfo(JUnitTestTransformer transformer, ExecutionConfig executionConfig, CompilationUnit unit) {
      boolean anyParentExcluded = false;
      boolean anyParentIncluded = false;

      TestCase parentTest = getParentTest(unit);
      while (parentTest != null) {
         CompilationUnit parentUnit = getUnit(parentTest, transformer, executionConfig);
         IncludeExcludeInfo parentInfo = getIncludeExcludeInfo(executionConfig, parentUnit);
         if (parentInfo.isExcluded()) {
            anyParentExcluded = true;
         }
         if (parentInfo.isIncluded()) {
            anyParentIncluded = true;
         }
         
         parentTest = getParentTest(parentUnit);
      }
      IncludeExcludeInfo parentInfo = new IncludeExcludeInfo(anyParentIncluded, anyParentExcluded);
      return parentInfo;
   }

   private static TestCase getParentTest(CompilationUnit unit) {
      for (ClassOrInterfaceDeclaration clazz : ParseUtil.getClasses(unit)) {
         if (clazz.getExtendedTypes().size() == 1) {
            String extendType = clazz.getExtendedTypes(0).getNameAsString();
            String fqn = FQNDeterminer.getParameterFQN(unit, extendType);
            ChangedEntity entity = new ChangedEntity(fqn);
            TestCase parentTest = new TestCase(entity);
            return parentTest;
         }
      }
      return null;
   }

   private static IncludeExcludeInfo getIncludeExcludeInfo(ExecutionConfig executionConfig, CompilationUnit unit) {
      boolean includedByRule = isIncluded(executionConfig, unit);

      boolean excludeByRule = isExcluded(executionConfig, unit);

      return new IncludeExcludeInfo(includedByRule, excludeByRule);
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
