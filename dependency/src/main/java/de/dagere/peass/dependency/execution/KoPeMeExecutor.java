package de.dagere.peass.dependency.execution;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.testtransformation.JUnitTestShortener;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public abstract class KoPeMeExecutor extends TestExecutor {
   
   private static final Logger LOG = LogManager.getLogger(KoPeMeExecutor.class);

   protected final JUnitTestTransformer testTransformer;

   public KoPeMeExecutor(final PeASSFolders folders, final JUnitTestTransformer testTransformer, final EnvironmentVariables env) {
      super(folders, testTransformer, env);
      this.testTransformer = testTransformer;
   }
   
   public void transformTests() {
      try {
         final List<File> modules = getModules().getModules();
         testTransformer.determineVersions(modules);
         testTransformer.transformTests();
      } catch (IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   protected String getTestGoal() {
      String testGoal;
      if (isAndroid) {
         testGoal = testTransformer.getConfig().getTestGoal() != null ? testTransformer.getConfig().getTestGoal() : "testRelease";
      } else {
         testGoal = testTransformer.getConfig().getTestGoal() != null ? testTransformer.getConfig().getTestGoal() : "test";
      }
      return testGoal;
   }

   protected abstract void runTest(File moduleFolder, final File logFile, final String testname, final long timeout);
   
   void runMethod(final File logFolder, final TestCase test, final File moduleFolder, final long timeout) {
      try (final JUnitTestShortener shortener = new JUnitTestShortener(testTransformer, moduleFolder, test.toEntity(), test.getMethod())) {
         LOG.info("Cleaning...");
         final File cleanFile = getCleanLogFile(logFolder, test);
         clean(cleanFile);

         final File methodLogFile = getMethodLogFile(logFolder, test);
         runTest(moduleFolder, methodLogFile, test.getClazz(), timeout);
      } catch (Exception e1) {
         e1.printStackTrace();
      }
   }
   
   @Override
   public JUnitTestTransformer getTestTransformer() {
      return testTransformer;
   }
}
