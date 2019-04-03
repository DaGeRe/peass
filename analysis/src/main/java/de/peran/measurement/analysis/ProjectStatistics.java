package de.peran.measurement.analysis;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependencyprocessors.VersionComparator;

public class ProjectStatistics {

	private static final Logger LOG = LogManager.getLogger(ProjectStatistics.class);

	public Map<String, Map<TestCase, TestcaseStatistic>> statistics = VersionComparator.hasDependencies() ? new TreeMap<>(VersionComparator.INSTANCE) : new LinkedHashMap<>();

	public Map<String, Map<TestCase, TestcaseStatistic>> getStatistics() {
		return statistics;
	}

	public void setStatistics(final Map<String, Map<TestCase, TestcaseStatistic>> statistics) {
		this.statistics = statistics;
	}

	public void addMeasurement(final String version, final TestCase test, final double meanOld, final double meanCurrent, final double deviationOld, final double deviationCurrent, final int executions,
			final double tvalue) {
		final TestcaseStatistic statistic = new TestcaseStatistic(meanOld, meanCurrent, deviationOld, deviationCurrent, executions, tvalue);
		addMeasurement(version, test, statistic);
	}

	public void addMeasurement(final String version, final TestCase test, final TestcaseStatistic statistic) {
		Map<TestCase, TestcaseStatistic> versionMap = statistics.get(version);
		if (versionMap == null) {
			versionMap = new HashMap<>();
			statistics.put(version, versionMap);
		}
		if (versionMap.containsKey(test)) {
			LOG.error("Test " + test + " already executed in " + version + " - two measurements!");
		}
		versionMap.put(test, statistic);

	}
}
