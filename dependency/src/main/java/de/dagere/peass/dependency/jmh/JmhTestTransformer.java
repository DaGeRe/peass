package de.dagere.peass.dependency.jmh;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import de.dagere.kopeme.parsing.JUnitParseUtil;
import de.dagere.peass.ci.NonIncludedTestRemover;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.ClazzFileFinder;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.changesreading.ClazzFinder;
import de.dagere.peass.dependency.changesreading.JavaParserProvider;
import de.dagere.peass.dependency.execution.ProjectModules;
import de.dagere.peass.testtransformation.TestTransformer;

public class JmhTestTransformer implements TestTransformer {

   private static final Logger LOG = LogManager.getLogger(JmhTestTransformer.class);

   private final File projectFolder;
   private final MeasurementConfiguration measurementConfig;
   private boolean isAggregatedWriter;
   private boolean ignoreEOIs;

   public JmhTestTransformer(final File projectFolder, final MeasurementConfiguration measurementConfig) {
      this.projectFolder = projectFolder;
      this.measurementConfig = measurementConfig;
   }

   public JmhTestTransformer(final File projectFolder, final ExecutionConfig executionConfig) {
      this.projectFolder = projectFolder;
      measurementConfig = new MeasurementConfiguration(1, executionConfig);
      measurementConfig.setIterations(1);
      measurementConfig.setWarmup(0);
      measurementConfig.setUseKieker(true);
   }

   @Override
   public MeasurementConfiguration getConfig() {
      return measurementConfig;
   }

   @Override
   public TestSet buildTestMethodSet(final TestSet testsToUpdate, final List<File> modules) {
      final TestSet tests = new TestSet();
      for (final ChangedEntity clazzname : testsToUpdate.getClasses()) {
         final Set<String> currentClazzMethods = testsToUpdate.getMethods(clazzname);
         if (currentClazzMethods == null || currentClazzMethods.isEmpty()) {
            final File moduleFolder = new File(projectFolder, clazzname.getModule());
            final List<TestCase> methods = getTestMethodNames(moduleFolder, clazzname);
            for (final TestCase test : methods) {
               addTestIfIncluded(tests, test);
            }
         } else {
            for (final String method : currentClazzMethods) {
               TestCase test = new TestCase(clazzname.getClazz(), method, clazzname.getModule());
               tests.addTest(test);
            }
         }
      }
      return tests;
   }

   // TODO includedModules is currenctly ignored for jmh!
   @Override
   public TestSet findModuleTests(final ModuleClassMapping mapping, final List<String> includedModules, final ProjectModules modules) {
      TestSet allBenchmarks = new TestSet();
      try {
         for (File module : modules.getModules()) {
            TestSet moduleTests = findModuleTests(mapping, includedModules, module);
            allBenchmarks.addTestSet(moduleTests);
         }
      } catch (FileNotFoundException e) {
         throw new RuntimeException("File was not found, can not handle this error", e);
      }
      return allBenchmarks;
   }

   public TestSet findModuleTests(final ModuleClassMapping mapping, final List<String> includedModules, final File module) throws FileNotFoundException {
      TestSet moduleTests = new TestSet();
      for (final String clazz : ClazzFileFinder.getClasses(module)) {
         String currentModule = ModuleClassMapping.getModuleName(projectFolder, module);
         final List<TestCase> testMethodNames = getTestMethodNames(module, new ChangedEntity(clazz, currentModule));
         for (TestCase test : testMethodNames) {
            if (includedModules == null || includedModules.contains(test.getModule())) {
               addTestIfIncluded(moduleTests, test);
            }
         }
      }
      return moduleTests;
   }

   @Override
   public void setAggregatedWriter(final boolean isAggregatedWriter) {
      this.isAggregatedWriter = isAggregatedWriter;
   }

   @Override
   public boolean isAggregatedWriter() {
      return isAggregatedWriter;
   }

   @Override
   public boolean isJUnit3() {
      return false;
   }

   @Override
   public boolean isIgnoreEOIs() {
      return ignoreEOIs;
   }

   @Override
   public List<TestCase> getTestMethodNames(final File module, final ChangedEntity clazzname) {
      final List<TestCase> methods = new LinkedList<>();
      final File clazzFile = ClazzFileFinder.getClazzFile(module, clazzname);
      try {
         LOG.debug("Parsing {} - {}", clazzFile, clazzname);
         final CompilationUnit unit = JavaParserProvider.parse(clazzFile);
         List<ClassOrInterfaceDeclaration> clazzDeclarations = ClazzFinder.getClazzDeclarations(unit);
         for (ClassOrInterfaceDeclaration clazz : clazzDeclarations) {
            String parsedClassName = getFullName(clazz);
            System.out.println(parsedClassName + " " + clazzname.getSimpleClazzName());
            if (parsedClassName.equals(clazzname.getSimpleClazzName())) {
               List<String> benchmarkMethods = JUnitParseUtil.getAnnotatedMethods(clazz, "org.openjdk.jmh.annotations.Benchmark", "Benchmark");
               for (String benchmarkMethod : benchmarkMethods) {
                  TestCase foundBenchmark = new TestCase(clazzname.getClazz(), benchmarkMethod, clazzname.getModule());
                  methods.add(foundBenchmark);
               }
            }
         }
      } catch (FileNotFoundException e) {
         throw new RuntimeException(e);
      }
      return methods;
   }

   private String getFullName(ClassOrInterfaceDeclaration clazz) {
      String parsedClassName = clazz.getNameAsString();
      boolean hasClassParent = clazz.getParentNode().isPresent() && clazz.getParentNode().get() instanceof ClassOrInterfaceDeclaration;
      ClassOrInterfaceDeclaration parent = hasClassParent ? (ClassOrInterfaceDeclaration) clazz.getParentNode().get() : null;
      while (parent != null) {
         parsedClassName = parent.getNameAsString() + ChangedEntity.CLAZZ_SEPARATOR + parsedClassName;
         hasClassParent = parent.getParentNode().isPresent() && parent.getParentNode().get() instanceof ClassOrInterfaceDeclaration;
         parent = hasClassParent ? (ClassOrInterfaceDeclaration) parent.getParentNode().get() : null;
      }
      return parsedClassName;
   }

   private void addTestIfIncluded(final TestSet moduleTests, final TestCase test) {
      final List<String> includes = getConfig().getExecutionConfig().getIncludes();
      if (NonIncludedTestRemover.isTestIncluded(test, includes)) {
         moduleTests.addTest(test);
      }
   }

   @Override
   public void setIgnoreEOIs(final boolean ignoreEOIs) {
      this.ignoreEOIs = ignoreEOIs;
   }

}
