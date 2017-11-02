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
package de.peran;

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

import de.peran.debugtools.DependencyReadingContinueStarter;
import de.peran.dependency.reader.DependencyReaderMultiModule;
import de.peran.dependencyprocessors.VersionComparator;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.utils.OptionConstants;
import de.peran.vcs.GitCommit;
import de.peran.vcs.GitUtils;
import de.peran.vcs.VersionControlSystem;
import de.peran.vcs.VersionIterator;
import de.peran.vcs.VersionIteratorGit;

/**
 * Creates dependency information and statics for a project by running all tests
 * and identifying the dependencies with Kieker.
 * 
 * @author reichelt
 *
 */
public class DependencyReadingStarterMultiModule {
	private static final Logger LOG = LogManager.getLogger(DependencyReadingStarterMultiModule.class);

	public static void main(final String[] args) throws ParseException, FileNotFoundException, JAXBException {
		final Options options = OptionConstants.createOptions(OptionConstants.FOLDER, OptionConstants.STARTVERSION, OptionConstants.ENDVERSION, OptionConstants.OUT,
				OptionConstants.MODULE, OptionConstants.DEPENDENCYFILE);

		final CommandLineParser parser = new DefaultParser();
		final CommandLine line = parser.parse(options, args);

		final File projectFolder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));

		final File dependencyFile = DependencyReadingStarter.getDependencyFile(line, projectFolder);

		final String module = line.getOptionValue(OptionConstants.MODULE.getName());
		final File moduleFolder = new File(projectFolder, module);

		File outputFile = projectFolder.getParentFile();
		if (outputFile.isDirectory()) {
			outputFile = new File(projectFolder.getParentFile(), "ausgabe.txt");
		}

		LOG.debug("Lese {}", projectFolder.getAbsolutePath());
		final VersionControlSystem vcs = VersionControlSystem.getVersionControlSystem(projectFolder);

		System.setOut(new PrintStream(outputFile));

		final DependencyReaderMultiModule reader;
		if (vcs.equals(VersionControlSystem.GIT)) {
			final String url = GitUtils.getURL(projectFolder);
			final List<GitCommit> commits = DependencyReadingStarter.getGitCommits(line, projectFolder);
			System.out.println(url);
			if (line.hasOption(OptionConstants.DEPENDENCYFILE.getName())) {
				final File dependencyFileIn = new File(line.getOptionValue(OptionConstants.DEPENDENCYFILE.getName()));
				final Versiondependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFileIn);
				VersionComparator.setVersions(commits);
				String previousVersion;
				if (line.hasOption(OptionConstants.STARTVERSION.getName())) {
					final String startversion = line.getOptionValue(OptionConstants.STARTVERSION.getName());
					final List<Version> versionList = dependencies.getVersions().getVersion();
					DependencyReadingContinueStarter.truncateVersions(startversion, versionList);
					final int index = versionList.size() - 1;
					previousVersion = (index >= 0) ? versionList.get(index).getVersion() : dependencies.getInitialversion().getVersion();
				} else {
					
					previousVersion = VersionComparator.getPreviousVersion(dependencies.getInitialversion().getVersion());
				}
				final GitCommit previous = new GitCommit(previousVersion, "", "", "");
				final VersionIterator iterator = new VersionIteratorGit(projectFolder, commits, previous);
				reader = new DependencyReaderMultiModule(projectFolder, dependencyFile, url, iterator, moduleFolder, dependencies);

			} else {
				final VersionIterator iterator = new VersionIteratorGit(projectFolder, commits, null);
				reader = new DependencyReaderMultiModule(projectFolder, dependencyFile, url, iterator, moduleFolder);
			}

			LOG.debug("Reader initalized");
		} else {
			throw new RuntimeException("Unknown version control system");
		}
		reader.readDependencies();
	}

}
