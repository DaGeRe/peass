package de.peran.analysis.helper;

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
