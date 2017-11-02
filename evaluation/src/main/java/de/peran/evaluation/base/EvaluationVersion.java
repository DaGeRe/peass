package de.peran.evaluation.base;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Saves how many testcases for each test class are executed.
 * 
 * @author reichelt
 *
 */
public class EvaluationVersion {
	private Map<String, Integer> testcaseExecutions = new LinkedHashMap<>();

	public Map<String, Integer> getTestcaseExecutions() {
		return testcaseExecutions;
	}

	public void setTestcaseExecutions(final Map<String, Integer> testcaseExecutions) {
		this.testcaseExecutions = testcaseExecutions;
	}

	public int getTestCount() {
		int tests = 0;
		for (final Integer testCount : testcaseExecutions.values()) {
			tests += testCount;
		}
		return tests;
	}

}