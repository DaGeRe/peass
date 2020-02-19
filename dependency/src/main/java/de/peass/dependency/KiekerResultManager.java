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
package de.peass.dependency;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.kopeme.datacollection.DataCollectorList;
import de.dagere.kopeme.parsing.BuildtoolProjectNameReader;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependency.execution.TestExecutor;
import de.peass.testtransformation.JUnitTestTransformer;

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
   protected final JUnitTestTransformer testTransformer;

   

   public KiekerResultManager(final File projectFolder, final long timeout) {
      folders = new PeASSFolders(projectFolder);
      testTransformer = createTestTransformer();
      executor = ExecutorCreator.createExecutor(folders, timeout, testTransformer);
   }
   
   public JUnitTestTransformer getTestTransformer() {
      return testTransformer;
   }

   protected JUnitTestTransformer createTestTransformer() {
      final MeasurementConfiguration config = new MeasurementConfiguration(1);
      config.setIterations(1);
      config.setWarmup(0);
      config.setUseKieker(true);
      final JUnitTestTransformer testGenerator = new JUnitTestTransformer(folders.getProjectFolder(), config);
      testGenerator.getConfig().setLogFullData(false);
      testGenerator.setEncoding(StandardCharsets.UTF_8);
      return testGenerator;
   }

   public void runTraceTests(final TestSet testsToUpdate, final String version) throws IOException, XmlPullParserException, InterruptedException {
      truncateKiekerResults();
      // TODO Verschieben

      LOG.debug("Executing dependency update test, results folder: {}", folders.getTempMeasurementFolder());
      final TestSet tests = new TestSet();
      testTransformer.determineVersions(executor.getModules());
      for (final ChangedEntity clazzname : testsToUpdate.getClasses()) {
         final File moduleFolder = new File(folders.getProjectFolder(), clazzname.getModule());

         final List<String> methods = testTransformer.getTests(moduleFolder, clazzname);
         for (final String method : methods) {
            tests.addTest(clazzname, method);
         }
      }
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
            executor.executeTest(testcase, logVersionFolder, executor.getTimeout());
         }
      }
      cleanAboveSize(logVersionFolder, 500, "txt");
   }

   /**
    * Deletes files which are bigger than sizeInMb Mb, since they pollute the disc space and will not be analyzable
    * 
    * @param folderToClean
    */
   void cleanAboveSize(final File folderToClean, final int sizeInMb, final String ending) {
      for (final File logFile : FileUtils.listFiles(folderToClean, new WildcardFileFilter("*." + ending), TrueFileFilter.INSTANCE)) {
         final long size = logFile.length() / (1024 * sizeInMb);
         LOG.debug("File: {} Size: {}", logFile, size);
         if (size > 1024) {
            LOG.debug("Deleting file.");
            logFile.delete();
         }
      }
   }

   public File getXMLFileFolder(final File moduleFolder) throws FileNotFoundException, IOException, XmlPullParserException {
      return getXMLFileFolder(folders, moduleFolder);
   }

   public static File getXMLFileFolder(final PeASSFolders folders, final File moduleFolder) throws FileNotFoundException, IOException, XmlPullParserException {
      File xmlFileFolder = null;
      final BuildtoolProjectNameReader buildtoolProjectNameReader = new BuildtoolProjectNameReader();
      if (buildtoolProjectNameReader.foundPomXml(moduleFolder, 1)) {
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
