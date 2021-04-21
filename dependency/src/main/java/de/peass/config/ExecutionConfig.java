package de.peass.config;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.peass.dependency.execution.ExecutionConfigMixin;

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
   private List<String> excludes;

   private String version = "HEAD";
   private String versionOld = "HEAD~1";
   protected String startversion;
   protected String endversion;
   private String pl;
   private boolean createDefaultConstructor = true;

   public ExecutionConfig() {
      includes = new LinkedList<>();
      excludes = new LinkedList<>();
      testGoal = null;
      this.timeout = 5 * 60 * 1000;
   }

   public ExecutionConfig(final int timeoutInMinutes) {
      if (timeoutInMinutes <= 0) {
         throw new RuntimeException("Illegal timeout: " + timeoutInMinutes);
      }
      includes = new LinkedList<>();
      excludes = new LinkedList<>();
      testGoal = null;
      this.timeout = timeoutInMinutes * 60 * 1000;
   }

   public ExecutionConfig(@JsonProperty("includes") final List<String> includes,
         @JsonProperty("testGoal") final String testGoal) {
      this.includes = includes;
      excludes = new LinkedList<>();
      this.testGoal = testGoal;
      this.timeout = 5 * 60 * 1000;
   }
   
   public ExecutionConfig(final ExecutionConfigMixin executionMixin) {
      timeout = executionMixin.getTimeout();
      setVersion(executionMixin.getVersion());
      setVersionOld(executionMixin.getVersionOld());
      setStartversion(executionMixin.getStartversion());
      setEndversion(executionMixin.getEndversion());
      
      setTestGoal(executionMixin.getTestGoal());
      if (executionMixin.getIncludes() != null) {
         for (String include : executionMixin.getIncludes()) {
            includes.add(include);
         }
      }
      if (executionMixin.getExcludes() != null) {
         for (String exclude : executionMixin.getExcludes()) {
            excludes.add(exclude);
         }
         throw new RuntimeException("Not implemented yet");
      }
      if (executionMixin.getPl() != null) {
         pl = executionMixin.getPl();
      }
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
   
   @JsonInclude(Include.NON_NULL)
   public List<String> getExcludes() {
      return excludes;
   }
   
   public void setExcludes(final List<String> excludes) {
      this.excludes = excludes;
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

   public boolean isCreateDefaultConstructor() {
      return createDefaultConstructor;
   }

   public void setCreateDefaultConstructor(final boolean createDefaultConstructor) {
      this.createDefaultConstructor = createDefaultConstructor;
   }

   public String getPl() {
      return pl;
   }

   public void setPl(final String pl) {
      this.pl = pl;
   }
}
