/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.dagere.peass.dependency;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.kopeme.parsing.BuildtoolProjectNameReader;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.TestTransformer;

/**
 * Handles the running of tests
 * 
 * @author reichelt
 *
 */
public class KiekerResultManager {

   private static final Logger LOG = LogManager.getLogger(KiekerResultManager.class);

   protected final TestExecutor executor;
   protected final PeassFolders folders;
   protected final TestTransformer testTransformer;
   private TestSet ignoredTests;

   public KiekerResultManager(final PeassFolders folders, final ExecutionConfig executionConfig, final KiekerConfig kiekerConfig, final EnvironmentVariables env) {
      this.folders = folders;
      this.testTransformer = RTSTestTransformerBuilder.createTestTransformer(folders, executionConfig, kiekerConfig);

      // testTransformer = new JUnitTestTransformer(folders.getProjectFolder(), fakeConfig);
      executor = ExecutorCreator.createExecutor(folders, testTransformer, env);
   }

   public KiekerResultManager(final TestExecutor executor, final PeassFolders folders) {
      this.executor = executor;
      this.folders = folders;
      this.testTransformer = executor.getTestTransformer();
   }

   public TestTransformer getTestTransformer() {
      return testTransformer;
   }
   
   public ModuleClassMapping getModuleClassMapping() {
      return new ModuleClassMapping(executor);
   }
   
   public TestSet getIgnoredTests() {
      return ignoredTests;
   }

   public void runTraceTests(final TestSet testsToUpdate, final String commit) {
      truncateKiekerResults();
      // TODO Verschieben
      
      LOG.debug("Executing dependency update test, results folder: {}", folders.getTempMeasurementFolder());
      ModuleClassMapping mapping = new ModuleClassMapping(executor);
      final RunnableTestInformation tests = testTransformer.buildTestMethodSet(testsToUpdate, mapping);
      executeKoPeMeKiekerRun(tests.getTestsToUpdate(), commit, folders.getDependencyLogFolder());
      ignoredTests = tests.getIgnoredTests();
   }

   private void truncateKiekerResults() {
      LOG.debug("Truncating: {}", folders.getTempMeasurementFolder().getAbsolutePath());
      try {
         FileUtils.deleteDirectory(folders.getTempMeasurementFolder());
         
         // Workaround: do delete temporary tomcat output folders, so peass-ant "knows" when to re-instrument all classes
         File outputFolder = new File(folders.getProjectFolder(), "output");
         if (folders.getProjectName().equals("tomcat") && outputFolder.exists()) {
        	 FileUtils.deleteDirectory(outputFolder);
         }
      } catch (final IOException e) {
         e.printStackTrace();
         if (folders.getTempMeasurementFolder().exists()) {
            try {
               FileUtils.deleteDirectory(folders.getTempMeasurementFolder());
            } catch (final IOException e1) {
               e1.printStackTrace();
            }
         }
      }
   }

   /**
    * Executes the tests with all methods of the given test classes.
    * 
    * @param testsToUpdate
    * @throws IOException
    * @throws XmlPullParserException
    * @throws InterruptedException
    */
   public void executeKoPeMeKiekerRun(final TestSet testsToUpdate, final String commit, final File logFolder) {
      final File commitLogFolder = new File(logFolder, commit);
      if (!commitLogFolder.exists()) {
         commitLogFolder.mkdir();
      }

      executor.prepareKoPeMeExecution(new File(commitLogFolder, "clean.txt"));
      for (final TestMethodCall testcase : testsToUpdate.getTestMethods()) {
         executor.executeTest(testcase, commitLogFolder, testTransformer.getConfig().getTimeoutInSeconds());
      }
      LOG.debug("KoPeMe-Kieker-Run finished");
   }

   protected void cleanFolderAboveSize(final File folderToClean, final long sizeInMb) {
      for (final File file : folderToClean.listFiles()) {
         if (file.isDirectory()) {
            final long size = FileUtils.sizeOfDirectory(file) / (1024 * 1024);
            LOG.debug("File: {} Size: {}", file, size);
            if (size > sizeInMb) {
               LOG.debug("Deleting file.");
               try {
                  FileUtils.deleteDirectory(file);
               } catch (IOException e) {
                  e.printStackTrace();
               }
            }
         }
      }
   }

   public File getXMLFileFolder(final File moduleFolder) throws FileNotFoundException, IOException, XmlPullParserException {
      return getXMLFileFolder(folders, moduleFolder);
   }

   public static File getXMLFileFolder(final PeassFolders folders, final File moduleFolder) {
      File xmlFileFolder = null;
      final BuildtoolProjectNameReader buildtoolProjectNameReader = new BuildtoolProjectNameReader();
      if (buildtoolProjectNameReader.searchBuildfile(moduleFolder, 1)) {
         final String name = buildtoolProjectNameReader.getProjectName();
         xmlFileFolder = new File(folders.getTempMeasurementFolder(), name);
      } else {
         LOG.error("No buildfile found in {}", moduleFolder);
      }
      return xmlFileFolder;
   }

   public void deleteTempFiles() {
      executor.deleteTemporaryFiles();
   }

   public TestExecutor getExecutor() {
      return executor;
   }

}
