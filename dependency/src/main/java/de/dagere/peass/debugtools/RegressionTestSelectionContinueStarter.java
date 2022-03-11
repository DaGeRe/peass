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
package de.dagere.peass.debugtools;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.parameters.TestSelectionConfigMixin;
import de.dagere.peass.config.parameters.ExecutionConfigMixin;
import de.dagere.peass.config.parameters.KiekerConfigMixin;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependency.reader.VersionKeeper;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.CommitUtil;
import de.dagere.peass.vcs.GitCommit;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionControlSystem;
import de.dagere.peass.vcs.VersionIterator;
import de.dagere.peass.vcs.VersionIteratorGit;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * Creates dependency information and statics for a project by running all tests and identifying the dependencies with Kieker.
 * 
 * Starts with a given static selection file and continues its analysis.
 * 
 * @author reichelt
 *
 */
public class RegressionTestSelectionContinueStarter implements Callable<Void> {
   private static final Logger LOG = LogManager.getLogger(RegressionTestSelectionContinueStarter.class);

   @Mixin
   private TestSelectionConfigMixin config;
   
   @Mixin
   private KiekerConfigMixin kiekerConfigMixin;
   
   @Mixin
   private ExecutionConfigMixin executionConfigMixin;

   @Option(names = { "-staticSelectionFile", "--staticSelectionFile" }, description = "Path to the staticSelectionFile")
   private File staticSelectionFile;

   public static void main(final String[] args) {
      try {
         final CommandLine commandLine = new CommandLine(new RegressionTestSelectionContinueStarter());
         commandLine.execute(args);
      } catch (final Throwable t) {
         t.printStackTrace();
      }
   }

   @Override
   public Void call() throws Exception {
      final File projectFolder = config.getProjectFolder();
      if (!projectFolder.exists()) {
         throw new RuntimeException("Folder " + projectFolder.getAbsolutePath() + " does not exist.");
      }

      final File dependencyFileIn = getDependencyInFile();

      final StaticTestSelection dependencies = Constants.OBJECTMAPPER.readValue(dependencyFileIn, StaticTestSelection.class);
      VersionComparator.setVersions(GitUtils.getCommits(projectFolder, false));

      String previousVersion = getPreviousVersion(executionConfigMixin.getStartversion(), projectFolder, dependencies);

      final int timeout = executionConfigMixin.getTimeout();

      LOG.debug("Lese {}", projectFolder.getAbsolutePath());
      final VersionControlSystem vcs = VersionControlSystem.getVersionControlSystem(projectFolder);

      ResultsFolders resultsFolders = new ResultsFolders(config.getResultBaseFolder(), config.getProjectFolder().getName() + "_out");
      final DependencyReader reader = createReader(config, resultsFolders, dependencies, previousVersion, timeout, vcs);
      reader.readCompletedVersions(dependencies);
      reader.readDependencies();

      return null;
   }

   private File getDependencyInFile() {
      final File dependencyFileIn;
      if (this.staticSelectionFile != null) {
         dependencyFileIn = this.staticSelectionFile;
      } else {
         dependencyFileIn = new File(config.getResultBaseFolder(), ResultsFolders.STATIC_SELECTION_PREFIX + config.getProjectFolder().getName() + "_continue.json");
      }
      return dependencyFileIn;
   }

   /**
    * Returns the previous version before the dependency reading starts, i.e. the version before the given startversion or
    * if no startversion is given the latest version in the dependencies
    * @param startversion
    * @param projectFolder
    * @param dependencies
    * @return
    */
   static String getPreviousVersion(final String startversion, final File projectFolder, final StaticTestSelection dependencies) {
      String previousVersion;
      if (startversion != null) {
         String[] versionNames = dependencies.getVersionNames();
         int startVersionIndex = Arrays.asList(versionNames).indexOf(startversion);
         String versionAfterStartVersion = versionNames[startVersionIndex - 1];
         previousVersion = versionAfterStartVersion;
         truncateVersions(startversion, dependencies.getVersions());
      } else {
         String[] versionNames = dependencies.getVersionNames();
         String newestVersion = versionNames[versionNames.length - 1];
         previousVersion = newestVersion;
      }
      return previousVersion;
   }

   DependencyReader createReader(final TestSelectionConfigMixin config, final ResultsFolders resultsFolders, final StaticTestSelection dependencies, final String previousVersion,
         final int timeout, final VersionControlSystem vcs) {
      final DependencyReader reader;
      if (vcs.equals(VersionControlSystem.GIT)) {
         final VersionIterator iterator = createIterator(config, previousVersion);
         ExecutionConfig executionConfig = executionConfigMixin.getExecutionConfig();
         reader = new DependencyReader(config.getDependencyConfig(), new PeassFolders(config.getProjectFolder()), 
               resultsFolders, dependencies.getUrl(), iterator, new VersionKeeper(new File(resultsFolders.getStaticTestSelectionFile().getParentFile(), "nochanges.json")), 
               executionConfig, kiekerConfigMixin.getKiekerConfig(), new EnvironmentVariables());
         iterator.goTo0thCommit();
      } else if (vcs.equals(VersionControlSystem.SVN)) {
         throw new RuntimeException("SVN not supported currently.");
      } else {
         throw new RuntimeException("Unknown version control system");
      }
      return reader;
   }

   private VersionIterator createIterator(final TestSelectionConfigMixin config, final String previousVersion) {
      final List<GitCommit> commits = CommitUtil.getGitCommits(executionConfigMixin.getStartversion(), executionConfigMixin.getEndversion(), config.getProjectFolder());
      commits.add(0, new GitCommit(previousVersion, "", "", ""));
      final GitCommit previous = new GitCommit(previousVersion, "", "", "");
      final VersionIterator iterator = new VersionIteratorGit(config.getProjectFolder(), commits, previous);
      return iterator;
   }

   /**
    * Removes every version from the map that is before the given startversion
    */
   public static void truncateVersions(final String startversion, final Map<String, VersionStaticSelection> versions) {
      for (final java.util.Iterator<Entry<String, VersionStaticSelection>> it = versions.entrySet().iterator(); it.hasNext();) {
         final Entry<String, VersionStaticSelection> version = it.next();
         if (VersionComparator.isBefore(startversion, version.getKey()) || version.getKey().equals(startversion)) {
            LOG.trace("Remove: " + version.getKey() + " " + VersionComparator.isBefore(startversion, version.getKey()));
            it.remove();
         }
      }
   }
}
