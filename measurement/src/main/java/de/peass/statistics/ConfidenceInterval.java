package de.peass.statistics;

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
}
