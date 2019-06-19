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
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.reader.DependencyReader;
import de.peass.dependency.reader.VersionKeeper;
import de.peass.utils.OptionConstants;
import de.peass.vcs.GitCommit;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionControlSystem;
import de.peass.vcs.VersionIterator;
import de.peass.vcs.VersionIteratorGit;

/**
 * Creates dependency information and statics for a project by running all tests and identifying the dependencies with Kieker.
 * 
 * @author reichelt
 *
 */
public class DependencyReadingStarter {
   private static final Logger LOG = LogManager.getLogger(DependencyReadingStarter.class);

   public static void main(final String[] args) throws ParseException, FileNotFoundException {
      final Options options = OptionConstants.createOptions(OptionConstants.FOLDER, OptionConstants.STARTVERSION, OptionConstants.ENDVERSION, OptionConstants.OUT,
            OptionConstants.TIMEOUT);

      final CommandLineParser parser = new DefaultParser();
      final CommandLine line = parser.parse(options, args);

      final File projectFolder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));
      if (!projectFolder.exists()) {
         throw new RuntimeException("Folder " + projectFolder.getAbsolutePath() + " does not exist.");
      }

      final File dependencyFile = getDependencyFile(line, projectFolder);

      File outputFile = projectFolder.getParentFile();
      if (outputFile.isDirectory()) {
         outputFile = new File(projectFolder.getParentFile(), "ausgabe.txt");
      }

      LOG.debug("Lese {}", projectFolder.getAbsolutePath());
      final VersionControlSystem vcs = VersionControlSystem.getVersionControlSystem(projectFolder);

      final int timeout = Integer.parseInt(line.getOptionValue(OptionConstants.TIMEOUT.getName(), "5"));

      System.setOut(new PrintStream(outputFile));

      final VersionKeeper nonRunning = new VersionKeeper(new File(dependencyFile.getParentFile(), "nonRunning_" + projectFolder.getName() + ".json"));
      final VersionKeeper nonChanges = new VersionKeeper(new File(dependencyFile.getParentFile(), "nonChanges_" + projectFolder.getName() + ".json"));

      final DependencyReader reader;
      if (vcs.equals(VersionControlSystem.SVN)) {
         throw new RuntimeException("SVN not supported currently.");
      } else if (vcs.equals(VersionControlSystem.GIT)) {
         final String url = GitUtils.getURL(projectFolder);
         final List<GitCommit> commits = getGitCommits(line, projectFolder);
         LOG.debug(url);
         final VersionIterator iterator = new VersionIteratorGit(projectFolder, commits, null);
         reader = new DependencyReader(projectFolder, dependencyFile, url, iterator, timeout, nonRunning, nonChanges);
         LOG.debug("Reader initalized");
      } else {
         throw new RuntimeException("Unknown version control system");
      }
      reader.readDependencies();
   }

   public static File getDependencyFile(final CommandLine line, final File projectFolder) {
      final File dependencyFile;
      if (line.hasOption(OptionConstants.OUT.getName())) {
         dependencyFile = new File(line.getOptionValue(OptionConstants.OUT.getName()));
      } else {
         final File resultFolder = getResultFolder();
         dependencyFile = new File(resultFolder, "deps_" + projectFolder.getName() + ".json");
      }
      return dependencyFile;
   }

   public static File getResultFolder() {
      final File resultFolder = new File("results");
      if (!resultFolder.exists()) {
         resultFolder.mkdir();
      }
      return resultFolder;
   }

   /**
    * Reads the list of all git commits from the given URL using start- and endversion from the given CommandLine.
    * 
    * @param line
    * @param url
    * @return
    */
   public static List<GitCommit> getGitCommits(final CommandLine line, final File projectFolder) {
      final List<GitCommit> commits = GitUtils.getCommits(projectFolder);

      LOG.info("Processing git repo, commits: {}", commits.size());
      // LOG.debug("First Commits: {}", commits.subList(0, 10));
      if (line.hasOption(OptionConstants.STARTVERSION.getName())) {
         final String startversion = line.getOptionValue(OptionConstants.STARTVERSION.getName());
         if (line.hasOption(OptionConstants.ENDVERSION.getName())) {
            final String endversion = line.getOptionValue(OptionConstants.ENDVERSION.getName());
            GitUtils.filterList(startversion, endversion, commits);
         } else {
            GitUtils.filterList(startversion, null, commits);
            LOG.debug("First Commits: {}", commits.size() > 10 ? commits.subList(0, 10) : commits.subList(0, commits.size() - 1));
         }
      } else if (line.hasOption(OptionConstants.ENDVERSION.getName())) {
         final String endversion = line.getOptionValue(OptionConstants.ENDVERSION.getName());
         GitUtils.filterList(null, endversion, commits);
      }
      LOG.info(commits);
      return commits;
   }
}
