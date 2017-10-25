package de.peran.measurement.analysis.knowledge;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Changes {
	Map<String, List<Change>> testcaseChanges = new LinkedHashMap<>();

	public Map<String, List<Change>> getTestcaseChanges() {
		return testcaseChanges;
	}

	public void setTestcaseChanges(Map<String, List<Change>> testcaseChanges) {
		this.testcaseChanges = testcaseChanges;
	}

	public Change addChange(String testcase, String viewName, String method, double percent) {
		Change change = new Change();
		change.setDiff(viewName);
		change.setChangePercent(percent);
		change.setClazz(method);
		List<Change> currentChanges = testcaseChanges.get(testcase);
		if (currentChanges == null){
			currentChanges = new LinkedList<>();
			testcaseChanges.put(testcase, currentChanges);
		}
		currentChanges.add(change);
		return change;
	}
}