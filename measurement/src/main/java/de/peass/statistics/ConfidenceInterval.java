package de.peass.statistics;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.Result.Fulldata.Value;
import de.peass.measurement.analysis.StatisticUtil;

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
         final Result resultShort = StatisticUtil.shortenResult(result, 0, start);
         shortenedValues.add(resultShort);
      }
      return shortenedValues;
   }
	
	public static List<Result> getWarmedUpData(final List<Result> values) {
      final List<Result> shortenedValues = new LinkedList<>();
      for (final Result result : values) {
         final Result resultShort = StatisticUtil.shortenResult(result);
         shortenedValues.add(resultShort);
      }
      return shortenedValues;
   }
}
