package de.dagere.peass.statistics;

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
