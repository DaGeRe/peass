package de.dagere.peass.dependency.jmh;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import de.dagere.kopeme.parsing.JUnitParseUtil;
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

public class JMHTestTransformer implements TestTransformer {

   private final File projectFolder;
   private final MeasurementConfiguration measurementConfig;

   public JMHTestTransformer(final File projectFolder, final MeasurementConfiguration measurementConfig) {
      this.projectFolder = projectFolder;
      this.measurementConfig = measurementConfig;
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
            String moduleName = ModuleClassMapping.getModuleName(projectFolder, moduleFolder);
            File clazzFile = ClazzFileFinder.getClazzFile(moduleFolder, clazzname);
            try {
               findBenchmarksInClazzfile(moduleName, new File(moduleFolder, "src/main/java"), tests, clazzFile);
            } catch (FileNotFoundException e) {
               throw new RuntimeException(e);
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
            TestSet moduleTests;
            moduleTests = findModuleTests(mapping, includedModules, module);
            allBenchmarks.addTestSet(moduleTests);
         }
      } catch (FileNotFoundException e) {
         throw new RuntimeException("File was not found, can not handle this error", e);
      }
      return allBenchmarks;
   }

   public TestSet findModuleTests(final ModuleClassMapping mapping, final List<String> includedModules, final File module) throws FileNotFoundException {
      File srcFolder = new File(module, "src/main/java");
      Collection<File> javaFiles = FileUtils.listFiles(srcFolder, new WildcardFileFilter("*.java"), TrueFileFilter.INSTANCE);
      TestSet result = new TestSet();
      for (File clazzFile : javaFiles) {
         String moduleName = ModuleClassMapping.getModuleName(projectFolder, module);
         findBenchmarksInClazzfile(moduleName, srcFolder, result, clazzFile);
      }
      return result;
   }

   private void findBenchmarksInClazzfile(final String moduleName, final File srcFolder, final TestSet result, final File clazzFile) throws FileNotFoundException {
      String clazzName = ClazzFileFinder.getClazz(srcFolder, clazzFile);
      final CompilationUnit cu = JavaParserProvider.parse(clazzFile);
      List<ClassOrInterfaceDeclaration> clazzDeclarations = ClazzFinder.getClazzDeclarations(cu);
      for (ClassOrInterfaceDeclaration clazz : clazzDeclarations) {
         List<String> benchmarkMethods = JUnitParseUtil.getAnnotatedMethods(clazz, "org.openjdk.jmh.annotations.Benchmark", "Benchmark");
         for (String benchmarkMethod : benchmarkMethods) {
            TestCase foundBenchmark = new TestCase(clazzName, benchmarkMethod, moduleName);
            result.addTest(foundBenchmark);
         }
      }
   }

}
