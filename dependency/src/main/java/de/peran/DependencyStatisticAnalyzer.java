package de.peran;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.peran.dependency.analysis.data.TestCase;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Initialversion.Initialdependency;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency.Testcase;
import de.peran.reduceddependency.ChangedTraceTests;
import de.peran.utils.OptionConstants;

/**
 * Analyzes a depedency file and prints some statistical information about it.
 * 
 * @author reichelt
 *
 */
public class DependencyStatisticAnalyzer {

	static class Statistics {
		int overallRunTests = 0;
		int changedTraceTests = 0;
		int pruningRunTests = 0;
		
		int size = 0;
		
		List<TestCase> multipleChangedTest = new LinkedList<>();
		List<TestCase> onceChangedTests = new LinkedList<>();
	}
 
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

		ChangedTraceTests changedTests;
		if (line.hasOption(OptionConstants.EXECUTIONFILE.getName())) {
			final File executeFile = new File(line.getOptionValue(OptionConstants.EXECUTIONFILE.getName()));
			final ObjectMapper mapper = new ObjectMapper();
			changedTests = mapper.readValue(executeFile, ChangedTraceTests.class);
		} else {
			changedTests = null;
		}

		final Statistics statistics = getChangeStatistics(dependenciesFile, changedTests);

		LOG.info("Versions: {} Bei Pruning ausgeführte Tests: {} Trace-Changed Tests: {}", statistics.size, statistics.pruningRunTests, statistics.changedTraceTests);
		LOG.info("Gesamt-Tests: {} Bei Pruning (ggf. mehrmals) genutzte Tests: {} Nur einmal ausgeführte Tests (d.h. keine Veränderung möglich): {}", statistics.overallRunTests,
				statistics.multipleChangedTest.size(), statistics.onceChangedTests.size());
	}

	public static Statistics getChangeStatistics(final File dependenciesFile, final ChangedTraceTests changedTests) throws JAXBException {
		final Versiondependencies dependencies = readVersions(dependenciesFile);
		final List<Version> versions = dependencies.getVersions().getVersion();

		final int startTestCound = dependencies.getInitialversion().getInitialdependency().size();
		final List<TestCase> currentContainedTests = new LinkedList<>();
		for (final Initialdependency dependency : dependencies.getInitialversion().getInitialdependency()) {
			currentContainedTests.add(new TestCase(dependency.getTestclass()));
		}

		LOG.trace("StartTest: {}", startTestCound);
//		final List<TestCase> sometimesChangedTest = new LinkedList<>(); // Nicht nur Vorkommen, auch Anzahl relevant
		final Statistics statistics = new Statistics();
//		final List<TestCase> onlyOnceChangedTests = new LinkedList<>();
		statistics.onceChangedTests.addAll(currentContainedTests);
		
		statistics.size = versions.size();
//		final int changedTraceTests = 0;
//		final int pruningRunTests = 0;
		for (final Version version : versions) {
			final Set<TestCase> currentIterationTests = new HashSet<>();
			for (final Dependency dependency : version.getDependency()) {
				for (final Testcase testcase : dependency.getTestcase()) {
					final String testclass = testcase.getClazz();
					for (final String method : testcase.getMethod()) {
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
					if (changedTests.versionContainsTest(version.getVersion(), currentIterationTest)) {
						currentTraceChangedTests++;
					}
				}
			}

			LOG.trace("Version: {} Tests: {} Trace-Changed: {}", version.getVersion(), currentIterationTests.size(), currentTraceChangedTests);
			statistics.multipleChangedTest.addAll(currentIterationTests);
			statistics.onceChangedTests.removeAll(currentIterationTests);

			statistics.changedTraceTests += currentTraceChangedTests;

			statistics.pruningRunTests += currentIterationTests.size();

			statistics.overallRunTests += currentContainedTests.size();
		}
		return statistics;
	}

	public static Map<String, Version> readVersionMap(final File dependenciesFile) throws JAXBException {
		final Versiondependencies data = readVersions(dependenciesFile);

		final List<Version> versions = data.getVersions().getVersion();

		final Map<String, Version> versionMap = new LinkedHashMap<>();
		versions.forEach(version -> versionMap.put(version.getVersion(), version));

		return versionMap;
	}

	public static Versiondependencies readVersions(final File dependencyFile) throws JAXBException {
		LOG.trace("Reading versions: {}", dependencyFile.getAbsolutePath());
		final JAXBContext jc = JAXBContext.newInstance(Versiondependencies.class);
		final Unmarshaller unmarshaller = jc.createUnmarshaller();
		final Versiondependencies data = (Versiondependencies) unmarshaller.unmarshal(dependencyFile);

		return data;
	}
}
