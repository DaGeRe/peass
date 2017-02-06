/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the Affero GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     Affero GNU General Public License for more details.
 *
 *     You should have received a copy of the Affero GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.peran;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tmatesoft.svn.core.SVNLogEntry;

import de.peran.dependency.reader.DependencyReader;
import de.peran.utils.OptionConstants;
import de.peran.vcs.GitCommit;
import de.peran.vcs.GitUtils;
import de.peran.vcs.SVNUtils;
import de.peran.vcs.VersionControlSystem;

/**
 * Creates dependency information and statics for a project by running all tests and identifying the dependencies with Kieker.
 * 
 * @author reichelt
 *
 */
public class DependencyReadingStarter {
	private static final Logger LOG = LogManager.getLogger(DependencyReadingStarter.class);

	public static void main(final String[] args) throws ParseException, FileNotFoundException {
		final Options options = OptionConstants.createOptions(OptionConstants.FOLDER, OptionConstants.STARTVERSION, OptionConstants.ENDVERSION, OptionConstants.OUT);

		final CommandLineParser parser = new DefaultParser();
		final CommandLine line = parser.parse(options, args);

		final File projectFolder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));

		final File dependencyFile;
		if (line.hasOption(OptionConstants.OUT.getName())) {
			dependencyFile = new File(line.getOptionValue(OptionConstants.OUT.getName()));
		} else {
			dependencyFile = new File("dependencies.xml");
		}

		File outputFile = projectFolder.getParentFile();
		if (outputFile.isDirectory()) {
			outputFile = new File(projectFolder.getParentFile(), "ausgabe.txt");
		}

		LOG.debug("Lese {}", projectFolder.getAbsolutePath());
		final VersionControlSystem vcs = VersionControlSystem.getVersionControlSystem(projectFolder);

		System.setOut(new PrintStream(outputFile));
		// System.setErr(new PrintStream(outputFile));

		final DependencyReader reader;
		if (vcs.equals(VersionControlSystem.SVN)) {
			final String url = SVNUtils.getInstance().getWCURL(projectFolder);
			final List<SVNLogEntry> entries = getSVNCommits(line, url);
			LOG.debug("SVN commits: " + entries.stream().map(entry -> entry.getRevision()).collect(Collectors.toList()));
			reader = new DependencyReader(projectFolder, url, dependencyFile, entries);
		} else if (vcs.equals(VersionControlSystem.GIT)) {
			final List<GitCommit> commits = getGitCommits(line, projectFolder);
			reader = new DependencyReader(projectFolder, dependencyFile, commits);
			LOG.debug("Reader initalized");
		} else {
			throw new RuntimeException("Unknown version control system");
		}
		reader.readDependencies();
	}

	/**
	 * Reads the list of all SVN commits from the given URL using start- and endversion from the given CommandLine.
	 * 
	 * @param line
	 * @param url
	 * @return
	 */
	public static List<SVNLogEntry> getSVNCommits(final CommandLine line, final String url) {
		final List<SVNLogEntry> entries;
		if (line.hasOption(OptionConstants.STARTVERSION.getName())) {
			final int startversion = Integer.parseInt(line.getOptionValue(OptionConstants.STARTVERSION.getName()));
			if (line.hasOption(OptionConstants.ENDVERSION.getName())) {
				final int endversion = Integer.parseInt(line.getOptionValue(OptionConstants.ENDVERSION.getName()));
				entries = SVNUtils.getInstance().getVersions(url, startversion, endversion);
			} else {
				entries = SVNUtils.getInstance().getVersions(url, startversion);
			}

		} else {
			entries = SVNUtils.getInstance().getVersions(url);
		}
		return entries;
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
		LOG.info("Bearbeite git, Commits: {}", commits.size());
		if (line.hasOption(OptionConstants.STARTVERSION.getName())) {
			final String startversion = line.getOptionValue(OptionConstants.STARTVERSION.getName());
			if (line.hasOption(OptionConstants.ENDVERSION.getName())) {
				final String endversion = line.getOptionValue(OptionConstants.ENDVERSION.getName());
				GitUtils.filterList(startversion, endversion, commits);
			} else {
				GitUtils.filterList(startversion, null, commits);
			}
		}
		return commits;
	}
}
