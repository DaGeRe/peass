package de.dagere.peass.config;

import java.io.Serializable;
import java.util.ArrayList;
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

   public static final String CLASSPATH_SEPARATOR = ":";

   private static final long serialVersionUID = -6642358125854337047L;

   /**
    * Timeout in milliseconds, default 5 minutes
    */
   private long timeout = 5 * 60 * 1000;
   private String testGoal;
   private List<String> includes = new LinkedList<>();
   private List<String> excludes = new LinkedList<>();
   
   private List<String> forbiddenMethods = new LinkedList<>();

   private String version = "HEAD";
   private String versionOld = "HEAD~1";
   protected String startversion;
   protected String endversion;
   private String pl;
   private boolean createDefaultConstructor = true;
   private int kiekerWaitTime = 10;

   private boolean redirectSubprocessOutputToFile = true;
   private boolean useTieredCompilation = false;

   private boolean removeSnapshots = false;
   private boolean useAlternativeBuildfile = false;
   private boolean excludeLog4jSlf4jImpl = false;
   private boolean excludeLog4jToSlf4j = false;

   private boolean executeBeforeClassInMeasurement = false;
   private boolean onlyMeasureWorkload = false;
   private boolean showStart = false;
   private boolean redirectToNull = true;
   private boolean createDetailDebugFiles = true;

   private String testTransformer = "de.dagere.peass.testtransformation.JUnitTestTransformer";
   private String testExecutor = "default";

   private String properties;

   private List<String> clazzFolders = new LinkedList<>();
   {
      clazzFolders.add("src/main/java");
      clazzFolders.add("src/main");
   }
   private List<String> testClazzFolders = new LinkedList<>();
   {
      testClazzFolders.add("src/test/java");
      testClazzFolders.add("src/test");
      testClazzFolders.add("src/androidTest/java/");
   }

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
      this.kiekerWaitTime = other.kiekerWaitTime;
      this.redirectSubprocessOutputToFile = other.isRedirectSubprocessOutputToFile();
      this.removeSnapshots = other.removeSnapshots;
      this.useAlternativeBuildfile = other.useAlternativeBuildfile;
      this.excludeLog4jSlf4jImpl = other.excludeLog4jSlf4jImpl;
      this.excludeLog4jToSlf4j = other.excludeLog4jToSlf4j;
      this.testTransformer = other.getTestTransformer();
      this.testExecutor = other.getTestExecutor();
      this.useTieredCompilation = other.isUseTieredCompilation();
      this.pl = other.getPl();

      this.executeBeforeClassInMeasurement = other.executeBeforeClassInMeasurement;
      this.onlyMeasureWorkload = other.onlyMeasureWorkload;
      this.showStart = other.showStart;
      this.redirectToNull = other.redirectToNull;

      this.clazzFolders = other.clazzFolders;
      this.testClazzFolders = other.testClazzFolders;
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

   public static List<String> buildFolderList(final String folderList) {
      List<String> clazzFolders = new ArrayList<>();
      String[] classpathElements = folderList.trim().split(CLASSPATH_SEPARATOR);
      for (String clazzFolder : classpathElements) {
         clazzFolders.add(clazzFolder);
      }
      return clazzFolders;
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

   public List<String> getForbiddenMethods() {
      return forbiddenMethods;
   }

   public void setForbiddenMethods(List<String> forbiddenMethods) {
      this.forbiddenMethods = forbiddenMethods;
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

   public int getKiekerWaitTime() {
      return kiekerWaitTime;
   }

   public void setKiekerWaitTime(final int kiekerWaitTime) {
      this.kiekerWaitTime = kiekerWaitTime;
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
      return excludeLog4jSlf4jImpl;
   }

   public void setExcludeLog4j(final boolean excludeLog4j) {
      this.excludeLog4jSlf4jImpl = excludeLog4j;
   }

   public boolean isExcludeLog4jToSlf4j() {
      return excludeLog4jToSlf4j;
   }

   public void setExcludeLog4jToSlf4j(final boolean excludeLog4jToSlf4j) {
      this.excludeLog4jToSlf4j = excludeLog4jToSlf4j;
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

   public List<String> getClazzFolders() {
      return clazzFolders;
   }

   public void setClazzFolders(final List<String> clazzFolders) {
      this.clazzFolders = clazzFolders;
   }

   public List<String> getTestClazzFolders() {
      return testClazzFolders;
   }

   public void setTestClazzFolders(final List<String> testClazzFolders) {
      this.testClazzFolders = testClazzFolders;
   }

   public void setCreateDetailDebugFiles(final boolean createDetailDebugFiles) {
      this.createDetailDebugFiles = createDetailDebugFiles;
   }

   public boolean isCreateDetailDebugFiles() {
      return createDetailDebugFiles;
   }

   public String getProperties() {
      return properties;
   }

   public void setProperties(final String properties) {
      this.properties = properties;
   }

   @JsonIgnore
   public List<String> getAllClazzFolders() {
      List<String> allFolders = new LinkedList<>();
      allFolders.addAll(clazzFolders);
      allFolders.addAll(testClazzFolders);
      return allFolders;
   }
}
