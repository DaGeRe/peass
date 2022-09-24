package de.dagere.peass.dependency.traces.coverage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;

/**
 * Information of a trace how often each method has been called for JSON serialization
 * 
 * @author reichelt
 *
 */
public class TraceCallSummary {

   private int overallScore;
   private boolean selected;
   private TestMethodCall testcase;
   private Map<String, Integer> callCounts = new HashMap<>();
   private Set<String> selectedChanges = new HashSet<>();

   public boolean isSelected() {
      return selected;
   }

   public void setSelected(final boolean selected) {
      this.selected = selected;
   }

   public TestMethodCall getTestcase() {
      return testcase;
   }

   public void setTestcase(final TestMethodCall testcase) {
      this.testcase = testcase;
   }

   public Map<String, Integer> getCallCounts() {
      return callCounts;
   }

   public void setCallCounts(final Map<String, Integer> callCounts) {
      this.callCounts = callCounts;
   }

   public void setSelectedChanges(final Set<String> selectedChanges) {
      this.selectedChanges = selectedChanges;
   }

   public Set<String> getSelectedChanges() {
      return selectedChanges;
   }

   public void setOverallScore(final int overallScore) {
      this.overallScore = overallScore;
   }

   public int getOverallScore() {
      return overallScore;
   }

   @JsonIgnore
   public void addCall(final String method) {
      Integer callCount = callCounts.get(method);
      if (callCount == null) {
         callCounts.put(method, 1);
      } else {
         callCounts.put(method, callCount + 1);
      }
   }
}
