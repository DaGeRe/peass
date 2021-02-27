package de.peass.config;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration properties of Peass-executions that are used in every circumstance, i.e. for regression test selection, measurement and root cause analysis
 * 
 * @author DaGeRe
 *
 */
public class ExecutionConfig {
   private String testGoal;
   private List<String> includes;

   public ExecutionConfig() {
      includes = new LinkedList<String>();
      testGoal = null;
   }
   
   public ExecutionConfig(@JsonProperty("includes") final List<String> includes,
         @JsonProperty("testGoal") final String testGoal) {
      this.includes = includes;
      this.testGoal = testGoal;
   }

   public String getTestGoal() {
      return testGoal;
   }

   public void setTestGoal(final String testGoal) {
      this.testGoal = testGoal;
   }

   public List<String> getIncludes() {
      return includes;
   }

   public void setIncludes(final List<String> includes) {
      this.includes = includes;
   }
}
