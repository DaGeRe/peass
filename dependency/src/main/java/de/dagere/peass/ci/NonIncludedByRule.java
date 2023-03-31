package de.dagere.peass.ci;

import java.io.File;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;

import de.dagere.nodeDiffDetector.data.TestCase;
import de.dagere.nodeDiffDetector.data.TestClazzCall;
import de.dagere.nodeDiffDetector.typeFinding.TypeFileFinder;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.changesreading.FQNDeterminer;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.testtransformation.ParseUtil;
import de.dagere.peass.testtransformation.TestTransformer;

public class NonIncludedByRule {

   private static final Logger LOG = LogManager.getLogger(NonIncludedByRule.class);

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

   public static boolean isTestIncluded(TestCase test, JUnitTestTransformer transformer, ModuleClassMapping mapping) {
      ExecutionConfig executionConfig = transformer.getConfig().getExecutionConfig();
      CompilationUnit unit = getUnit(test, transformer, executionConfig);
      if (unit == null) {
         LOG.info("Did not find compilation unit for {}, assuming test is not existing but was included before", test);
         return true;
      }
      IncludeExcludeInfo testInfo = getIncludeExcludeInfo(executionConfig, unit);

      IncludeExcludeInfo parentInfo = getParentInfo(transformer, executionConfig, unit, mapping);

      return (testInfo.isSelected() && !parentInfo.isExcluded()) || (parentInfo.isIncluded() && !parentInfo.isExcluded());
   }

   private static IncludeExcludeInfo getParentInfo(JUnitTestTransformer transformer, ExecutionConfig executionConfig, CompilationUnit unit, ModuleClassMapping mapping) {
      boolean anyParentExcluded = false;
      boolean anyParentIncluded = false;

      TestClazzCall parentTest = getParentTest(unit, mapping);
      while (parentTest != null) {
         CompilationUnit parentUnit = getUnit(parentTest, transformer, executionConfig);
         if (parentUnit == null) {
            LOG.warn("Did not find {}; ignoring parent class for includeByRule-checking", parentTest);
            break;
         }
         
         IncludeExcludeInfo parentInfo = getIncludeExcludeInfo(executionConfig, parentUnit);
         if (parentInfo.isExcluded()) {
            anyParentExcluded = true;
         }
         if (parentInfo.isIncluded()) {
            anyParentIncluded = true;
         }

         parentTest = getParentTest(parentUnit, mapping);
      }
      IncludeExcludeInfo parentInfo = new IncludeExcludeInfo(anyParentIncluded, anyParentExcluded);
      return parentInfo;
   }

   private static TestClazzCall getParentTest(CompilationUnit unit, ModuleClassMapping mapping) {
      for (ClassOrInterfaceDeclaration clazz : ParseUtil.getClasses(unit)) {
         if (clazz.getExtendedTypes().size() == 1) {
            String extendType = clazz.getExtendedTypes(0).getNameAsString();
            String fqn = FQNDeterminer.getParameterFQN(unit, extendType);
            String module = mapping.getModuleOfClass(fqn);
            
            // Parent tests outside of the currently examined project are not considered, since we do not have the source code
            if (module != null) {
               TestClazzCall parentTest = new TestClazzCall(fqn, module);
               return parentTest;
            }
            
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
      TypeFileFinder finder = new TypeFileFinder(executionConfig);
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

   public static void removeNotIncluded(TestSet tests, TestTransformer testTransformer, ModuleClassMapping mapping) {
      if (testTransformer.getConfig().getExecutionConfig().getIncludeByRule().size() > 0 && testTransformer instanceof JUnitTestTransformer) {
         JUnitTestTransformer junitTestTransformer = (JUnitTestTransformer) testTransformer;
         for (Iterator<Entry<TestClazzCall, Set<String>>> testcaseIterator = tests.getTestcases().entrySet().iterator(); testcaseIterator.hasNext();) {
            Entry<TestClazzCall, Set<String>> testcase = testcaseIterator.next();
            if (!isTestIncluded(testcase.getKey(), junitTestTransformer, mapping)) {
               testcaseIterator.remove();
            }
         }
      }
   }
}
