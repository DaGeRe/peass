package de.peass.statistics;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.Result.Fulldata.Value;

/**
 * Represents the data of a confidence interval of a measurement.
 * @author reichelt
 *
 */
public class ConfidenceInterval {
	private final double min, max;
	private final int percentage;

	public ConfidenceInterval(final double min, final double max, final int percentage) {
		super();
		this.min = min;
		this.max = max;
		this.percentage = percentage;
	}

	/**
	 * @return the min
	 */
	public double getMin() {
		return min;
	}

	/**
	 * @return the max
	 */
	public double getMax() {
		return max;
	}

	public double getLength() {
		return max - min;
	}

	/**
	 * @return the percentage
	 */
	public int getPercentage() {
		return percentage;
	}

	@Override
	public String toString() {
		return percentage + "-Interval: " + min + " - " + max;
	}
	
	public static List<Result> getWarmupData(final List<Result> values) {
      final List<Result> shortenedValues = new LinkedList<>();
      for (final Result result : values) {
         final int start = result.getFulldata().getValue().size() / 2;
         final Result resultShort = shortenResult(result, 0, start);
         shortenedValues.add(resultShort);
      }
      return shortenedValues;
   }
	
	public static List<Result> getWarmedUpData(final List<Result> values) {
      final List<Result> shortenedValues = new LinkedList<>();
      for (final Result result : values) {
         final Result resultShort = shortenValues(result);
         shortenedValues.add(resultShort);
      }
      return shortenedValues;
   }

   public static Result shortenValues(final Result result) {
      final int start = result.getFulldata().getValue().size() / 2;
      final int end = result.getFulldata().getValue().size();
      final Result resultShort = shortenResult(result, start, end);
      return resultShort;
   }
	
	public static List<Result> shortenValues(final List<Result> values, final int start, final int end) {
      final List<Result> shortenedValues = new LinkedList<>();
      for (final Result result : values) {
         final Result resultShort = shortenResult(result, start, end);
         shortenedValues.add(resultShort);
      }
      return shortenedValues;
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
      resultShort.setDeviation(statistics.getStandardDeviation());
      resultShort.setExecutionTimes(end-start);
      resultShort.setWarmupExecutions(start);
      resultShort.setRepetitions(result.getRepetitions());
      return resultShort;
   }
}
