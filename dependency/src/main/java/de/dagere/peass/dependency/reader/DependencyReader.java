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
package de.dagere.peass.dependency.reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.github.javaparser.ParseProblemException;

import de.dagere.peass.config.DependencyConfig;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.KiekerResultManager;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.vcs.VersionIterator;

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
   
   public DependencyReader(final DependencyConfig dependencyConfig, final PeASSFolders folders, 
         final File dependencyFile, final String url, final VersionIterator iterator, 
         final ChangeManager changeManager, final ExecutionConfig executionConfig, final EnvironmentVariables env) {
      super(dependencyConfig, new Dependencies(), folders, dependencyFile, new VersionKeeper(new File("/dev/null")), iterator, executionConfig, env);

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
   public DependencyReader(final DependencyConfig dependencyConfig, final PeASSFolders folders, final File dependencyFile, final String url, final VersionIterator iterator, 
         final VersionKeeper nochange, final ExecutionConfig executionConfig, final EnvironmentVariables env) {
      super(dependencyConfig, new Dependencies(), folders, dependencyFile, nochange, iterator, executionConfig, env);

      dependencyResult.setUrl(url);
      
      changeManager = new ChangeManager(folders, iterator);
      
   }

   /**
    * Reads the dependencies of the tests
    */
   public boolean readDependencies() {
      try {
         LOG.debug("Analysing {} entries", iterator.getRemainingSize());

         prunedSize += dependencyManager.getDependencyMap().size();

         changeManager.saveOldClasses();
         lastRunningVersion = iterator.getTag();
         while (iterator.hasNextCommit()) {
            iterator.goToNextCommit();
            readVersion();
         }

         LOG.debug("Finished dependency-reading");
         return true;
      } catch (IOException e) {
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
         TooBigLogCleaner.cleanTooBigLogs(folders, iterator.getTag());
      } catch (final ParseProblemException | XmlPullParserException | InterruptedException | IOException ppe) {
         LOG.debug("Exception while reading a version");
         ppe.printStackTrace();
      } 
   }

}
