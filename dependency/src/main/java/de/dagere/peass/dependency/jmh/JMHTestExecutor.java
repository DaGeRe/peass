package de.dagere.peass.dependency.jmh;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.execution.MavenPomUtil;
import de.dagere.peass.dependency.execution.ProjectModules;
import de.dagere.peass.dependency.execution.TestExecutor;
import de.dagere.peass.execution.maven.MavenCleaner;
import de.dagere.peass.testtransformation.TestTransformer;

/**
 * Supports the execution of JMH tests, which is necessary to do their regression test selection. Currently only supports maven projects.
 * @author reichelt
 *
 */
public class JMHTestExecutor extends TestExecutor {

   private final JMHTestTransformer transformer;
   
   public JMHTestExecutor(final PeASSFolders folders, final TestTransformer testTransformer, final EnvironmentVariables env) {
      super(folders, null, env);
      this.transformer = (JMHTestTransformer) testTransformer;
   }

   @Override
   public void prepareKoPeMeExecution(final File logFile) throws IOException, InterruptedException, XmlPullParserException {
      // TODO Auto-generated method stub

   }

   @Override
   public void executeAllKoPeMeTests(final File logFile) throws IOException, XmlPullParserException, InterruptedException {
      // TODO Auto-generated method stub

   }

   @Override
   public void executeTest(final TestCase tests, final File logFolder, final long timeout) {
      // TODO Auto-generated method stub

   }

   @Override
   protected void runTest(final File moduleFolder, final File logFile, final String testname, final long timeout) {
      // TODO Auto-generated method stub

   }

   @Override
   public boolean isVersionRunning(final String version) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public ProjectModules getModules() throws IOException, XmlPullParserException {
      File pomFile = new File(folders.getProjectFolder(), "pom.xml");
      return MavenPomUtil.getModules(pomFile);
   }

   @Override
   protected void clean(final File logFile) throws IOException, InterruptedException {
      new MavenCleaner(folders, env).clean(logFile);
   }

}
