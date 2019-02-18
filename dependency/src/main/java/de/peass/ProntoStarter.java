package de.peass;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.utils.OptionConstants;
import de.peass.vcs.VersionControlSystem;

public class ProntoStarter {
	private static final Logger LOG = LogManager.getLogger(DependencyReadingStarter.class);

	public static void main(final String[] args) throws ParseException, FileNotFoundException {
		final Options options = OptionConstants.createOptions(OptionConstants.FOLDER, OptionConstants.STARTVERSION, OptionConstants.ENDVERSION, OptionConstants.OUT);

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

		System.setOut(new PrintStream(outputFile));

//		final DependencyReader reader;
//		if (vcs.equals(VersionControlSystem.SVN)) {
//			throw new RuntimeException("Not supported yet.");
//		} else if (vcs.equals(VersionControlSystem.GIT)) {
//			final String url = GitUtils.getURL(projectFolder);
//			final List<GitCommit> commits = DependencyReadingStarter.getGitCommits(line, projectFolder);
//			LOG.debug(url);
//			final VersionIterator iterator = new VersionIteratorGit(projectFolder, commits, null);
//			reader = new DependencyReader(projectFolder, dependencyFile, url, iterator, 5000, dependencyFile.getParentFile());
//			LOG.debug("Reader initalized");
//		} else {
//			throw new RuntimeException("Unknown version control system");
//		}
//		reader.readDependencies();
	}

	public static File getDependencyFile(final CommandLine line, final File projectFolder) {
		final File dependencyFile;
		if (line.hasOption(OptionConstants.OUT.getName())) {
			dependencyFile = new File(line.getOptionValue(OptionConstants.OUT.getName()));
		} else {
			final File resultFolder = getResultFolder();
			dependencyFile = new File(resultFolder, "deps_" + projectFolder.getName() + ".xml");
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

}
