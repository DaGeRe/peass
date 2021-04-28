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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.CommitUtil;
import de.dagere.peass.config.DependencyReaderConfigMixin;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.Version;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.utils.Constants;
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
 * Starts with a given dependencyfile and continues its analysis.
 * 
 * @author reichelt
 *
 */
public class DependencyReadingContinueStarter implements Callable<Void> {
   private static final Logger LOG = LogManager.getLogger(DependencyReadingContinueStarter.class);

   @Mixin
   private DependencyReaderConfigMixin config;

   @Option(names = { "-dependencyfile", "--dependencyfile" }, description = "Folder for dependencyfile")
   private File dependencyFile = null;

   public static void main(final String[] args) {
      try {
         final CommandLine commandLine = new CommandLine(new DependencyReadingContinueStarter());
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

      final File dependencyFileOut = getDependencyOutFile();

      final File dependencyFileIn = getDependencyInFile();

      final Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFileIn, Dependencies.class);
      VersionComparator.setVersions(GitUtils.getCommits(projectFolder, false));

      String previousVersion = getPreviousVersion(config.getStartversion(), projectFolder, dependencies);

      final int timeout = config.getTimeout();

      LOG.debug("Lese {}", projectFolder.getAbsolutePath());
      final VersionControlSystem vcs = VersionControlSystem.getVersionControlSystem(projectFolder);

      final DependencyReader reader = createReader(config, dependencyFileOut, dependencies, previousVersion, timeout, vcs);
      reader.readCompletedVersions(dependencies);
      reader.readDependencies();

      return null;
   }

   private File getDependencyInFile() {
      final File dependencyFileIn;
      if (this.dependencyFile != null) {
         dependencyFileIn = this.dependencyFile;
      } else {
         dependencyFileIn = new File(config.getResultBaseFolder(), "deps_" + config.getProjectFolder().getName() + "_continue.json");
      }
      return dependencyFileIn;
   }

   private File getDependencyOutFile() {
      final File dependencyFile = new File(config.getResultBaseFolder(), "deps_" + config.getProjectFolder().getName() + ".json");
      if (!dependencyFile.getParentFile().exists()) {
         dependencyFile.getParentFile().mkdirs();
      }
      return dependencyFile;
   }

   /**
    * Returns the previous version before the dependency reading starts, i.e. the version before the given startversion or
    * if no startversion is given the latest version in the dependencies
    * @param startversion
    * @param projectFolder
    * @param dependencies
    * @return
    */
   static String getPreviousVersion(final String startversion, final File projectFolder, final Dependencies dependencies) {
      String previousVersion;
      if (startversion != null) {
         truncateVersions(startversion, dependencies.getVersions());
         previousVersion = GitUtils.getPrevious(startversion, projectFolder);
      } else {
         String[] versionNames = dependencies.getVersionNames();
         String newestVersion = versionNames[versionNames.length - 1];
         previousVersion = newestVersion;
      }
      return previousVersion;
   }

   static DependencyReader createReader(final DependencyReaderConfigMixin config, final File dependencyFile, final Dependencies dependencies, final String previousVersion,
         final int timeout, final VersionControlSystem vcs) {
      final DependencyReader reader;
      if (vcs.equals(VersionControlSystem.GIT)) {
         final VersionIterator iterator = createIterator(config, previousVersion);
         ExecutionConfig executionConfig = config.getExecutionConfig();
         reader = new DependencyReader(config.getDependencyConfig(), config.getProjectFolder(), dependencyFile, dependencies.getUrl(), iterator, executionConfig,
               new EnvironmentVariables());
         iterator.goTo0thCommit();
      } else if (vcs.equals(VersionControlSystem.SVN)) {
         throw new RuntimeException("SVN not supported currently.");
      } else {
         throw new RuntimeException("Unknown version control system");
      }
      return reader;
   }

   private static VersionIterator createIterator(final DependencyReaderConfigMixin config, final String previousVersion) {
      final List<GitCommit> commits = CommitUtil.getGitCommits(config.getStartversion(), config.getEndversion(), config.getProjectFolder());
      commits.add(0, new GitCommit(previousVersion, "", "", ""));
      final GitCommit previous = new GitCommit(previousVersion, "", "", "");
      final VersionIterator iterator = new VersionIteratorGit(config.getProjectFolder(), commits, previous);
      return iterator;
   }

   public static void truncateVersions(final String startversion, final Map<String, Version> versions) {
      for (final java.util.Iterator<Entry<String, Version>> it = versions.entrySet().iterator(); it.hasNext();) {
         final Entry<String, Version> version = it.next();
         if (VersionComparator.isBefore(startversion, version.getKey()) || version.getKey().equals(startversion)) {
            LOG.trace("Remove: " + version.getKey() + " " + VersionComparator.isBefore(startversion, version.getKey()));
            it.remove();
         }
      }
   }
}
