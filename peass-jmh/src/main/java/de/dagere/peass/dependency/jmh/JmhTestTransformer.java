package de.dagere.peass.dependency.jmh;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import de.dagere.kopeme.parsing.JUnitParseUtil;
import de.dagere.nodeDiffGenerator.clazzFinding.ClazzFileFinder;
import de.dagere.nodeDiffGenerator.clazzFinding.ClazzFinder;
import de.dagere.nodeDiffGenerator.data.MethodCall;
import de.dagere.nodeDiffGenerator.data.TestClazzCall;
import de.dagere.nodeDiffGenerator.data.TestMethodCall;
import de.dagere.peass.ci.NonIncludedTestRemover;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.WorkloadType;
import de.dagere.peass.dependency.RunnableTestInformation;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.changesreading.JavaParserProvider;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.testtransformation.JUnitVersions;
import de.dagere.peass.testtransformation.TestTransformer;

public class JmhTestTransformer implements TestTransformer {

   private static final Logger LOG = LogManager.getLogger(JmhTestTransformer.class);

   private final File projectFolder;
   private final MeasurementConfig measurementConfig;
   private boolean ignoreEOIs;

   public JmhTestTransformer(final File projectFolder, final MeasurementConfig measurementConfig) {
      this.projectFolder = projectFolder;
      this.measurementConfig = measurementConfig;
      if (!measurementConfig.getExecutionConfig().getTestExecutor().equals(WorkloadType.JMH.getTestExecutor())) {
         throw new RuntimeException("Test Executor needs to be " + WorkloadType.JMH.getTestExecutor());
      }
   }

   public JmhTestTransformer(final File projectFolder, final ExecutionConfig executionConfig, final KiekerConfig kiekerConfig) {
      this.projectFolder = projectFolder;
      measurementConfig = new MeasurementConfig(1, executionConfig, kiekerConfig);
      measurementConfig.setIterations(1);
      measurementConfig.setWarmup(0);
      measurementConfig.getKiekerConfig().setUseKieker(true);
   }

   @Override
   public MeasurementConfig getConfig() {
      return measurementConfig;
   }

   @Override
   public RunnableTestInformation buildTestMethodSet(final TestSet testsToUpdate, ModuleClassMapping modules) {
      final RunnableTestInformation tests = new RunnableTestInformation();
      for (final TestClazzCall clazzname : testsToUpdate.getClasses()) {
         final Set<String> currentClazzMethods = testsToUpdate.getMethods(clazzname);
         if (currentClazzMethods == null || currentClazzMethods.isEmpty()) {
            final File moduleFolder = new File(projectFolder, clazzname.getModule());
            final Set<TestMethodCall> methods = getTestMethodNames(moduleFolder, clazzname);
            for (final TestMethodCall test : methods) {
               addTestIfIncluded(tests, test);
            }
         } else {
            for (final String method : currentClazzMethods) {
               TestMethodCall test = new TestMethodCall(clazzname.getClazz(), method, clazzname.getModule());
               tests.getTestsToUpdate().addTest(test);
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
      RunnableTestInformation moduleTests = new RunnableTestInformation();
      ClazzFileFinder finder = new ClazzFileFinder(measurementConfig.getExecutionConfig());
      for (final String clazz : finder.getClasses(module)) {
         String currentModule = ModuleClassMapping.getModuleName(projectFolder, module);
         final Set<TestMethodCall> testMethodNames = getTestMethodNames(module, new TestClazzCall(clazz, currentModule));
         for (TestMethodCall test : testMethodNames) {
            if (includedModules == null || includedModules.contains(test.getModule())) {
               addTestIfIncluded(moduleTests, test);
            }
         }
      }
      return moduleTests.getTestsToUpdate();
   }

   @Override
   public boolean isIgnoreEOIs() {
      return ignoreEOIs;
   }

   @Override
   public JUnitVersions getJUnitVersions() {
      JUnitVersions jUnitVersions = new JUnitVersions();
      jUnitVersions.setJunit4(true);
      return jUnitVersions;
   }

   @Override
   public Set<TestMethodCall> getTestMethodNames(final File module, final TestClazzCall clazzname) {
      final Set<TestMethodCall> methods = new LinkedHashSet<>();
      ClazzFileFinder finder = new ClazzFileFinder(measurementConfig.getExecutionConfig());
      final File clazzFile = finder.getClazzFile(module, clazzname);
      try {
         // File might be removed or moved
         if (clazzFile != null) {
            LOG.debug("Parsing {} - {}", clazzFile, clazzname);
            final CompilationUnit unit = JavaParserProvider.parse(clazzFile);
            List<ClassOrInterfaceDeclaration> clazzDeclarations = ClazzFinder.getClazzDeclarations(unit);
            for (ClassOrInterfaceDeclaration clazz : clazzDeclarations) {
               String parsedClassName = getFullName(clazz);
               LOG.trace("Clazz: {} - {}", parsedClassName, clazzname.getShortClazz());
               if (parsedClassName.equals(clazzname.getShortClazz())) {
                  List<String> benchmarkMethods = JUnitParseUtil.getAnnotatedMethods(clazz, "org.openjdk.jmh.annotations.Benchmark", "Benchmark");
                  for (String benchmarkMethod : benchmarkMethods) {
                     TestMethodCall foundBenchmark = new TestMethodCall(clazzname.getClazz(), benchmarkMethod, clazzname.getModule());
                     methods.add(foundBenchmark);
                  }
               }
            }
         }
      } catch (FileNotFoundException e) {
         throw new RuntimeException(e);
      }
      return methods;
   }

   private String getFullName(final ClassOrInterfaceDeclaration clazz) {
      String parsedClassName = clazz.getNameAsString();
      boolean hasClassParent = clazz.getParentNode().isPresent() && clazz.getParentNode().get() instanceof ClassOrInterfaceDeclaration;
      ClassOrInterfaceDeclaration parent = hasClassParent ? (ClassOrInterfaceDeclaration) clazz.getParentNode().get() : null;
      while (parent != null) {
         parsedClassName = parent.getNameAsString() + MethodCall.CLAZZ_SEPARATOR + parsedClassName;
         hasClassParent = parent.getParentNode().isPresent() && parent.getParentNode().get() instanceof ClassOrInterfaceDeclaration;
         parent = hasClassParent ? (ClassOrInterfaceDeclaration) parent.getParentNode().get() : null;
      }
      return parsedClassName;
   }

   private void addTestIfIncluded(final RunnableTestInformation moduleTests, final TestMethodCall test) {
      if (NonIncludedTestRemover.isTestIncluded(test, getConfig().getExecutionConfig())) {
         moduleTests.getTestsToUpdate().addTest(test);
      }
   }

   @Override
   public void setIgnoreEOIs(final boolean ignoreEOIs) {
      this.ignoreEOIs = ignoreEOIs;
   }

   @Override
   public void determineVersions(final List<File> modules) {
      // not required for JmhTestTransformer, since inheritance is not considered and therefore no re-reading of files is required (and therefore no cache)
   }

}
