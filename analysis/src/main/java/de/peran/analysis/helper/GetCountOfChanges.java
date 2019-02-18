package de.peran.analysis.helper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.peass.analysis.changes.Change;
import de.peass.analysis.changes.Changes;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.utils.OptionConstants;
import de.peass.utils.TestLoadUtil;
import de.peass.vcs.GitCommit;
import de.peass.vcs.GitUtils;

public class GetCountOfChanges {

	private static final String STARTVERSION = "9561dfcf4949e5b69231e99e38572803632121ae";
	private static final int COUNT = 100;

	public static void main(String[] args) throws ParseException, JsonParseException, JsonMappingException, IOException {
		final Options options = OptionConstants.createOptions(OptionConstants.FOLDER, OptionConstants.KNOWLEDGEFILE, OptionConstants.EXECUTIONFILE);
		final CommandLineParser parser = new DefaultParser();

		final CommandLine line = parser.parse(options, args);

		final File folder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));
		final File knowledgeFile = new File(line.getOptionValue(OptionConstants.KNOWLEDGEFILE.getName()));

		final ProjectChanges knowledge = new ObjectMapper().readValue(knowledgeFile, ProjectChanges.class);
		final List<GitCommit> commits = GitUtils.getCommits(folder, STARTVERSION, null);
		final ExecutionData tests = line.hasOption(OptionConstants.EXECUTIONFILE.getName()) ? TestLoadUtil.loadChangedTests(line) : null;

		int changes = 0, executedTests = 0;
		for (int i = 0; i < COUNT; i++) {
			final GitCommit commit = commits.get(i);
			final Changes version = knowledge.getVersion(commit.getTag());
			if (version != null) {
				if (version.getTestcaseChanges().size() > 0){
					System.out.println("Version: " + commit.getTag() + "(" + i + ")");
					for (final Entry<String, List<Change>> testclass : version.getTestcaseChanges().entrySet()) {
						changes += testclass.getValue().size();
					}
				}
			}
			if (tests != null){
				final TestSet versionTests = tests.getVersions().get(commit.getTag());
				if (versionTests != null){
					for (final Entry<ChangedEntity, List<String>> testclass : versionTests.getTestcases().entrySet()) {
						executedTests += testclass.getValue().size();
					}
				}
			}
		}
		System.out.println("Last Version: " + commits.get(COUNT-1).getTag());
		System.out.println("Changes until version " + COUNT + ": " + changes);
		if (tests != null){
			System.out.println("Tests executed until version " + COUNT + ": " + executedTests);
		}
	}
}
