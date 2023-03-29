package de.dagere.peass.dependency.analysis.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.dagere.nodeDiffGenerator.data.MethodCall;
import de.dagere.nodeDiffGenerator.data.TestCase;
import de.dagere.nodeDiffGenerator.data.TestMethodCall;

/**
 * Manages data about test existence changes, i.e. tests that are added or removed
 * 
 * @author reichelt
 *
 */
public class TestExistenceChanges {
	
	//Map from dependency (fqn) -> testcase
	private final Map<MethodCall, Set<TestMethodCall>> addedTests = new TreeMap<>();
	private final Set<TestCase> removedTests = new HashSet<>();

	public Map<MethodCall, Set<TestMethodCall>> getAddedTests() {
		return addedTests;
	}

	public Set<TestCase> getRemovedTests() {
		return removedTests;
	}

	public void addRemovedTest(final TestCase testcase) {
		removedTests.add(testcase);
	}

	public void addAddedTest(final MethodCall changedEntity, final TestMethodCall testcase) {
		Set<TestMethodCall> testcaseSet = addedTests.get(changedEntity);
		if (testcaseSet == null) {
			testcaseSet = new TreeSet<>();
			addedTests.put(changedEntity, testcaseSet);
		}
		testcaseSet.add(testcase);
		
	}
}
