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
import de.dagere.peass.config.parameters.ExecutionConfigMixin;
import de.dagere.peass.config.parameters.KiekerConfigMixin;
import de.dagere.peass.config.parameters.TestSelectionConfigMixin;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.reader.CommitKeeper;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.CommitIterator;
import de.dagere.peass.vcs.CommitIteratorGit;
import de.dagere.peass.vcs.CommitUtil;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionControlSystem;
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
      CommitComparatorInstance comparator = new CommitComparatorInstance(GitUtils.getCommits(projectFolder, false));
      
      VersionComparator.setVersions(GitUtils.getCommits(projectFolder, false));

      String previousCommit = getPreviousCommit(executionConfigMixin.getStartcommit(), projectFolder, dependencies, comparator);

      final long timeout = executionConfigMixin.getTimeout();

      LOG.debug("Lese {}", projectFolder.getAbsolutePath());
      final VersionControlSystem vcs = VersionControlSystem.getVersionControlSystem(projectFolder);

      ResultsFolders resultsFolders = new ResultsFolders(config.getResultBaseFolder(), config.getProjectFolder().getName() + "_out");
      final DependencyReader reader = createReader(config, resultsFolders, dependencies, previousCommit, timeout, vcs);
      reader.readCompletedCommits(dependencies, comparator);
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
    * Returns the previous commit before the dependency reading starts, i.e. the commit before the given startcommit or
    * if no startcommit is given the latest commit in the dependencies
    * @param startcommit
    * @param projectFolder
    * @param dependencies
    * @return
    */
   static String getPreviousCommit(final String startcommit, final File projectFolder, final StaticTestSelection dependencies, CommitComparatorInstance comparator) {
      String previousCommit;
      String[] commitNames = dependencies.getCommitNames();
      if (startcommit != null) {
         int startCommitIndex = Arrays.asList(commitNames).indexOf(startcommit);
         String commitAfterStartCommit = commitNames[startCommitIndex - 1];
         previousCommit = commitAfterStartCommit;
         truncateCommits(startcommit, dependencies.getCommits(), comparator);
      } else {
         String newestCommit = commitNames[commitNames.length - 1];
         previousCommit = newestCommit;
      }
      return previousCommit;
   }

   DependencyReader createReader(final TestSelectionConfigMixin config, final ResultsFolders resultsFolders, final StaticTestSelection dependencies, final String previousCommit,
         final long timeout, final VersionControlSystem vcs) {
      final DependencyReader reader;
      if (vcs.equals(VersionControlSystem.GIT)) {
         final CommitIterator iterator = createIterator(config, previousCommit);
         ExecutionConfig executionConfig = executionConfigMixin.getExecutionConfig();
         reader = new DependencyReader(config.getDependencyConfig(), new PeassFolders(config.getProjectFolder()), 
               resultsFolders, dependencies.getUrl(), iterator, new CommitKeeper(new File(resultsFolders.getStaticTestSelectionFile().getParentFile(), "nochanges.json")), 
               executionConfig, kiekerConfigMixin.getKiekerConfig(), new EnvironmentVariables());
         iterator.goTo0thCommit();
      } else if (vcs.equals(VersionControlSystem.SVN)) {
         throw new RuntimeException("SVN not supported currently.");
      } else {
         throw new RuntimeException("Unknown version control system");
      }
      return reader;
   }

   private CommitIterator createIterator(final TestSelectionConfigMixin config, final String previousCommit) {
      final List<String> commits = CommitUtil.getGitCommits(executionConfigMixin.getStartcommit(), executionConfigMixin.getEndcommit(), config.getProjectFolder());
      commits.add(0, previousCommit);
      final CommitIterator iterator = new CommitIteratorGit(config.getProjectFolder(), commits, previousCommit);
      return iterator;
   }

   /**
    * Removes every commit from the map that is before the given startcommit
    */
   public static void truncateCommits(final String startcommit, final Map<String, CommitStaticSelection> commits, CommitComparatorInstance comparator) {
      for (final java.util.Iterator<Entry<String, CommitStaticSelection>> it = commits.entrySet().iterator(); it.hasNext();) {
         final Entry<String, CommitStaticSelection> commit = it.next();
         if (comparator.isBefore(startcommit, commit.getKey()) || commit.getKey().equals(startcommit)) {
            LOG.trace("Remove: " + commit.getKey() + " " + comparator.isBefore(startcommit, commit.getKey()));
            it.remove();
         }
      }
   }
}
