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
package de.peran.testtransformation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datacollection.DataCollectorList;

/**
 * Transforms JUnit-Tests to performance tests.
 * @author reichelt
 *
 */
public class JUnitTestTransformer {

	private static final Logger LOG = LogManager.getLogger(JUnitTestTransformer.class);

	public static final String KOPEMEDEPENDCY = "<dependency><groupId>de.dagere.kopeme</groupId><artifactId>kopeme-junit3</artifactId><version>0.8-SNAPSHOT</version><scope>test</scope></dependency>" +
			"<dependency><groupId>de.dagere.kopeme</groupId><artifactId>kopeme-junit</artifactId><version>0.8-SNAPSHOT</version><scope>test</scope></dependency>";

	private static final String IMPORTS_JUNIT3 = "import de.dagere.kopeme.junit3.KoPeMeTestcase;\nimport de.dagere.kopeme.datacollection.DataCollectorList;\n";

	private static final String IMPORTS_JUNIT4 = "import de.dagere.kopeme.annotations.Assertion;import de.dagere.kopeme.annotations.MaximalRelativeStandardDeviation;\n"
			+ "import de.dagere.kopeme.annotations.PerformanceTest;import de.dagere.kopeme.junit.testrunner.PerformanceTestRunnerJUnit;"
			+ "import org.junit.runner.RunWith;";

	protected DataCollectorList datacollectorlist;
	protected int warmupExecutions, executions;
	protected int sumTime;
	protected boolean logFullData;
	protected File projectFolder;
	private final boolean useKieker;

	public JUnitTestTransformer(final File path, final boolean logFulldata, final boolean useKieker) {
		this.projectFolder = path;
		datacollectorlist = DataCollectorList.STANDARD;
		executions = 10;
		warmupExecutions = 10;
		sumTime = 60 * 60 * 1000; // Default: 1 hour
		this.logFullData = logFulldata;
		this.useKieker = useKieker;
	}

	/**
	 * Generates Performance-Test, i.e. transforms the current unit-tests to performance tests by adding annotations to the Java-files.
	 * 
	 * @throws FileNotFoundException
	 */
	public void transformTests() {
		if (!projectFolder.exists()) {
			LOG.error("Path " + projectFolder + " not found");
		}
		LOG.trace("Searching: {}", projectFolder);

		final IOFileFilter javaFilter = new WildcardFileFilter("*.java");
		for (final File javaFile : FileUtils.listFiles(new File(projectFolder, "src/test/"), javaFilter, TrueFileFilter.INSTANCE)) {
			editJUnitClazz(javaFile);
		}

	}

