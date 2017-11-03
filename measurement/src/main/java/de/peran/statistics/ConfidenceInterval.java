package de.peran.statistics;

/*-
 * #%L
 * peran-measurement
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
