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
 * Represents a performance change, i.e. changed measured values between two versions in one testcase.
 * 
 * @author reichelt
 *
 */
public class PerformanceChange {
	private final ConfidenceInterval intervalOld, intervalNew;
	private final String testClass, testMethod;
	private final String revision;
	private final String revisionOld;

	public PerformanceChange(final ConfidenceInterval intervalOld, final ConfidenceInterval intervalNew, final String testClass, final String testMethod, final String revisionOld,
			final String revision) {
		super();
		this.intervalOld = intervalOld;
		this.intervalNew = intervalNew;
		this.testClass = testClass;
		this.testMethod = testMethod;
		this.revision = revision;
		this.revisionOld = revisionOld;
	}

	/**
	 * @return the intervalOld
	 */
	public ConfidenceInterval getIntervalOld() {
		return intervalOld;
	}

	/**
	 * @return the intervalNew
	 */
	public ConfidenceInterval getIntervalNew() {
		return intervalNew;
	}

	/**
	 * @return the test
	 */
	public String getTestClass() {
		return testClass;
	}

	public String getTestMethod() {
		return testMethod;
	}

	/**
	 * @return the revisions
	 */
	public String getRevision() {
		return revision;
	}

	/**
	 * @return the revisions
	 */
	public String getRevisionOld() {
		return revisionOld;
	}

	/**
	 * Returns the difference between the ends of the confidence intervals divided by their average length.
	 * 
	 * @return
	 */
	public double getNormedDifference() {
		final double intervalLength = (intervalNew.getLength() + intervalOld.getLength()) / 2;
		return getDifference() / intervalLength;
	}

	/**
	 * Returns the difference between the ends of both confidence intervals.
	 * 
	 * @return
	 */
	public double getDifference() {
		if (intervalNew.getMax() < intervalOld.getMin()) {
			return (intervalOld.getMin() - intervalNew.getMax());
		} else if (intervalNew.getMin() > intervalOld.getMax()) {
			return (intervalNew.getMin() - intervalOld.getMax());
		} else {
			return 0.0;
		}
	}

	@Override
	public String toString() {
		return testClass + "." + testMethod + " (" + revision + ":" + revisionOld + ")" + intervalNew + " Vorher: " + intervalOld;
	}

}
