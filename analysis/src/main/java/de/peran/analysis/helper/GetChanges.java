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
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import de.peran.analysis.knowledge.Change;
import de.peran.analysis.knowledge.Changes;
import de.peran.analysis.knowledge.VersionKnowledge;
import de.peran.dependency.analysis.data.ChangedEntity;
import de.peran.dependency.analysis.data.TestSet;
import de.peran.reduceddependency.ChangedTraceTests;
import de.peran.utils.OptionConstants;
import de.peran.utils.TestLoadUtil;
import de.peran.vcs.GitCommit;
import de.peran.vcs.GitUtils;

public class GetChanges {

	private static final String STARTVERSION = "9561dfcf4949e5b69231e99e38572803632121ae";
	private static final int COUNT = 100;

	public static void main(String[] args) throws ParseException, JsonParseException, JsonMappingException, IOException {
		final Options options = OptionConstants.createOptions(OptionConstants.FOLDER, OptionConstants.KNOWLEDGEFILE, OptionConstants.EXECUTIONFILE);
		final CommandLineParser parser = new DefaultParser();

		final CommandLine line = parser.parse(options, args);

		final File folder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));
		final File knowledgeFile = new File(line.getOptionValue(OptionConstants.KNOWLEDGEFILE.getName()));

		final VersionKnowledge knowledge = new ObjectMapper().readValue(knowledgeFile, VersionKnowledge.class);
		final List<GitCommit> commits = GitUtils.getCommits(folder, STARTVERSION, null);
		final ChangedTraceTests tests = line.hasOption(OptionConstants.EXECUTIONFILE.getName()) ? TestLoadUtil.loadChangedTests(line) : null;

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
