package de.peran.evaluation.base;

/**
 * Result of one evaluation process with the methods that have been run in both and with the methods that would have been run in the evaluated project
 * 
 * @author reichelt
 *
 */
public class EvaluationResult {
	private int equalMethods;
	private int overallMethods;

	public int getEqualMethods() {
		return equalMethods;
	}

	public void setEqualMethods(final int equalMethods) {
		this.equalMethods = equalMethods;
	}

	public int getOverallMethods() {
		return overallMethods;
	}

	public void setOverallMethods(final int overallMethods) {
		this.overallMethods = overallMethods;
	}
}