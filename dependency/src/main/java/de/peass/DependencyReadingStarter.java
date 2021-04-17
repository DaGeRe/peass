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
package de.peass;

import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.config.DependencyReaderConfigMixin;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependency.reader.DependencyReader;
import de.peass.dependency.reader.FirstRunningVersionFinder;
import de.peass.dependency.reader.VersionKeeper;
import de.peass.vcs.GitCommit;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionControlSystem;
import de.peass.vcs.VersionIterator;
import de.peass.vcs.VersionIteratorGit;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

/**
 * Creates dependency information and statics for a project by running all tests and identifying the dependencies with Kieker.
 * 
 * @author reichelt
 *
 */
@Command(description = "Reads the dependencies", name = "readDependencies")
public class DependencyReadingStarter implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(DependencyReadingStarter.class);

   @Mixin
   private DependencyReaderConfigMixin config;

   public static void main(final String[] args) {
      try {
         final CommandLine commandLine = new CommandLine(new DependencyReadingParallelStarter());
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

      File outputFile = projectFolder.getParentFile();
      if (outputFile.isDirectory()) {
         outputFile = new File(projectFolder.getParentFile(), "ausgabe.txt");
      }

      LOG.debug("Lese {}", projectFolder.getAbsolutePath());
      final VersionControlSystem vcs = VersionControlSystem.getVersionControlSystem(projectFolder);

      System.setOut(new PrintStream(outputFile));

      final VersionKeeper nonRunning = new VersionKeeper(new File(dependencyFile.getParentFile(), "nonRunning_" + projectFolder.getName() + ".json"));
      final VersionKeeper nonChanges = new VersionKeeper(new File(dependencyFile.getParentFile(), "nonChanges_" + projectFolder.getName() + ".json"));

      final DependencyReader reader;
      if (vcs.equals(VersionControlSystem.SVN)) {
         throw new RuntimeException("SVN not supported currently.");
      } else if (vcs.equals(VersionControlSystem.GIT)) {
         final String url = GitUtils.getURL(projectFolder);
         final List<GitCommit> commits = getGitCommits(config.getStartversion(), config.getEndversion(), projectFolder);
         LOG.debug(url);
         final VersionIterator iterator = new VersionIteratorGit(projectFolder, commits, null);
         boolean init = new FirstRunningVersionFinder(new PeASSFolders(projectFolder), nonRunning, iterator, config.getExecutionConfig(), new EnvironmentVariables()).searchFirstRunningCommit();
         if (!init) {
            throw new RuntimeException("No analyzable version");
         }
         reader = new DependencyReader(new PeASSFolders(projectFolder), dependencyFile, url, iterator, nonChanges, config.getExecutionConfig(), new EnvironmentVariables());
         LOG.debug("Reader initalized");
      } else {
         throw new RuntimeException("Unknown version control system");
      }
      if (!reader.readInitialVersion()) {
         LOG.error("Analyzing first version was not possible");
      } else {
         reader.readDependencies();
      }

      return null;
   }

   public static List<GitCommit> getGitCommits(final String startversion, final String endversion, final File projectFolder) {
      final List<GitCommit> commits = GitUtils.getCommits(projectFolder);

      LOG.info("Processing git repo, commits: {}", commits.size());
      // LOG.debug("First Commits: {}", commits.subList(0, 10));
      if (startversion != null) {
         if (endversion != null) {
            GitUtils.filterList(startversion, endversion, commits);
         } else {
            GitUtils.filterList(startversion, null, commits);
            LOG.debug("First Commits: {}", commits.size() > 10 ? commits.subList(0, 10) : commits.subList(0, commits.size() - 1));
         }
      } else if (endversion != null) {
         GitUtils.filterList(null, endversion, commits);
      }
      LOG.info(commits);
      return commits;
   }
}
