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

   /**
    * Timeout in milliseconds, default 5 minutes
    */
   private final int timeout;
   private String testGoal;
   private List<String> includes;

   public ExecutionConfig() {
      includes = new LinkedList<String>();
      testGoal = null;
      this.timeout = 5 * 60 * 1000;
   }
   
   public ExecutionConfig(final int timeoutInMinutes) {
      includes = new LinkedList<String>();
      testGoal = null;
      this.timeout = timeoutInMinutes * 60 * 1000;
   }

   public ExecutionConfig(@JsonProperty("includes") final List<String> includes,
         @JsonProperty("testGoal") final String testGoal) {
      this.includes = includes;
      this.testGoal = testGoal;
      this.timeout = 5 * 60 * 1000;
   }

   public int getTimeout() {
      return timeout;
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
