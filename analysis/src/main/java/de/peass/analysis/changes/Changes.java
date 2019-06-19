package de.peass.analysis.changes;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.peass.dependency.analysis.data.TestCase;

/**
 * Saves all changes for one version. For each testcase  it is saved which change has happened with method, difference in percent etc.
 * 
 * @author reichelt
 *
 */
public class Changes {
	private Map<String, List<Change>> testcaseChanges = new TreeMap<>();

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
	public Change addChange(final TestCase testcase, final String viewName, final double oldTime, final double percent, final double tvalue, long vms) {
		Change change = new Change();
		change.setDiff(viewName);
		change.setTvalue(tvalue);
		change.setOldTime(oldTime);
		change.setChangePercent(percent);
		change.setVms(vms);
		change.setMethod(testcase.getMethod());
		addChange(testcase.getClazz(), change);
		return change;
	}
	
	public Change getChange(TestCase test) {
	   List<Change> changes = testcaseChanges.get(test.getClazz());
	   if (changes != null) {
	      for (Change candidate : changes) {
	         String candidateMethod = candidate.getMethod();
	         String testMethod = test.getMethod();
	         if (candidateMethod.equals(testMethod)) {
	            return candidate;
	         }
	      }
	   }
	   return null;
	}

   public void addChange(String testclazz, Change change) {
      if (change == null) {
         throw new RuntimeException("Change should not be null! Testclass: " + testclazz);
      }
      List<Change> currentChanges = testcaseChanges.get(testclazz);
      if (currentChanges == null) {
         currentChanges = new LinkedList<>();
         testcaseChanges.put(testclazz, currentChanges);
      }
      currentChanges.add(change);
   }
}