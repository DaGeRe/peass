package de.dagere.peass.config;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.dagere.peass.config.parameters.ExecutionConfigMixin;

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
   private long timeout = 5 * 60 * 1000;
   private String testGoal;
   private List<String> includes = new LinkedList<>();
   private List<String> excludes = new LinkedList<>();

   private String version = "HEAD";
   private String versionOld = "HEAD~1";
   protected String startversion;
   protected String endversion;
   private String pl;
   private boolean createDefaultConstructor = true;
   private boolean redirectSubprocessOutputToFile = true;
   private boolean useTieredCompilation = false;

   private boolean removeSnapshots = false;
   private boolean useAlternativeBuildfile = false;
   private boolean excludeLog4j = false;
   
   private boolean executeBeforeClassInMeasurement = false;
   private boolean onlyMeasureWorkload = false;
   private boolean showStart = false;
   private boolean redirectToNull = true;

   private String testTransformer = "de.dagere.peass.testtransformation.JUnitTestTransformer";
   private String testExecutor = "default";

   public ExecutionConfig() {
      includes = new LinkedList<>();
      excludes = new LinkedList<>();
      testGoal = null;
   }

   public ExecutionConfig(final ExecutionConfig other) {
      this.timeout = other.getTimeout();
      this.testGoal = other.getTestGoal();
      this.includes = other.getIncludes();
      this.version = other.getVersion();
      this.versionOld = other.getVersionOld();
      this.startversion = other.getStartversion();
      this.endversion = other.getEndversion();
      this.createDefaultConstructor = other.isCreateDefaultConstructor();
      this.redirectSubprocessOutputToFile = other.isRedirectSubprocessOutputToFile();
      this.removeSnapshots = other.removeSnapshots;
      this.useAlternativeBuildfile = other.useAlternativeBuildfile;
      this.excludeLog4j = other.excludeLog4j;
      this.testTransformer = other.getTestTransformer();
      this.testExecutor = other.getTestExecutor();
      this.useTieredCompilation = other.isUseTieredCompilation();
      this.pl = other.getPl();
      
      this.executeBeforeClassInMeasurement = other.executeBeforeClassInMeasurement;
      this.onlyMeasureWorkload = other.onlyMeasureWorkload;
      this.showStart = other.showStart;
      this.redirectToNull = other.redirectToNull;
   }

   public ExecutionConfig(final long timeoutInMinutes) {
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
      timeout = executionMixin.getTimeout() * 60 * 1000;
      version = executionMixin.getVersion();
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
      boolean transformerSet = executionMixin.getTestTransformer() != null;
      boolean executorSet = executionMixin.getTestExecutor() != null;
      if (transformerSet && executorSet) {
         setTestTransformer(executionMixin.getTestTransformer());
         setTestExecutor(executionMixin.getTestExecutor());
      } else if (transformerSet != executorSet) {
         throw new RuntimeException("If the test transformer is set by CLI parameters, the test executor needs also be set!");
      } else {
         setTestTransformer(executionMixin.getWorkloadType().getTestTransformer());
         setTestExecutor(executionMixin.getWorkloadType().getTestExecutor());
      }
      useTieredCompilation = executionMixin.isUseTieredCompilation();
      removeSnapshots = executionMixin.isRemoveSnapshots();
      useAlternativeBuildfile = executionMixin.isUseAlternativeBuildfile();
      createDefaultConstructor = !executionMixin.isSkipDefaultConstructor();
      executeBeforeClassInMeasurement = executionMixin.isExecuteBeforeClassInMeasurement();
   }

   public void setTimeout(final long timeout) {
      this.timeout = timeout;
   }

   public long getTimeout() {
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
   public long getTimeoutInSeconds() {
      return timeout / 1000;
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

   public boolean isRedirectSubprocessOutputToFile() {
      return redirectSubprocessOutputToFile;
   }

   public void setRedirectSubprocessOutputToFile(final boolean redirectSubprocessOutputToFile) {
      this.redirectSubprocessOutputToFile = redirectSubprocessOutputToFile;
   }

   public boolean isUseTieredCompilation() {
      return useTieredCompilation;
   }

   public void setUseTieredCompilation(final boolean useTieredCompilation) {
      this.useTieredCompilation = useTieredCompilation;
   }

   public void setRemoveSnapshots(final boolean removeSnapshots) {
      this.removeSnapshots = removeSnapshots;
   }

   public boolean isRemoveSnapshots() {
      return removeSnapshots;
   }
   
   public boolean isUseAlternativeBuildfile() {
      return useAlternativeBuildfile;
   }

   public void setUseAlternativeBuildfile(final boolean useAlternativeBuildfile) {
      this.useAlternativeBuildfile = useAlternativeBuildfile;
   }

   public boolean isExcludeLog4j() {
      return excludeLog4j;
   }

   public void setExcludeLog4j(final boolean excludeLog4j) {
      this.excludeLog4j = excludeLog4j;
   }

   public boolean isExecuteBeforeClassInMeasurement() {
      return executeBeforeClassInMeasurement;
   }

   public void setExecuteBeforeClassInMeasurement(final boolean executeBeforeClassInMeasurement) {
      this.executeBeforeClassInMeasurement = executeBeforeClassInMeasurement;
   }

   public boolean isOnlyMeasureWorkload() {
      return onlyMeasureWorkload;
   }

   public void setOnlyMeasureWorkload(final boolean onlyMeasureWorkload) {
      this.onlyMeasureWorkload = onlyMeasureWorkload;
   }

   public boolean isShowStart() {
      return showStart;
   }

   public void setShowStart(final boolean showStart) {
      this.showStart = showStart;
   }

   public boolean isRedirectToNull() {
      return redirectToNull;
   }

   public void setRedirectToNull(final boolean redirectToNull) {
      this.redirectToNull = redirectToNull;
   }

   public String getTestTransformer() {
      return testTransformer;
   }

   public void setTestTransformer(final String testTransformer) {
      this.testTransformer = testTransformer;
   }

   public String getTestExecutor() {
      return testExecutor;
   }

   public void setTestExecutor(final String testExecutor) {
      this.testExecutor = testExecutor;
   }
}
