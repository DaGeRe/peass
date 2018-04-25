package de.peran.evaluation.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.peran.DependencyReadingStarter;
import de.peran.dependency.reader.DependencyReader;
import de.peran.utils.OptionConstants;
import de.peran.vcs.GitCommit;
import de.peran.vcs.GitUtils;
import de.peran.vcs.VersionIterator;
import de.peran.vcs.VersionIteratorGit;

/**
 * Base class for those classes who evaluate a method against DePeC. Therefore, the evaluation itself for the specific method needs to be added.
 * 
 * @author reichelt
 *
 */
public abstract class Evaluator {
	private static final Logger LOG = LogManager.getLogger(Evaluator.class);

	public static final ObjectMapper OBJECTMAPPER = new ObjectMapper();

	static {
		OBJECTMAPPER.enable(SerializationFeature.INDENT_OUTPUT);
	}

	protected final File projectFolder;
	protected final VersionIterator iterator;
	protected final File debugFolder;
	protected final File resultFolder;
	protected final EvaluationProject evaluation;
	protected final SysoutTestExecutor executor;

	public Evaluator(String type, String[] args) throws ParseException {
		final Options options = OptionConstants.createOptions(OptionConstants.FOLDER, OptionConstants.STARTVERSION, OptionConstants.ENDVERSION, OptionConstants.OUT);

		final CommandLineParser parser = new DefaultParser();
		final CommandLine line = parser.parse(options, args);

		final File projectFolder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));

		File outputFile = projectFolder.getParentFile();
		if (outputFile.isDirectory()) {
			outputFile = new File(projectFolder.getParentFile(), "ausgabe.txt");
		}

		LOG.debug("Lese {}", projectFolder.getAbsolutePath());
		this.projectFolder = projectFolder;

		final String url = GitUtils.getURL(projectFolder);
		final List<GitCommit> commits = DependencyReadingStarter.getGitCommits(line, projectFolder);

		iterator = new VersionIteratorGit(projectFolder, commits, null);

		executor = new SysoutTestExecutor(projectFolder);
		DependencyReader.searchFirstRunningCommit(iterator, executor, projectFolder);

		debugFolder = new File(projectFolder, "debug_" + type);
		if (!debugFolder.exists()) {
			debugFolder.mkdir();
		}
		System.out.println("Commit: " + iterator.getTag());

		resultFolder = new File("evaluation_results");
		if (!resultFolder.exists()) {
			resultFolder.mkdir();
		}

		evaluation = new EvaluationProject();
		evaluation.setUrl(url);
	}

	public abstract void evaluate();

	/**
	 * Reads tests from a file container a maven test output.
	 * 
	 * @param currentFile
	 *            Logfile of maven test
	 * @return Count of test methods of every testfile in the current logfile
	 */
	protected EvaluationVersion getTestsFromFile(final File currentFile) {
		final EvaluationVersion currentVersion = new EvaluationVersion();
		try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
			String line;
			boolean inTests = true;
			// Map<String, Integer> tests = new HashMap<>();
			while ((line = reader.readLine()) != null) {
				if (line.equals("-------------------------------------------------------")) {
					final String test = reader.readLine();
					final String nextLine = reader.readLine();
					if (test.contains("T E S T S")
							&& nextLine.equals("-------------------------------------------------------")) {
						inTests = true;
					}
				}

				if (inTests) {
					LOG.debug("Line: {}", line);
					final String runningString = "Running ";
					if (line.startsWith(runningString)) {
						final String testname = line.substring(runningString.length());
						final String testsRun = reader.readLine();
						LOG.debug("Line: {}", testsRun);
						final String testsRunString = "Tests run: ";
						if (testsRun != null && testsRun.startsWith(testsRunString)) {
							final String[] splitted = testsRun.split(",");
							final String runCount = splitted[0].substring(testsRunString.length());
							final int count = Integer.parseInt(runCount);
							LOG.info("Test: " + testname + " " + count);
							currentVersion.getTestcaseExecutions().put(testname, count);
						} else {
							LOG.error("Unexpected line: " + testsRun);
						}

						// tests.put(arg0, arg1)
					}
				}
			}

		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return currentVersion;
	}
}
