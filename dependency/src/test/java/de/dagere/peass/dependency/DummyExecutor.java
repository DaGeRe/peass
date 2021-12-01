package de.dagere.peass.dependency;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.execution.ProjectModules;
import de.dagere.peass.dependency.execution.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.TestTransformer;

public class DummyExecutor extends TestExecutor{

   public DummyExecutor(final PeassFolders folders, final TestTransformer testTransformer, final EnvironmentVariables env) {
      super(folders, testTransformer, env);
      // TODO Auto-generated constructor stub
   }

   @Override
   public void prepareKoPeMeExecution(final File logFile) throws IOException, InterruptedException, XmlPullParserException {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void executeTest(final TestCase test, final File logFolder, final long timeout) {
      // TODO Auto-generated method stub
      
   }

   @Override
   public boolean isVersionRunning(final String version) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public ProjectModules getModules() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   protected void clean(final File logFile) throws IOException, InterruptedException {
      // TODO Auto-generated method stub
      
   }

   @Override
   public boolean doesBuildfileExist() {
      return true;
   }

}
