package de.peran.measurement.analysis.statistics;

/*-
 * #%L
 * peran-analysis
 * %%
 * Copyright (C) 2015 - 2017 DaGeRe
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


import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result;
import de.peran.statistics.ConfidenceInterval;
import de.peran.statistics.PerformanceChange;

public class ConfidenceIntervalInterpretion {
	
	private static final Logger LOG = LogManager.getLogger(ConfidenceIntervalInterpretion.class);
	
	public static double getMean(final List<Result> results) {
		final DescriptiveStatistics ds = new DescriptiveStatistics();
		results.forEach(result -> ds.addValue(result.getValue()));
		return ds.getMean();
	}

	/**
	 * Returns the relation of the two empirical results, based on confidence interval interpretation, viewed from the _first_ result. E.g. LESS_THAN means
	 * that the first result is LESS_THAN the second, and GREATER_THAN means the first result is GREATER_THAN the second.
	 * @param before
	 * @param after
	 * @return
	 */
	public static Relation compare(final List<Result> before, final List<Result> after) {
		final ConfidenceInterval intervalBefore = getConfidenceInterval(before);
		final ConfidenceInterval intervalAfter = getConfidenceInterval(after);
		
		final double avgBefore = before.stream().mapToDouble(b -> b.getValue()).average().getAsDouble();
		final double avgAfter = after.stream().mapToDouble(b -> b.getValue()).average().getAsDouble();
		
		LOG.trace("Intervalle: {} ({}) vs. vorher {} ({})", intervalAfter, avgAfter, intervalBefore, avgBefore);
		final PerformanceChange change = new PerformanceChange(intervalBefore, intervalAfter, "", "", "0", "1");
		final double diff = change.getDifference();
		if (intervalBefore.getMax() < intervalAfter.getMin()) {
			if (change.getNormedDifference() > MeasurementAnalysationUtil.MIN_NORMED_DISTANCE && diff > MeasurementAnalysationUtil.MIN_ABSOLUTE_PERCENTAGE_DISTANCE * intervalAfter.getMax()) {
				LOG.trace("Änderung: {} {} Diff: {}", change.getRevisionOld(), change.getTestMethod(), diff);
				LOG.trace("Ist kleiner geworden: {} vs. vorher {}", intervalAfter, intervalBefore);
				LOG.trace("Abstand: {} Versionen: {}:{}", diff);
				return Relation.LESS_THAN;
			}
		}
		if (intervalBefore.getMin() > intervalAfter.getMax()) {
			if (change.getNormedDifference() > MeasurementAnalysationUtil.MIN_NORMED_DISTANCE && diff > MeasurementAnalysationUtil.MIN_ABSOLUTE_PERCENTAGE_DISTANCE * intervalAfter.getMax()) {
				LOG.trace("Änderung: {} {} Diff: {}", change.getRevisionOld(), change.getTestMethod(), diff);
				LOG.trace("Ist größer geworden: {} vs. vorher {}", intervalAfter, intervalBefore);
				LOG.trace("Abstand: {}", diff);
				return Relation.GREATER_THAN;
			}
		}
		
		return Relation.EQUAL;
	}

	public static ConfidenceInterval getConfidenceInterval(final List<Result> before) {
//		System.out.println(before);
		final double[] valuesBefore = MeasurementAnalysationUtil.getAveragesArrayFromResults(before);
//		LOG.info(valuesBefore + " " + valuesBefore.length);
		final ConfidenceInterval intervalBefore = MeasurementAnalysationUtil.getBootstrapConfidenceInterval(valuesBefore, 20, 1000, 96);
		return intervalBefore;
	}
}
