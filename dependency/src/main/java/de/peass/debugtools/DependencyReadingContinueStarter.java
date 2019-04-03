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
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.DependencyReadingStarter;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.Version;
import de.peass.dependency.reader.DependencyReader;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.statistics.DependencyStatisticAnalyzer;
import de.peass.utils.OptionConstants;
import de.peass.vcs.GitCommit;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionControlSystem;
import de.peass.vcs.VersionIterator;
import de.peass.vcs.VersionIteratorGit;

/**
 * Creates dependency information and statics for a project by running all tests
 * and identifying the dependencies with Kieker.
 * 
 * Starts with a given dependencyfile and continues its analysis.
 * 
 * @author reichelt
 *
 */
public class DependencyReadingContinueStarter {
	private static final Logger LOG = LogManager.getLogger(DependencyReadingContinueStarter.class);

	public static void main(final String[] args) throws ParseException, FileNotFoundException, JAXBException {
		final Options options = OptionConstants.createOptions(OptionConstants.FOLDER, OptionConstants.STARTVERSION, OptionConstants.ENDVERSION, OptionConstants.OUT,
				OptionConstants.DEPENDENCYFILE, OptionConstants.TIMEOUT);

		final CommandLineParser parser = new DefaultParser();
		final CommandLine line = parser.parse(options, args);

		final File projectFolder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));

		final File dependencyFile = DependencyReadingStarter.getDependencyFile(line, projectFolder);

		final File dependencyFileIn = new File(line.getOptionValue(OptionConstants.DEPENDENCYFILE.getName()));
		final Dependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFileIn);
		VersionComparator.setVersions(GitUtils.getCommits(projectFolder));

		String previousVersion = getPreviousVersion(line, projectFolder, dependencies);

		File outputFile = projectFolder.getParentFile();
		if (outputFile.isDirectory()) {
			outputFile = new File(projectFolder.getParentFile(), "ausgabe.txt");
		}

		final int timeout = Integer.parseInt(line.getOptionValue(OptionConstants.TIMEOUT.getName(), "3"));
		
		LOG.debug("Lese {}", projectFolder.getAbsolutePath());
		final VersionControlSystem vcs = VersionControlSystem.getVersionControlSystem(projectFolder);

		System.setOut(new PrintStream(outputFile));

		final DependencyReader reader = createReader(line, projectFolder, dependencyFile, dependencies, previousVersion, timeout, vcs);
		reader.readDependencies();
		LOG.debug("Reader initalized");

	}

   static String getPreviousVersion(final CommandLine line, final File projectFolder, final Dependencies dependencies) {
      String previousVersion;
		if (line.hasOption(OptionConstants.STARTVERSION.getName())) {
			final String startversion = line.getOptionValue(OptionConstants.STARTVERSION.getName());
			truncateVersions(startversion, dependencies.getVersions());
		   previousVersion = GitUtils.getPrevious(startversion, projectFolder);
		} else {
			previousVersion = VersionComparator.getPreviousVersion(dependencies.getInitialversion().getVersion());
		}
      return previousVersion;
   }

   static DependencyReader createReader(final CommandLine line, final File projectFolder, final File dependencyFile, final Dependencies dependencies, String previousVersion,
         final int timeout, final VersionControlSystem vcs) {
      final DependencyReader reader;
		if (vcs.equals(VersionControlSystem.GIT)) {
			final List<GitCommit> commits = DependencyReadingStarter.getGitCommits(line, projectFolder);
			commits.add(0, new GitCommit(previousVersion, "", "", ""));
			VersionComparator.setVersions(commits);
			final GitCommit previous = new GitCommit(previousVersion, "", "", "");
			final VersionIterator iterator = new VersionIteratorGit(projectFolder, commits, previous);
			reader = new DependencyReader(projectFolder, dependencyFile, dependencies.getUrl(), iterator, dependencies, timeout);
			iterator.goTo0thCommit();
		} else if (vcs.equals(VersionControlSystem.SVN)) {
		   throw new RuntimeException("SVN not supported currently.");
		} else {
			throw new RuntimeException("Unknown version control system");
		}
      return reader;
   }

	public static void truncateVersions(final String startversion, final Map<String,Version> versions) {
	   for (final java.util.Iterator<Entry<String, Version>> it = versions.entrySet().iterator(); it.hasNext();) {
			final Entry<String, Version> version = it.next();
			if (VersionComparator.isBefore(startversion, version.getKey()) || version.getKey().equals(startversion)) {
			   LOG.trace("Remove: " + version.getKey() + " " + VersionComparator.isBefore(startversion, version.getKey()));
				it.remove();
			}
		}
//		if (versions.size() > 0) {
//			LOG.debug("Letzte vorgeladene Version: " + versions.get(versions.size() - 1).getKey());
//		}
	}
}
