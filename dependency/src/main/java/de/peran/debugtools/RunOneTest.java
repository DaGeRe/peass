package de.peran.debugtools;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import de.peran.dependency.DependencyManager;
import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependency.analysis.data.TestExistenceChanges;
import de.peran.dependency.analysis.data.TestSet;
import de.peran.utils.OptionConstants;
import de.peran.vcs.GitUtils;

public class RunOneTest {
	public static void main(String[] args) throws ParseException, IOException {
		final Options options = OptionConstants.createOptions(OptionConstants.FOLDER, OptionConstants.STARTVERSION, OptionConstants.TEST);
		final CommandLineParser parser = new DefaultParser();
		final CommandLine line = parser.parse(options, args);
		final File projectFolder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));
		if (!projectFolder.exists()){
			throw new RuntimeException("Folder " + projectFolder.getAbsolutePath() + " does not exist.");
		}
		final DependencyManager handler = new DependencyManager(projectFolder);
		final String tag = line.getOptionValue(OptionConstants.STARTVERSION.getName());
		final TestSet testsToRun = new TestSet();
		final TestCase clazz = new TestCase(line.getOptionValue(OptionConstants.TEST.getName()));
		testsToRun.addTest(clazz);
		
		GitUtils.goToTag(tag, projectFolder);
		final TestExistenceChanges testExistenceChanges = handler.updateDependencies(testsToRun, tag);
		System.out.println(testExistenceChanges);
	}
}
