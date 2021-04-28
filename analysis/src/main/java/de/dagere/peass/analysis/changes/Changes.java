package de.dagere.peass.analysis.changes;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.dependency.analysis.data.TestCase;

/**
 * Saves all changes for one version. For each testcase it is saved which change has happened with method, difference in percent etc.
 * 
 * @author reichelt
 *
 */
public class Changes implements Serializable {
   
   private static final long serialVersionUID = -7339774896217980704L;

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
    * @param viewName view-file where trace-diff should be saved
    * @param method Testmethod where performance changed
    * @param percent How much the performance was changed
    * @return Added Change
    */
   public Change addChange(final TestCase testcase, final String viewName, final double oldTime, final double percent, final double tvalue, final long vms) {
      Change change = new Change();
      change.setDiff(viewName);
      change.setTvalue(tvalue);
      change.setOldTime(oldTime);
      change.setChangePercent(percent);
      change.setVms(vms);
      change.setMethod(testcase.getMethod());
      String clazz = testcase.getTestclazzWithModuleName();
      addChange(clazz, change);
      return change;
   }

   public Change getChange(final TestCase test) {
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

   public void addChange(final String testclazz, final Change change) {
      if (change == null) {
         throw new RuntimeException("Change should not be null! Testclass: " + testclazz);
      }
      List<Change> currentChanges = testcaseChanges.get(testclazz);
      if (currentChanges == null) {
         currentChanges = new LinkedList<>();
         testcaseChanges.put(testclazz, currentChanges);
      }
      currentChanges.add(change);

      currentChanges.sort(new Comparator<Change>() {
         @Override
         public int compare(final Change o1, final Change o2) {
            return o1.getDiff().compareTo(o2.getDiff());
         }
      });
   }
}