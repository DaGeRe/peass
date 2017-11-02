package de.peran.dependency.analysis.data;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Map from changed classes fqn to testcases that might have changed
 * @author reichelt
 *
 */
public class ChangeTestMapping {
	private final Map<String, Set<String>> changes = new TreeMap<>();
//	private final List<String> addedTestClasses = new LinkedList<>();

	public Map<String, Set<String>> getChanges() {
		return changes;
	}
	
//	public List<String> getAddedTestClasses() {
//		return addedTestClasses;
//	}
//
//	public void addAddedTest(String changedClass) {
//		addedTestClasses.add(changedClass);
//	}
}
