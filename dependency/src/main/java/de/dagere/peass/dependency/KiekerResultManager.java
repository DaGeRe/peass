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
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.kopeme.parsing.BuildtoolProjectNameReader;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.execution.TestExecutor;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
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
   protected final PeASSFolders folders;
   protected final TestTransformer testTransformer;

   public KiekerResultManager(final PeASSFolders folders, final ExecutionConfig executionConfig, final EnvironmentVariables env)  {
      this.folders = folders;
      MeasurementConfiguration fakeConfig = new MeasurementConfiguration(1, executionConfig);
      fakeConfig.setIterations(1);
      fakeConfig.setWarmup(0);
      fakeConfig.setUseKieker(true);
      fakeConfig.setUseSourceInstrumentation(true);
      fakeConfig.setTestGoal(executionConfig.getTestGoal());
      
      testTransformer = ExecutorCreator.createTestTransformer(folders, executionConfig, fakeConfig);
      
      // testTransformer = new JUnitTestTransformer(folders.getProjectFolder(), fakeConfig);
      executor = ExecutorCreator.createExecutor(folders, testTransformer, env);
   }

  

   public KiekerResultManager(final TestExecutor executor, final PeASSFolders folders, final JUnitTestTransformer testTransformer) {
      this.executor = executor;
      this.folders = folders;
      this.testTransformer = testTransformer;
   }

   public TestTransformer getTestTransformer() {
      return testTransformer;
   }

   public void runTraceTests(final TestSet testsToUpdate, final String version) throws IOException, XmlPullParserException, InterruptedException {
      truncateKiekerResults();
      // TODO Verschieben

      LOG.debug("Executing dependency update test, results folder: {}", folders.getTempMeasurementFolder());
      final TestSet tests = testTransformer.buildTestMethodSet(testsToUpdate, executor.getModules().getModules());
      executeKoPeMeKiekerRun(tests, version);
   }

   private void truncateKiekerResults() {
      LOG.debug("Truncating: {}", folders.getTempMeasurementFolder().getAbsolutePath());
      try {
         FileUtils.deleteDirectory(folders.getTempMeasurementFolder());
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
   public void executeKoPeMeKiekerRun(final TestSet testsToUpdate, final String version) throws IOException, XmlPullParserException, InterruptedException {
      final File logVersionFolder = new File(folders.getLogFolder(), version);
      if (!logVersionFolder.exists()) {
         logVersionFolder.mkdir();
      }

      executor.prepareKoPeMeExecution(new File(logVersionFolder, "clean.txt"));
      for (final Map.Entry<ChangedEntity, Set<String>> test : testsToUpdate.getTestcases().entrySet()) {
         for (final String method : test.getValue()) {
            final TestCase testcase = new TestCase(test.getKey().getJavaClazzName(), method, test.getKey().getModule());
            executor.executeTest(testcase, logVersionFolder, testTransformer.getConfig().getTimeoutInMinutes());
         }
      }
      cleanAboveSize(logVersionFolder, 100, "txt");

      LOG.debug("KoPeMe-Kieker-Run finished");

   }

   /**
    * Deletes files which are bigger than sizeInMb Mb, since they pollute the disc space and will not be analyzable
    * 
    * @param folderToClean
    */
   protected void cleanAboveSize(final File folderToClean, final int sizeInMb, final String ending) {
      for (final File file : FileUtils.listFiles(folderToClean, new WildcardFileFilter("*." + ending), TrueFileFilter.INSTANCE)) {
         final long size = file.length() / (1024 * 1024);
         LOG.debug("File: {} Size: {}", file, size);
         if (size > sizeInMb) {
            LOG.debug("Deleting file.");
            file.delete();
         }
      }
   }

   protected void cleanFolderAboveSize(final File folderToClean, final int sizeInMb) {
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

   public static File getXMLFileFolder(final PeASSFolders folders, final File moduleFolder) {
      File xmlFileFolder = null;
      final BuildtoolProjectNameReader buildtoolProjectNameReader = new BuildtoolProjectNameReader();
      if (buildtoolProjectNameReader.searchBuildfile(moduleFolder, 1)) {
         final String name = buildtoolProjectNameReader.getProjectName();
         xmlFileFolder = new File(folders.getTempMeasurementFolder(), name);
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
