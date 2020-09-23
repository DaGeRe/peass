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
package de.peass.debugtools;

import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.DependencyReaderConfig;
import de.peass.DependencyReadingStarter;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.Version;
import de.peass.dependency.reader.DependencyReader;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.Constants;
import de.peass.vcs.GitCommit;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionControlSystem;
import de.peass.vcs.VersionIterator;
import de.peass.vcs.VersionIteratorGit;
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
   private DependencyReaderConfig config;

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

      final File dependencyFile = new File(config.getResultBaseFolder(), "deps_" + config.getProjectFolder().getName() + ".json");

      final File dependencyFileIn;
      if (this.dependencyFile != null) {
         dependencyFileIn = this.dependencyFile;
      } else {
         dependencyFileIn = new File(config.getResultBaseFolder(), "deps_" + config.getProjectFolder().getName() + "_continue.json");
      }

      final Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFileIn, Dependencies.class);
      VersionComparator.setVersions(GitUtils.getCommits(projectFolder));

      String previousVersion = getPreviousVersion(config.getStartversion(), projectFolder, dependencies);

      File outputFile = projectFolder.getParentFile();
      if (outputFile.isDirectory()) {
         outputFile = new File(projectFolder.getParentFile(), "ausgabe.txt");
      }

      final int timeout = config.getTimeout();

      LOG.debug("Lese {}", projectFolder.getAbsolutePath());
      final VersionControlSystem vcs = VersionControlSystem.getVersionControlSystem(projectFolder);

      System.setOut(new PrintStream(outputFile));

      final DependencyReader reader = createReader(config, dependencyFile, dependencies, previousVersion, timeout, vcs);
      reader.readCompletedVersions(dependencies);
      reader.readDependencies();

      return null;
   }

   static String getPreviousVersion(final String startversion, final File projectFolder, final Dependencies dependencies) {
      String previousVersion;
      if (startversion != null) {
         truncateVersions(startversion, dependencies.getVersions());
         previousVersion = GitUtils.getPrevious(startversion, projectFolder);
      } else {
         previousVersion = VersionComparator.getPreviousVersion(dependencies.getInitialversion().getVersion());
      }
      return previousVersion;
   }

   static DependencyReader createReader(DependencyReaderConfig config, final File dependencyFile, final Dependencies dependencies, String previousVersion,
         final int timeout, final VersionControlSystem vcs) {
      final DependencyReader reader;
      if (vcs.equals(VersionControlSystem.GIT)) {
         final List<GitCommit> commits = DependencyReadingStarter.getGitCommits(config.getStartversion(), config.getEndversion(), config.getProjectFolder());
         commits.add(0, new GitCommit(previousVersion, "", "", ""));
         // VersionComparator.setVersions(commits);
         final GitCommit previous = new GitCommit(previousVersion, "", "", "");
         final VersionIterator iterator = new VersionIteratorGit(config.getProjectFolder(), commits, previous);
         reader = new DependencyReader(config.getProjectFolder(), dependencyFile, dependencies.getUrl(), iterator, timeout);
         iterator.goTo0thCommit();
      } else if (vcs.equals(VersionControlSystem.SVN)) {
         throw new RuntimeException("SVN not supported currently.");
      } else {
         throw new RuntimeException("Unknown version control system");
      }
      return reader;
   }

   public static void truncateVersions(final String startversion, final Map<String, Version> versions) {
      for (final java.util.Iterator<Entry<String, Version>> it = versions.entrySet().iterator(); it.hasNext();) {
         final Entry<String, Version> version = it.next();
         if (VersionComparator.isBefore(startversion, version.getKey()) || version.getKey().equals(startversion)) {
            LOG.trace("Remove: " + version.getKey() + " " + VersionComparator.isBefore(startversion, version.getKey()));
            it.remove();
         }
      }
      // if (versions.size() > 0) {
      // LOG.debug("Letzte vorgeladene Version: " + versions.get(versions.size() - 1).getKey());
      // }
   }
}
