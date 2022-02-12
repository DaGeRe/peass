package de.dagere.peass.execution.utils;

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestShortener;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public abstract class KoPeMeExecutor extends TestExecutor {

   private static final Logger LOG = LogManager.getLogger(KoPeMeExecutor.class);

   protected final JUnitTestTransformer testTransformer;

   public KoPeMeExecutor(final PeassFolders folders, final JUnitTestTransformer testTransformer, final EnvironmentVariables env) {
      super(folders, testTransformer, env);
      this.testTransformer = testTransformer;
   }

   public void transformTests() {
      final List<File> modules = getModules().getModules();
      testTransformer.determineVersions(modules);
      testTransformer.transformTests();
   }

   protected String getTestGoal() {
      String testGoal;
      ExecutionConfig executionConfig = testTransformer.getConfig().getExecutionConfig();
      if (isAndroid) {
         testGoal = executionConfig.getTestGoal() != null ? executionConfig.getTestGoal() : "testRelease";
      } else {
         testGoal = executionConfig.getTestGoal() != null ? executionConfig.getTestGoal() : "test";
      }
      return testGoal;
   }

   protected abstract void runTest(File moduleFolder, final File logFile, TestCase test, final String testname, final long timeout);

   protected void runMethod(final File logFolder, final TestCase test, final File moduleFolder, final long timeout) {
      try (final JUnitTestShortener shortener = new JUnitTestShortener(testTransformer, moduleFolder, test.toEntity(), test.getMethod())) {
         LOG.info("Cleaning...");
         final File cleanFile = getCleanLogFile(logFolder, test);
         clean(cleanFile);

         final File methodLogFile = getMethodLogFile(logFolder, test);
         runTest(moduleFolder, methodLogFile, test, test.getClazz(), timeout);
      } catch (Exception e1) {
         e1.printStackTrace();
      }
   }

   @Override
   public JUnitTestTransformer getTestTransformer() {
      return testTransformer;
   }
}
