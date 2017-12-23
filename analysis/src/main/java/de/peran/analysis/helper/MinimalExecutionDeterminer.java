package de.peran.analysis.helper;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result.Fulldata;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result.Fulldata.Value;
import de.peran.FindLowestPossibleIterations;

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
			final List<Result> reduced = shortenValues(current, 0, localMinValue);
			final List<Result> reducedBefore = shortenValues(before, 0, localMinValue);
			significant = FindLowestPossibleIterations.isStillSignificant(getValues(reduced), getValues(reducedBefore), oldResult);
			// final boolean significant = isStillSignificant(statistics.subList(start, start + localMinVmTry), statisticsBefore.subList(start, start + localMinVmTry), oldResult);
			if (!significant) {
				LOG.info("Break at " + localMinValue);
				break executionloop;
			}
		}
		return localMinValue;
	}

	public static Result shortenResult(final Result result, final int start, final int end) {
		final Result resultShort = new Result();
		resultShort.setFulldata(new Fulldata());
		final DescriptiveStatistics statistics = new DescriptiveStatistics();
		// LOG.debug("Size: " + result.getFulldata().getValue().size());
		final int size = (Math.min(end, result.getFulldata().getValue().size()));
		if (start > size) {
			throw new RuntimeException("Start (" + start + ") is after end of data (" + size + ").");
		}
		if (end > size) {
			throw new RuntimeException("End (" + end + ") is after end of data (" + size + ").");
		}
		// LOG.debug("Size: {}", j);
		for (int i = start; i < size; i++) {
			final Value value = result.getFulldata().getValue().get(i);
			final Fulldata fulldata = resultShort.getFulldata();
			fulldata.getValue().add(value);
			statistics.addValue(Double.parseDouble(value.getValue()));
		}
		resultShort.setValue(statistics.getMean());
		return resultShort;
	}
	
	public static List<Result> cutValuesMiddle(final List<Result> values) {
		final List<Result> shortenedValues = new LinkedList<>();
		for (final Result result : values) {
			final int start = result.getFulldata().getValue().size() / 2;
			final int end = result.getFulldata().getValue().size();
			final Result resultShort = shortenResult(result, start, end);
			shortenedValues.add(resultShort);
		}
		return shortenedValues;
	}

	public static List<Result> shortenValues(final List<Result> values, final int start, final int end) {
		final List<Result> shortenedValues = new LinkedList<>();
		for (final Result result : values) {
			final Result resultShort = shortenResult(result, start, end);
			shortenedValues.add(resultShort);
		}
		return shortenedValues;
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