	public void editJUnitClazz(final File javaFile) {
		LOG.trace("Transform java: {} iterations: {}", javaFile, executions);
		try (BufferedReader br = new BufferedReader(new FileReader(javaFile))) {
			String line;
			boolean junit3 = false, junit4 = false;
			while ((line = br.readLine()) != null) {
				if (line.contains("extends TestCase")) {
					junit3 = true;
					break;
				}
				if (line.contains("@Test")) {
					junit4 = true;
					break;
				}
			}
			if (junit3) {
				editJUnit3(javaFile);
			} else if (junit4) {
				editJUnit4(javaFile);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Edits Java so that the class extends KoPeMeTestcase instead of TestCase and that the methods for specifying the performance test are added. It is assumed that every class is in it's original
	 * state, i.e. no KoPeMeTestcase-changes have been made yet. Classes, that already extend KoPeMeTestcase are not changed.
	 * 
	 * @param javaFile
	 * @param logFullData
	 */
	private void editJUnit3(final File javaFile) {

		final File temp = new File(javaFile.getAbsolutePath() + ".1");
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(temp)); BufferedReader br = new BufferedReader(new FileReader(javaFile))) {
			String line;
			boolean imports = false, found = false;
			final String warmupMethod = "	protected int getWarmupExecutions() { return " + warmupExecutions + "; }";
			final String executionsMethod = "	protected int getExecutionTimes() { return  " + executions + "; }";
			final String logFulldataMethod = "	protected boolean logFullData() { return " + logFullData + "; }";
			final String useKiekerMethod = "	protected boolean useKieker() { return " + useKieker + "; }";
			final String maxTimeMethod = "	protected int getMaximalTime() { return " + sumTime + "; }";
			while ((line = br.readLine()) != null) {
				if (!line.startsWith("//")) {
					if (!imports && line.contains("import ")) {
						bw.write(IMPORTS_JUNIT3);
						imports = true;
					}
					if (!found && line.contains("extends TestCase")) {
						final int position = line.indexOf("extends TestCase");
						final String newLine = line.substring(0, position) + "extends KoPeMeTestcase" + line.substring(position + 16);
						bw.write(newLine);
						if (!line.contains("{")) {
							line = br.readLine();
							bw.write(line);
						}
						String dclMethode = null;
						if (datacollectorlist.equals(DataCollectorList.ONLYTIME)) {
							LOG.trace("Kollektor: " + datacollectorlist);
							dclMethode = "	protected DataCollectorList getDataCollectors() { return DataCollectorList.ONLYTIME; }";
						}
						LOG.trace("Ausführungen: {}", executions);
						if (dclMethode != null) {
							bw.write(dclMethode + "\n");
						}
						bw.write(maxTimeMethod + "\n");
						bw.write(warmupMethod + "\n");
						bw.write(executionsMethod + "\n");
						bw.write(logFulldataMethod + "\n");
						bw.write(useKiekerMethod + "\n");
						found = true;
					} else {
						bw.write(line + "\n");
					}
				}

			}
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		javaFile.delete();
		temp.renameTo(javaFile);
	}

	/**
	 * Edits Java so that the class is run with the KoPeMe-Testrunner and the methods are annotated additionally with @PerformanceTest.
	 * 
	 * @param javaFile
	 * @param logFullData
	 */
	private void editJUnit4(final File javaFile) {
		final File temp = new File(javaFile.getAbsolutePath() + ".1");
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(temp)); BufferedReader br = new BufferedReader(new FileReader(javaFile))) {
			boolean imports = false, found = false;
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.startsWith("//")) {
					if (!imports && line.contains("import ")) {
						bw.write(IMPORTS_JUNIT4);
						imports = true;
					}
					if (line.contains("@RunWith")) {
						return;
					}
					if (!found && line.startsWith("public class")) {
						bw.write("@RunWith(PerformanceTestRunnerJUnit.class)\n");
						bw.write(line);
						found = true;
					} else {
						bw.write(line + "\n");
					}
					if (line.contains("@Test")) {
						bw.write("@PerformanceTest(executionTimes = " + executions + ", warmupExecutions = " + warmupExecutions + ",\n" +
								"deviations = { @MaximalRelativeStandardDeviation(collectorname = \"de.dagere.kopeme.datacollection.TimeDataCollector\", maxvalue = 30000) })");
					}
				}
			}
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		javaFile.delete();
		temp.renameTo(javaFile);
	}

	public boolean isLogFullData() {
		return logFullData;
	}

	public void setLogFullData(final boolean logFullData) {
		this.logFullData = logFullData;
	}

	public void setDatacollectorlist(final DataCollectorList datacollectorlist) {
		this.datacollectorlist = datacollectorlist;
	}

	public void setIterations(final int iterations) {
		this.executions = iterations;
	}

	public int getExecutions() {
		return executions;
	}

	public void setWarmupExecutions(final int warmup) {
		this.warmupExecutions = warmup;
	}

	public int getWarmupExecutions() {
		return warmupExecutions;
	}

	public int getSumTime() {
		return sumTime;
	}

	public void setSumTime(final int sumTime) {
		this.sumTime = sumTime;
	}

	public File getProjectFolder() {
		return projectFolder;
	}

}
