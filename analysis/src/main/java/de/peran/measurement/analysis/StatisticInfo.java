package de.peran.measurement.analysis;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependencyprocessors.VersionComparator;

public class StatisticInfo {

	private static final Logger LOG = LogManager.getLogger(StatisticInfo.class);

	public Map<String, Map<TestCase, Statistic>> statistics = VersionComparator.hasDependencies() ? new TreeMap<>(VersionComparator.INSTANCE) : new LinkedHashMap<>();

	public Map<String, Map<TestCase, Statistic>> getStatistics() {
		return statistics;
	}

	public void setStatistics(final Map<String, Map<TestCase, Statistic>> statistics) {
		this.statistics = statistics;
	}

	public void addMeasurement(final String version, final TestCase test, final double meanOld, final double meanCurrent, final double deviationOld, final double deviationCurrent, final int executions,
			final double tvalue) {
		final Statistic statistic = new Statistic(meanOld, meanCurrent, deviationOld, deviationCurrent, executions, tvalue);
		addMeasurement(version, test, statistic);
	}

	public void addMeasurement(final String version, final TestCase test, final Statistic statistic) {
		Map<TestCase, Statistic> versionMap = statistics.get(version);
		if (versionMap == null) {
			versionMap = new HashMap<>();
			statistics.put(version, versionMap);
		}
		if (versionMap.containsKey(test)) {
			LOG.error("Test " + test + " already executed - two measurements!");
		}
		versionMap.put(test, statistic);

	}
}
