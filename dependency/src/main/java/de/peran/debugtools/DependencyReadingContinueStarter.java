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
package de.peran.debugtools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tmatesoft.svn.core.SVNLogEntry;

import de.peran.DependencyReadingStarter;
import de.peran.DependencyStatisticAnalyzer;
import de.peran.dependency.reader.DependencyReader;
import de.peran.dependencyprocessors.VersionComparator;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.utils.OptionConstants;
import de.peran.vcs.GitCommit;
import de.peran.vcs.SVNUtils;
import de.peran.vcs.VersionControlSystem;
import de.peran.vcs.VersionIterator;
import de.peran.vcs.VersionIteratorGit;
import de.peran.vcs.VersionIteratorSVN;

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
				OptionConstants.DEPENDENCYFILE);

		final CommandLineParser parser = new DefaultParser();
		final CommandLine line = parser.parse(options, args);

		final File projectFolder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));

		final File dependencyFile;
		if (line.hasOption(OptionConstants.OUT.getName())) {
			dependencyFile = new File(line.getOptionValue(OptionConstants.OUT.getName()));
		} else {
			final File resultFolder = DependencyReadingStarter.getResultFolder();
			dependencyFile = new File(resultFolder, "deps_" + projectFolder.getName() + ".xml");
		}

		final File dependencyFileIn = new File(line.getOptionValue(OptionConstants.DEPENDENCYFILE.getName()));
		final Versiondependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFileIn);
		VersionComparator.setDependencies(dependencies);

		String previousVersion;
		if (line.hasOption(OptionConstants.STARTVERSION.getName())) {
			final String startversion = line.getOptionValue(OptionConstants.STARTVERSION.getName());
			List<Version> versionList = dependencies.getVersions().getVersion();
			truncateVersions(startversion, versionList);
			int index = versionList.size() - 1;
			previousVersion = (index >= 0) ? versionList.get(index).getVersion() : dependencies.getInitialversion().getVersion();
		} else {
			previousVersion = VersionComparator.getPreviousVersion(dependencies.getInitialversion().getVersion());
		}

		File outputFile = projectFolder.getParentFile();
		if (outputFile.isDirectory()) {
			outputFile = new File(projectFolder.getParentFile(), "ausgabe.txt");
		}

		LOG.debug("Lese {}", projectFolder.getAbsolutePath());
		final VersionControlSystem vcs = VersionControlSystem.getVersionControlSystem(projectFolder);

		System.setOut(new PrintStream(outputFile));

		final DependencyReader reader;
		if (vcs.equals(VersionControlSystem.GIT)) {
			final List<GitCommit> commits = DependencyReadingStarter.getGitCommits(line, projectFolder);
			VersionComparator.setVersions(commits);
			final GitCommit previous = new GitCommit(previousVersion, "", "", "");
			final VersionIterator iterator = new VersionIteratorGit(projectFolder, commits, previous);
			reader = new DependencyReader(projectFolder, dependencyFile, dependencies.getUrl(), iterator, dependencies);
		} else if (vcs.equals(VersionControlSystem.SVN)) {
			final List<SVNLogEntry> commits = DependencyReadingStarter.getSVNCommits(line, projectFolder);
			final String url = SVNUtils.getInstance().getWCURL(projectFolder);
			final VersionIterator iterator = new VersionIteratorSVN(projectFolder, commits, url);
			reader = new DependencyReader(projectFolder, dependencyFile, dependencies.getUrl(), iterator, dependencies);
		} else {
			throw new RuntimeException("Unknown version control system");
		}
		reader.readDependencies();
		LOG.debug("Reader initalized");

	}

	public static void truncateVersions(final String startversion, final List<Version> versions) {
		for (final java.util.Iterator<Version> it = versions.iterator(); it.hasNext();) {
			final Version version = it.next();
			if (VersionComparator.isBefore(startversion, version.getVersion()) || startversion.equals(version.getVersion())) {
				it.remove();
			}
		}
		if (versions.size() > 0) {
			LOG.debug("Letzte vorgeladene Version: " + versions.get(versions.size() - 1).getVersion());
		}
	}
}
