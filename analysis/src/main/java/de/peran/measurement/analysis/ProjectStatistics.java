package de.peran.measurement.analysis;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.measurement.analysis.statistics.TestcaseStatistic;

public class ProjectStatistics {

	private static final Logger LOG = LogManager.getLogger(ProjectStatistics.class);

	public Map<String, Map<TestCase, TestcaseStatistic>> statistics = VersionComparator.hasDependencies() ? new TreeMap<>(VersionComparator.INSTANCE) : new LinkedHashMap<>();

	public Map<String, Map<TestCase, TestcaseStatistic>> getStatistics() {
		return statistics;
	}

	public void setStatistics(final Map<String, Map<TestCase, TestcaseStatistic>> statistics) {
		this.statistics = statistics;
	}

	public void addMeasurement(final String version, final TestCase test, final double meanOld, final double meanCurrent, final double deviationOld, final double deviationCurrent, final int vms,
			final double tvalue) {
	   //TODO t-value based on count of values, not only >3.2 (>3.2 means 99% significance with more than 10 values)
		final TestcaseStatistic statistic = new TestcaseStatistic(meanOld, meanCurrent, deviationOld, deviationCurrent, vms, tvalue, Math.abs(tvalue) > 3.2);
		addMeasurement(version, test, statistic);
	}

	public void addMeasurement(final String version, final TestCase test, final TestcaseStatistic statistic) {
		Map<TestCase, TestcaseStatistic> versionMap = statistics.get(version);
		if (versionMap == null) {
			versionMap = new TreeMap<>();
			statistics.put(version, versionMap);
		}
		if (versionMap.containsKey(test)) {
			LOG.error("Test " + test + " already executed in " + version + " - two measurements!");
		}
		versionMap.put(test, statistic);

	}
}
