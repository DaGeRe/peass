package de.peran.dependency.analysis.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Manages data about test existence changes, i.e. tests that are added or removed
 * @author reichelt
 *
 */
public class TestExistenceChanges {
	
	//Map from dependency (fqn) -> testcase
	private final Map<String, Set<String>> addedTests = new TreeMap<>();
	private final Set<TestCase> removedTests = new HashSet<>();

	public Map<String, Set<String>> getAddedTests() {
		return addedTests;
	}

	public Set<TestCase> getRemovedTests() {
		return removedTests;
	}

	public void addRemovedTest(TestCase testcase) {
		removedTests.add(testcase);
	}

	public void addAddedTest(String changedclass, String changedMethod, String testcase) {
		Set<String> testcaseSet = addedTests.get(changedclass+"."+changedMethod);
		if (testcaseSet == null) {
			testcaseSet = new TreeSet<>();
			addedTests.put(changedclass, testcaseSet);
		}
		testcaseSet.add(testcase);
		
	}
}
