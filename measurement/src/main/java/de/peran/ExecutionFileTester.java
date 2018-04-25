package de.peran;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peran.dependency.PeASSFolderUtil;
import de.peran.dependency.analysis.data.ChangedEntity;
import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependency.analysis.data.TestSet;
import de.peran.dependencyprocessors.DependencyTester;
import de.peran.reduceddependency.ChangedTraceTests;
import de.peran.utils.OptionConstants;
import de.peran.utils.TestLoadUtil;
import de.peran.vcs.GitUtils;
import de.peran.vcs.VersionControlSystem;

public class ExecutionFileTester {

	private static final Logger LOG = LogManager.getLogger(ExecutionFileTester.class);
	
	protected File projectFolder;
	protected VersionControlSystem vcs;
	protected final CommandLine line;
	protected final String startversion;
	protected final String endversion;
	private final ChangedTraceTests tests;
	private final DependencyTester tester;
	protected final Map<TestCase, String> lastTestcaseCalls = new HashMap<>();

	public ExecutionFileTester(String[] args) throws ParseException, JsonParseException, JsonMappingException, IOException {
		final Options options = OptionConstants.createOptions(OptionConstants.FOLDER, OptionConstants.WARMUP, OptionConstants.ITERATIONS, OptionConstants.VMS,
				OptionConstants.STARTVERSION, OptionConstants.ENDVERSION,
				OptionConstants.EXECUTIONFILE, OptionConstants.REPETITIONS,
				OptionConstants.USEKIEKER);
		final CommandLineParser parser = new DefaultParser();

		line = parser.parse(options, args);

		final File executionFile = new File(line.getOptionValue(OptionConstants.EXECUTIONFILE.getName()));
		tests = TestLoadUtil.loadChangedTests(line);

		projectFolder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));
		if (!projectFolder.exists()) {
			GitUtils.downloadProject(tests.getUrl(), projectFolder);
		}
		PeASSFolderUtil.setProjectFolder(projectFolder);

		startversion = line.getOptionValue(OptionConstants.STARTVERSION.getName(), null);
		endversion = line.getOptionValue(OptionConstants.ENDVERSION.getName(), null);

		final int warmup = Integer.parseInt(line.getOptionValue(OptionConstants.WARMUP.getName(), "10"));
		final int iterationen = Integer.parseInt(line.getOptionValue(OptionConstants.ITERATIONS.getName(), "10"));
		final int vms = Integer.parseInt(line.getOptionValue(OptionConstants.VMS.getName(), "15"));
		final int repetitions = Integer.parseInt(line.getOptionValue(OptionConstants.REPETITIONS.getName(), "1"));
		final boolean useKieker = Boolean.parseBoolean(line.getOptionValue(OptionConstants.USEKIEKER.getName(), "false"));
		tester = new DependencyTester(projectFolder, warmup, iterationen, vms, true, repetitions, useKieker);

	}

	public static void main(final String[] args) throws JsonParseException, JsonMappingException, ParseException, IOException {
		final ExecutionFileTester starter = new ExecutionFileTester(args);
		starter.processCommandline();
	}

	private void processCommandline() {
		for (final Entry<String, TestSet> version : tests.getVersions().entrySet()) {
			final boolean executeThisTest = true;
			if (executeThisTest) {
				final TestSet calls = version.getValue();
				for (final Map.Entry<ChangedEntity, List<String>> tests : calls.getTestcases().entrySet()) {
					for (final String method : tests.getValue()) {
						final TestCase testcase = new TestCase(tests.getKey().getJavaClazzName(), method);
						final String versionOld = lastTestcaseCalls.get(testcase);
						try {
							executeCompareTests(version.getKey(), versionOld, testcase);
						} catch (IOException | InterruptedException | JAXBException e) {
							e.printStackTrace();
						}
						lastTestcaseCalls.put(testcase, version.getKey());
					}
				}
			}
		}
	}
	
	//TODO: Simply copied..
	protected void executeCompareTests(final String version, final String versionOld, final TestCase testcase) throws IOException, InterruptedException, JAXBException {
		LOG.info("Executing test " + testcase.getClazz() + " " + testcase.getMethod() + " in versions {} and {}", versionOld, version);

		File logFile = new File(PeASSFolderUtil.getLogFolder(), version);
		if (logFile.exists()) {
			logFile = new File(PeASSFolderUtil.getLogFolder(), version + "_new");
		}
		logFile.mkdir();

		final TestSet testset = new TestSet();
		testset.addTest(testcase);
		for (int vmid = 0; vmid < tester.getVMCount(); vmid++) {
			tester.evaluateOnce(testset, versionOld, vmid, logFile);
			tester.evaluateOnce(testset, version, vmid, logFile);
		}
	}
}
