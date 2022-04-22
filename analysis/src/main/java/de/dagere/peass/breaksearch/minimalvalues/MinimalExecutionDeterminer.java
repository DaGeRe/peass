package de.dagere.peass.breaksearch.minimalvalues;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.breaksearch.FindLowestPossibleIterations;
import de.dagere.peass.measurement.statistics.StatisticUtil;

/**
 * Determines the minimal count of executions keeping the same result.
 * @author reichelt
 *
 */
public class MinimalExecutionDeterminer extends MinimalValueDeterminer {
	private static final Logger LOG = LogManager.getLogger(MinimalExecutionDeterminer.class);

	@Override
	int analyzeMeasurement(final int oldResult, final List<VMResult> current, final List<VMResult> before) {
		int localMinValue = current.get(0).getFulldata().getValues().size();

		executionloop: for (; localMinValue > 1000; localMinValue -= 500) {
			final boolean significant;
			final List<VMResult> reduced = StatisticUtil.shortenValues(current, 0, localMinValue);
			final List<VMResult> reducedBefore = StatisticUtil.shortenValues(before, 0, localMinValue);
			significant = FindLowestPossibleIterations.isStillSignificant(getValues(reduced), getValues(reducedBefore), oldResult);
			// final boolean significant = isStillSignificant(statistics.subList(start, start + localMinVmTry), statisticsBefore.subList(start, start + localMinVmTry), oldResult);
			if (!significant) {
				LOG.info("Break at " + localMinValue);
				break executionloop;
			}
		}
		return localMinValue;
	}

	
	
	public static DescriptiveStatistics getStatistic(final List<VMResult> values) {
	   DescriptiveStatistics statistics = new DescriptiveStatistics();
	   for (VMResult r : values) {
	      statistics.addValue(r.getValue());
	   }
	   return statistics;
	}

	@Override
	int getSize(final List<VMResult> results) {
		return results.size();
	}

	@Override
	int getMin(final List<VMResult> results) {
		return 2;
	}

	@Override
	int getChange(final List<VMResult> results) {
		return 1;
	}

}