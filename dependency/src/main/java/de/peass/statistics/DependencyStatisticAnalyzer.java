package de.peass.statistics;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependency.persistence.Version;
import de.peass.utils.Constants;
import de.peass.utils.OptionConstants;

/**
 * Analyzes a dependency file and prints some statistical information about it.
 * 
 * @author reichelt
 *
 */
public class DependencyStatisticAnalyzer {

	private static final Logger LOG = LogManager.getLogger(DependencyStatisticAnalyzer.class);

	public static void main(final String[] args) throws JAXBException, ParseException, JsonParseException, JsonMappingException, IOException {
		final Options options = OptionConstants.createOptions(OptionConstants.STARTVERSION, OptionConstants.ENDVERSION, OptionConstants.DEPENDENCYFILE, OptionConstants.EXECUTIONFILE);

		final CommandLineParser parser = new DefaultParser();
		final CommandLine line = parser.parse(options, args);

		// final File dependenciesFile = new File(args[0]);
		final File dependenciesFile = new File(line.getOptionValue(OptionConstants.DEPENDENCYFILE.getName()));
		if (!dependenciesFile.exists()) {
			LOG.info("Dependencies-file " + dependenciesFile.getAbsolutePath() + " should exist.");
			System.exit(1);
		}

		ExecutionData changedTests;
		if (line.hasOption(OptionConstants.EXECUTIONFILE.getName())) {
			final File executeFile = new File(line.getOptionValue(OptionConstants.EXECUTIONFILE.getName()));
			changedTests = Constants.OBJECTMAPPER.readValue(executeFile, ExecutionData.class);
		} else {
			changedTests = null;
		}

		final DependencyStatistics statistics = getChangeStatistics(dependenciesFile, changedTests);

		LOG.info("Versions: {} Bei Pruning ausgeführte Tests: {} Trace-Changed Tests: {}", statistics.size, statistics.pruningRunTests, statistics.changedTraceTests);
		LOG.info("Gesamt-Tests: {} Bei Pruning (ggf. mehrmals) genutzte Tests: {} Nur einmal ausgeführte Tests (d.h. keine Veränderung möglich): {}", statistics.overallRunTests,
				statistics.multipleChangedTest.size(), statistics.onceChangedTests.size());
	}

	public static DependencyStatistics getChangeStatistics(final File dependenciesFile, final ExecutionData changedTests) throws JAXBException {
		final Dependencies dependencies = readVersions(dependenciesFile);
		final Map<String,Version> versions = dependencies.getVersions();

		final int startTestCound = dependencies.getInitialversion().getInitialDependencies().size();
		final List<TestCase> currentContainedTests = new LinkedList<>();
		for (final ChangedEntity dependency : dependencies.getInitialversion().getInitialDependencies().keySet()) {
			currentContainedTests.add(new TestCase(dependency.getClazz(), dependency.getMethod(), dependency.getModule()));
		}

		LOG.trace("StartTest: {}", startTestCound);
//		final List<TestCase> sometimesChangedTest = new LinkedList<>(); // Nicht nur Vorkommen, auch Anzahl relevant
		final DependencyStatistics statistics = new DependencyStatistics();
//		final List<TestCase> onlyOnceChangedTests = new LinkedList<>();
		statistics.onceChangedTests.addAll(currentContainedTests);
		
		statistics.size = versions.size();
//		final int changedTraceTests = 0;
//		final int pruningRunTests = 0;
		for (final Entry<String, Version> version : versions.entrySet()) {
			final Set<TestCase> currentIterationTests = new HashSet<>();
			for (final Map.Entry<ChangedEntity, TestSet> dependency : version.getValue().getChangedClazzes().entrySet()) {
				for (final Entry<ChangedEntity, Set<String>> testcase : dependency.getValue().getTestcases().entrySet()) {
					final String testclass = testcase.getKey().getClazz();
					for (final String method : testcase.getValue()) {
						final TestCase testcase2 = new TestCase(testclass, method);
						// final String testname = testclass + "." + method;
						if (currentContainedTests.contains(testcase2)) {
							currentIterationTests.add(testcase2);
						} else {
							currentContainedTests.add(testcase2);
							statistics.onceChangedTests.add(testcase2);
							// LOG.info("Neuer Test: " + testname);
						}
					}
				}
			}
			int currentTraceChangedTests = 0;
			if (changedTests != null) {
				for (final TestCase currentIterationTest : currentContainedTests) {
					if (changedTests.versionContainsTest(version.getKey(), currentIterationTest)) {
						currentTraceChangedTests++;
					}
				}
			}

			LOG.trace("Version: {} Tests: {} Trace-Changed: {}", version.getKey(), currentIterationTests.size(), currentTraceChangedTests);
			statistics.multipleChangedTest.addAll(currentIterationTests);
			statistics.onceChangedTests.removeAll(currentIterationTests);

			statistics.changedTraceTests += currentTraceChangedTests;

			statistics.pruningRunTests += currentIterationTests.size();

			statistics.overallRunTests += currentContainedTests.size();
		}
		return statistics;
	}

//	public static Map<String, Version> readVersionMap(final File dependenciesFile) throws JAXBException {
//		final Dependencies data = readVersions(dependenciesFile);
//
//		final List<Version> versions = data.getVersions().getVersion();
//
//		final Map<String, Version> versionMap = new LinkedHashMap<>();
//		versions.forEach(version -> versionMap.put(version.getVersion(), version));
//
//		return versionMap;
//	}

	public static Dependencies readVersions(final File dependencyFile) throws JAXBException {
	   Dependencies deps = null;
	   try {
         deps = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
      } catch (final IOException e) {
         e.printStackTrace();
      }
	   return deps;
//		LOG.trace("Reading versions: {}", dependencyFile.getAbsolutePath());
//		final JAXBContext jc = JAXBContext.newInstance(Versiondependencies.class);
//		final Unmarshaller unmarshaller = jc.createUnmarshaller();
//		final Versiondependencies data = (Versiondependencies) unmarshaller.unmarshal(dependencyFile);
//
//		return data;
	}
}
