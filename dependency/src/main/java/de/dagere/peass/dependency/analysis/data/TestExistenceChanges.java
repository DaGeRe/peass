package de.dagere.peass.dependency.analysis.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Manages data about test existence changes, i.e. tests that are added or removed
 * 
 * @author reichelt
 *
 */
public class TestExistenceChanges {
	
	//Map from dependency (fqn) -> testcase
	private final Map<ChangedEntity, Set<TestCase>> addedTests = new TreeMap<>();
	private final Set<TestCase> removedTests = new HashSet<>();

	public Map<ChangedEntity, Set<TestCase>> getAddedTests() {
		return addedTests;
	}

	public Set<TestCase> getRemovedTests() {
		return removedTests;
	}

	public void addRemovedTest(final TestCase testcase) {
		removedTests.add(testcase);
	}

	public void addAddedTest(final ChangedEntity changedEntity, final TestCase testcase) {
		Set<TestCase> testcaseSet = addedTests.get(changedEntity);
		if (testcaseSet == null) {
			testcaseSet = new TreeSet<>();
			addedTests.put(changedEntity, testcaseSet);
		}
		testcaseSet.add(testcase);
		
	}
}
