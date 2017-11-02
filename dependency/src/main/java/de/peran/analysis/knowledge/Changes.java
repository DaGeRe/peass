package de.peran.analysis.knowledge;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Saves all changes for one version. For each testcase  it is saved which change has happened with method, difference in percent etc.
 * 
 * @author reichelt
 *
 */
public class Changes {
	private Map<String, List<Change>> testcaseChanges = new LinkedHashMap<>();

	public Map<String, List<Change>> getTestcaseChanges() {
		return testcaseChanges;
	}

	public void setTestcaseChanges(final Map<String, List<Change>> testcaseChanges) {
		this.testcaseChanges = testcaseChanges;
	}

	/**
	 * Adds a change
	 * 
	 * @param testcase Testcase that has changes
	 * @param viewName	view-file where trace-diff should be saved
	 * @param method	Testmethod where performance changed
	 * @param percent	How much the performance was changed
	 * @return	Added Change
	 */
	public Change addChange(final String testcase, final String viewName, final String method, final double percent) {
		Change change = new Change();
		change.setDiff(viewName);
		change.setChangePercent(percent);
		change.setClazz(method);
		List<Change> currentChanges = testcaseChanges.get(testcase);
		if (currentChanges == null) {
			currentChanges = new LinkedList<>();
			testcaseChanges.put(testcase, currentChanges);
		}
		currentChanges.add(change);
		return change;
	}
}