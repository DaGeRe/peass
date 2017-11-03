package de.peran.evaluation.base;

/*-
 * #%L
 * peran-evaluation
 * %%
 * Copyright (C) 2017 DaGeRe
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.peran.dependency.reader.DependencyReader;
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

	public Evaluator(final File projectFolder, final String type) {
		this.projectFolder = projectFolder;

		final String url = GitUtils.getURL(projectFolder);
		final List<GitCommit> commits = GitUtils.getCommits(projectFolder);

		iterator = new VersionIteratorGit(projectFolder, commits, null);

		// while (!iterator.getTag().startsWith("956")) {
		// iterator.goToNextCommit();
		// }

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
