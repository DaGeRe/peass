package de.dagere.peass.dependency;

import java.io.File;
import java.io.IOException;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.TestTransformer;

public class DummyExecutor extends TestExecutor{

   public DummyExecutor(final PeassFolders folders, final TestTransformer testTransformer, final EnvironmentVariables env) {
      super(folders, testTransformer, env);
      // TODO Auto-generated constructor stub
   }

   @Override
   public void prepareKoPeMeExecution(final File logFile) {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void executeTest(final TestMethodCall test, final File logFolder, final long timeout) {
      // TODO Auto-generated method stub
      
   }

   @Override
   public boolean isCommitRunning(final String version) {
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

   @Override
   public void executeTest(String javaAgent, TestMethodCall test, File logFolder, long timeout) {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("Unimplemented method 'executeTest'");
   }

}
