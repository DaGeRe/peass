package de.peran.breaksearch.helper;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.generated.Result;
import de.peass.breaksearch.FindLowestPossibleIterations;
import de.peass.measurement.analysis.StatisticUtil;

/**
 * Determines the minimal count of executions keeping the same result.
 * @author reichelt
 *
 */
public class MinimalExecutionDeterminer extends MinimalValueDeterminer {
	private static final Logger LOG = LogManager.getLogger(MinimalExecutionDeterminer.class);

	@Override
	int analyzeMeasurement(final int oldResult, final List<Result> current, final List<Result> before) {
		int localMinValue = current.get(0).getFulldata().getValue().size();

		executionloop: for (; localMinValue > 1000; localMinValue -= 500) {
			final boolean significant;
			final List<Result> reduced = StatisticUtil.shortenValues(current, 0, localMinValue);
			final List<Result> reducedBefore = StatisticUtil.shortenValues(before, 0, localMinValue);
			significant = FindLowestPossibleIterations.isStillSignificant(getValues(reduced), getValues(reducedBefore), oldResult);
			// final boolean significant = isStillSignificant(statistics.subList(start, start + localMinVmTry), statisticsBefore.subList(start, start + localMinVmTry), oldResult);
			if (!significant) {
				LOG.info("Break at " + localMinValue);
				break executionloop;
			}
		}
		return localMinValue;
	}

	
	
	public static DescriptiveStatistics getStatistic(List<Result> values) {
	   DescriptiveStatistics statistics = new DescriptiveStatistics();
	   for (Result r : values) {
	      statistics.addValue(r.getValue());
	   }
	   return statistics;
	}

	@Override
	int getSize(final List<Result> results) {
		return results.size();
	}

	@Override
	int getMin(final List<Result> results) {
		return 2;
	}

	@Override
	int getChange(final List<Result> results) {
		return 1;
	}

}