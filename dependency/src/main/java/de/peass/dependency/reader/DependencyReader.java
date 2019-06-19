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
import de.peass.dependency.TestResultManager;
import de.peass.dependency.execution.TestExecutor;
import de.peass.dependency.persistence.Dependencies;
import de.peass.vcs.VersionIterator;
import de.peass.vcs.VersionIteratorGit;

/**
 * Reads the dependencies of a project
 * 
 * @author reichelt
 *
 */
public class DependencyReader extends DependencyReaderBase {

   private static final Logger LOG = LogManager.getLogger(DependencyReader.class);

   protected boolean init = false;
   private final ChangeManager changeManager;
   private int overallSize = 0, prunedSize = 0;

   private final VersionKeeper nonRunning;

   public DependencyReader(final File projectFolder, final File dependencyFile, final String url, final VersionIterator iterator, final int timeout,
         final ChangeManager changeManager) {
      super(new Dependencies(), projectFolder, dependencyFile, timeout, new VersionKeeper(new File("/dev/null")));

      this.iterator = iterator;

      dependencyResult.setUrl(url);

      dependencyManager = new DependencyManager(projectFolder, timeout);

      this.changeManager = changeManager;
      nonRunning = new VersionKeeper(new File("/dev/null"));
   }

   /**
    * Starts reading dependencies
    * 
    * @param projectFolder
    * @param dependencyFile
    * @param url
    * @param iterator
    */
   public DependencyReader(final File projectFolder, final File dependencyFile, final String url, final VersionIterator iterator, final int timeout, final VersionKeeper nonRunning,
         final VersionKeeper nochange) {
      super(new Dependencies(), projectFolder, dependencyFile, timeout, nochange);

      this.iterator = iterator;

      dependencyResult.setUrl(url);

      dependencyManager = new DependencyManager(projectFolder, timeout);

      changeManager = new ChangeManager(folders);
      this.nonRunning = nonRunning;
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
      this(projectFolder, dependencyFile, url, iterator, timeout, new VersionKeeper(new File(dependencyFile.getParentFile(), "nonrunning.json")),
            new VersionKeeper(new File(dependencyFile.getParentFile(), "nochanges.json")));

      dependencyResult.setVersions(initialdependencies.getVersions());
      dependencyResult.setInitialversion(initialdependencies.getInitialversion());

      InitialVersionReader initialVersionReader = new InitialVersionReader(initialdependencies, dependencyManager, iterator);
      initialVersionReader.readCompletedVersions();
      DependencyReaderUtil.write(dependencyResult, dependencyFile);
      lastRunningVersion = iterator.getTag();
      init = true;
   }

   /**
    * Searches the first commit where a mvn clean packages runs correct, i.e. returns 1
    * 
    * @param projectFolder
    */
   public boolean searchFirstRunningCommit(final VersionIterator iterator, final TestExecutor executor, final File projectFolder) {
      boolean successGettingCommit = iterator.goToFirstCommit();
      while (!successGettingCommit && iterator.hasNextCommit()) {
         successGettingCommit = iterator.goToNextCommit();
      }
      if (!successGettingCommit) {
         throw new RuntimeException("Repository does not contain usable commit - maybe path has changed?");
      } else {
         LOG.info("Found first commit: " + iterator.getTag());
      }
      boolean isVersionRunning = false;
      while (!isVersionRunning && iterator.hasNextCommit()) {
         isVersionRunning = executor.isVersionRunning(iterator.getTag());

         if (!isVersionRunning) {
            LOG.debug("Buildfile does not exist / version is not running {}", iterator.getTag());
            if (executor.doesBuildfileExist()) {
               nonRunning.addVersion(iterator.getTag(), "Version is not running.");
            } else {
               nonRunning.addVersion(iterator.getTag(), "Buildfile does not exist.");
            }
            iterator.goToNextCommit();
         }
      }
      return isVersionRunning;
   }

   /**
    * Reads the dependencies of the tests
    */
   public boolean readDependencies() {
      try {
         if (!init) {
            final boolean running = searchFirstRunningCommit(iterator, dependencyManager.getExecutor(), folders.getProjectFolder());
            if (!running || !readInitialVersion()) {
               LOG.error("No version analyzable.");
               return false;
            }
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
         final File xmlFileFolder = TestResultManager.getXMLFileFolder(folders, folders.getProjectFolder());
         if (xmlFileFolder != null) {
            FileUtils.deleteDirectory(xmlFileFolder);
         }
         cleanTooBigLogs();
      } catch (final ParseProblemException ppe) {
         ppe.printStackTrace();
      } catch (final XmlPullParserException e) {
         e.printStackTrace();
      } catch (final InterruptedException e) {
         e.printStackTrace();
      }
   }

   public static final int MAX_SIZE_IN_MB = 10;
   
   public void cleanTooBigLogs() {
      File logFolder = folders.getLogFolder();
      File versionFolder = new File(logFolder, iterator.getTag());
      for (File clazzFolder : versionFolder.listFiles()) {
         if (clazzFolder.isDirectory()) {
            for (File methodLog : clazzFolder.listFiles()) {
               long sizeInMb = (methodLog.length() / (1024*1024));
               if (sizeInMb > MAX_SIZE_IN_MB) {
                  methodLog.delete();
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
