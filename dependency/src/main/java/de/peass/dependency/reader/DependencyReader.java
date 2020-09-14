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
package de.peass.dependency.reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.github.javaparser.ParseProblemException;

import de.peass.dependency.ChangeManager;
import de.peass.dependency.DependencyManager;
import de.peass.dependency.ExecutorCreator;
import de.peass.dependency.KiekerResultManager;
import de.peass.dependency.execution.TestExecutor;
import de.peass.dependency.persistence.Dependencies;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.vcs.VersionIterator;

/**
 * Reads the dependencies of a project
 * 
 * @author reichelt
 *
 */
public class DependencyReader extends DependencyReaderBase {

   private static final Logger LOG = LogManager.getLogger(DependencyReader.class);

   private final ChangeManager changeManager;
   private int overallSize = 0, prunedSize = 0;
   
   public DependencyReader(final File projectFolder, final File dependencyFile, final String url, final VersionIterator iterator, final int timeout,
         final ChangeManager changeManager) {
      super(new Dependencies(), projectFolder, dependencyFile, timeout, new VersionKeeper(new File("/dev/null")));

      this.iterator = iterator;

      dependencyResult.setUrl(url);
      
      this.changeManager = changeManager;
      
   }

   /**
    * Starts reading dependencies
    * 
    * @param projectFolder
    * @param dependencyFile
    * @param url
    * @param iterator
    */
   public DependencyReader(final File projectFolder, final File dependencyFile, final String url, final VersionIterator iterator, final int timeout,
         final VersionKeeper nochange) {
      super(new Dependencies(), projectFolder, dependencyFile, timeout, nochange);

      this.iterator = iterator;

      dependencyResult.setUrl(url);
      
      changeManager = new ChangeManager(folders);
      
   }

   /**
    * Continues reading dependencies
    * 
    * @param projectFolder
    * @param dependencyFile
    * @param url
    * @param iterator
    * @param initialdependencies
    * @param timeout Timeout in Minutes
    */
   public DependencyReader(final File projectFolder, final File dependencyFile, final String url, final VersionIterator iterator, final Dependencies initialdependencies,
         final int timeout) {
      this(projectFolder, dependencyFile, url, iterator, timeout,
            new VersionKeeper(new File(dependencyFile.getParentFile(), "nochanges.json")));

      dependencyResult.setVersions(initialdependencies.getVersions());
      dependencyResult.setInitialversion(initialdependencies.getInitialversion());

      InitialVersionReader initialVersionReader = new InitialVersionReader(initialdependencies, dependencyManager, iterator);
      initialVersionReader.readCompletedVersions();
      DependencyReaderUtil.write(dependencyResult, dependencyFile);
      lastRunningVersion = iterator.getTag();
   }

   boolean success = true;

   /**
    * Reads the dependencies of the tests
    */
   public boolean readDependencies() {
      try {
         if (!readInitialVersion()) {
            LOG.error("Analyzing first version was not possible");
            return false;
         }

         LOG.debug("Analysiere {} Einträge", iterator.getSize());

         prunedSize += dependencyManager.getDependencyMap().size();

         changeManager.saveOldClasses();
         lastRunningVersion = iterator.getTag();
         while (iterator.hasNextCommit()) {
            iterator.goToNextCommit();
            readVersion();
         }

         LOG.debug("Finished dependency-reading");
         return true;
      } catch (IOException | InterruptedException | XmlPullParserException e) {
         e.printStackTrace();
         return false;
      }
   }

   public void readVersion() throws IOException, FileNotFoundException {
      try {
         final int tests = analyseVersion(changeManager);
         DependencyReaderUtil.write(dependencyResult, dependencyFile);
         overallSize += dependencyManager.getDependencyMap().size();
         prunedSize += tests;

         LOG.info("Overall-tests: {} Executed tests with pruning: {}", overallSize, prunedSize);

         dependencyManager.getExecutor().deleteTemporaryFiles();
         final File xmlFileFolder = KiekerResultManager.getXMLFileFolder(folders, folders.getProjectFolder());
         if (xmlFileFolder != null) {
            FileUtils.deleteDirectory(xmlFileFolder);
         }
         cleanTooBigLogs();
      } catch (final ParseProblemException | XmlPullParserException | InterruptedException | IOException ppe) {
         LOG.debug("Exception while reading a version");
         ppe.printStackTrace();
      } 
   }

   public static final int MAX_SIZE_IN_MB = 10;

   public void cleanTooBigLogs() {
      File logFolder = folders.getLogFolder();
      File versionFolder = new File(logFolder, iterator.getTag());
      if (versionFolder.exists()) {
         for (File clazzFolder : versionFolder.listFiles()) {
            if (clazzFolder.isDirectory()) {
               for (File methodLog : clazzFolder.listFiles()) {
                  long sizeInMb = (methodLog.length() / (1024 * 1024));
                  if (sizeInMb > MAX_SIZE_IN_MB) {
                     methodLog.delete();
                  }
               }
            }
         }
      }
   }

   public Dependencies getDependencies() {
      return dependencyResult;
   }

   public void setIterator(final VersionIterator reserveIterator) {
      this.iterator = reserveIterator;
   }

}
