package de.peass.config;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration properties of Peass-executions that are used in every circumstance, i.e. for regression test selection, measurement and root cause analysis
 * 
 * @author DaGeRe
 *
 */
public class ExecutionConfig implements Serializable {

   private static final long serialVersionUID = -6642358125854337047L;
   
   /**
    * Timeout in milliseconds, default 5 minutes
    */
   private final int timeout;
   private String testGoal;
   private List<String> includes;
   
   private String version = "HEAD";
   private String versionOld = "HEAD~1";
   protected String startversion;
   protected String endversion;
   

   public ExecutionConfig() {
      includes = new LinkedList<String>();
      testGoal = null;
      this.timeout = 5 * 60 * 1000;
   }
   
   public ExecutionConfig(final int timeoutInMinutes) {
      if (timeoutInMinutes <= 0) {
         throw new RuntimeException("Illegal timeout: " + timeoutInMinutes);
      }
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

   @JsonInclude(Include.NON_NULL)
   public String getTestGoal() {
      return testGoal;
   }

   public void setTestGoal(final String testGoal) {
      this.testGoal = testGoal;
   }

   @JsonInclude(Include.NON_NULL)
   public List<String> getIncludes() {
      return includes;
   }

   public void setIncludes(final List<String> includes) {
      this.includes = includes;
   }

   @JsonIgnore
   public int getTimeoutInMinutes() {
      return timeout / 60 / 1000;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(final String version) {
      this.version = version;
   }

   public String getVersionOld() {
      return versionOld;
   }

   public void setVersionOld(final String versionOld) {
      this.versionOld = versionOld;
   }

   @JsonInclude(JsonInclude.Include.NON_NULL)
   public String getStartversion() {
      return startversion;
   }

   public void setStartversion(final String startversion) {
      this.startversion = startversion;
   }

   @JsonInclude(JsonInclude.Include.NON_NULL)
   public String getEndversion() {
      return endversion;
   }

   public void setEndversion(final String endversion) {
      this.endversion = endversion;
   }
}
