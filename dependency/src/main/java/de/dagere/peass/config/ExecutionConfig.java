package de.dagere.peass.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.dagere.nodeDiffDetector.config.FolderConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration properties of Peass-executions that are used in every circumstance, i.e. for regression test selection, measurement and root cause analysis
 * 
 * @author DaGeRe
 *
 */
public class ExecutionConfig implements FolderConfig, Serializable {

   public static final String CLASSPATH_SEPARATOR = ":";

   public static final String DEFAULT_TEST_TRANSFORMER = "de.dagere.peass.testtransformation.JUnitTestTransformer";
   public static final String DEFAULT_TEST_EXECUTOR = "default";

   public static final String SRC_JAVA = "src/java";
   public static final String SRC_MAIN_JAVA = "src/main/java";

   public static final String SRC_TEST_JAVA = "src/test/java";
   public static final String SRC_TEST = "src/test";
   public static final String SRC_ANDROID_TEST_JAVA = "src/androidTest/java/";

   private static final long serialVersionUID = -6642358125854337047L;

   /**
    * Timeout in milliseconds, default 5 minutes
    */
   private long timeout = 5 * 60 * 1000;
   private String cleanGoal;
   private String testGoal;
   private List<String> includes = new LinkedList<>();
   private List<String> excludes = new LinkedList<>();
   private List<String> includeByRule = new LinkedList<>();
   private List<String> excludeByRule = new LinkedList<>();

   private List<String> forbiddenMethods = new LinkedList<>();

   protected String startcommit;
   protected String endcommit;
   private boolean linearizeHistory;
   private String pl;

   private boolean redirectSubprocessOutputToFile = true;
   private boolean useTieredCompilation = false;

   private boolean removeSnapshots = false;
   private boolean useAlternativeBuildfile = false;
   private boolean excludeLog4jSlf4jImpl = false;
   private boolean excludeLog4jToSlf4j = false;

   private boolean executeBeforeClassInMeasurement = false;
   private boolean clearMockitoCaches = false;
   private boolean onlyMeasureWorkload = false;
   private boolean showStart = false;
   private boolean redirectToNull = true;
   private boolean createDetailDebugFiles = true;

   private String testTransformer = DEFAULT_TEST_TRANSFORMER;
   private String testExecutor = DEFAULT_TEST_EXECUTOR;
   private String gitCryptKey;
   private boolean useAnbox = false;
   private String androidCompileSdkVersion;
   private String androidMinSdkVersion;
   private String androidTargetSdkVersion;
   private String androidManifest;
   private List<String> androidGradleTasks = new LinkedList<>();
   private String androidGradleVersion;
   private String androidTestPackageName;

   private String xmx = null;

   private List<String> increaseVariableValues = new LinkedList<>();

   private String properties;

   private List<String> clazzFolders = new LinkedList<>();
   {
      clazzFolders.add(SRC_MAIN_JAVA);
      clazzFolders.add(SRC_JAVA);
   }
   private List<String> testClazzFolders = new LinkedList<>();
   {
      testClazzFolders.add(SRC_TEST_JAVA);
      testClazzFolders.add(SRC_TEST);
      testClazzFolders.add(SRC_ANDROID_TEST_JAVA);
   }

   public ExecutionConfig() {
      testGoal = null;
   }

   public ExecutionConfig(final ExecutionConfig other) {
      this.timeout = other.getTimeout();
      this.testGoal = other.getTestGoal();
      this.cleanGoal = other.getCleanGoal();
      this.includes = other.getIncludes();
      this.excludes = other.getExcludes();
      this.includeByRule = other.getIncludeByRule();
      this.excludeByRule = other.getExcludeByRule();
      this.forbiddenMethods = other.getForbiddenMethods();
      this.startcommit = other.getStartcommit();
      this.endcommit = other.getEndcommit();
      this.linearizeHistory = other.linearizeHistory;
      this.redirectSubprocessOutputToFile = other.isRedirectSubprocessOutputToFile();
      this.removeSnapshots = other.removeSnapshots;
      this.useAlternativeBuildfile = other.useAlternativeBuildfile;
      this.excludeLog4jSlf4jImpl = other.excludeLog4jSlf4jImpl;
      this.excludeLog4jToSlf4j = other.excludeLog4jToSlf4j;
      this.testTransformer = other.getTestTransformer();
      this.testExecutor = other.getTestExecutor();
      this.gitCryptKey = other.getGitCryptKey();
      this.useTieredCompilation = other.isUseTieredCompilation();
      this.pl = other.getPl();

      this.executeBeforeClassInMeasurement = other.executeBeforeClassInMeasurement;
      this.clearMockitoCaches = other.clearMockitoCaches;
      this.onlyMeasureWorkload = other.onlyMeasureWorkload;
      this.showStart = other.showStart;
      this.redirectToNull = other.redirectToNull;

      this.clazzFolders = other.clazzFolders;
      this.testClazzFolders = other.testClazzFolders;

      this.useAnbox = other.useAnbox;
      this.androidCompileSdkVersion = other.androidCompileSdkVersion;
      this.androidMinSdkVersion = other.androidMinSdkVersion;
      this.androidTargetSdkVersion = other.androidTargetSdkVersion;
      this.androidManifest = other.androidManifest;
      this.androidGradleTasks = other.androidGradleTasks;
      this.androidGradleVersion = other.androidGradleVersion;
      this.androidTestPackageName = other.androidTestPackageName;

      this.xmx = other.xmx;

      this.increaseVariableValues = other.getIncreaseVariableValues();
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
   public String getCleanGoal() {
      return cleanGoal;
   }

   public void setCleanGoal(String cleanGoal) {
      this.cleanGoal = cleanGoal;
   }

   @JsonInclude(Include.NON_NULL)
   public String getTestGoal() {
      return testGoal;
   }

   public void setTestGoal(final String testGoal) {
      this.testGoal = testGoal;
   }

   @JsonInclude(Include.NON_EMPTY)
   public List<String> getIncludes() {
      return includes;
   }

   public void setIncludes(final List<String> includes) {
      this.includes = includes;
   }

   @JsonInclude(Include.NON_EMPTY)
   public List<String> getExcludes() {
      return excludes;
   }

   public void setExcludes(final List<String> excludes) {
      this.excludes = excludes;
   }

   @JsonInclude(Include.NON_EMPTY)
   public List<String> getIncludeByRule() {
      return includeByRule;
   }

   public void setIncludeByRule(List<String> includeByRule) {
      this.includeByRule = includeByRule;
   }

   @JsonInclude(Include.NON_EMPTY)
   public List<String> getExcludeByRule() {
      return excludeByRule;
   }

   public void setExcludeByRule(List<String> excludeByRule) {
      this.excludeByRule = excludeByRule;
   }

   @JsonInclude(Include.NON_EMPTY)
   public List<String> getForbiddenMethods() {
      return forbiddenMethods;
   }

   public void setForbiddenMethods(final List<String> forbiddenMethods) {
      this.forbiddenMethods = forbiddenMethods;
   }

   @JsonIgnore
   public long getTimeoutInSeconds() {
      return timeout / 1000;
   }

   @JsonInclude(JsonInclude.Include.NON_NULL)
   public String getStartcommit() {
      return startcommit;
   }

   public void setStartcommit(final String startCommit) {
      this.startcommit = startCommit;
   }

   @JsonInclude(JsonInclude.Include.NON_NULL)
   public String getEndcommit() {
      return endcommit;
   }

   public void setEndcommit(final String endCommit) {
      this.endcommit = endCommit;
   }

   public void setLinearizeHistory(final boolean linearizeHistory) {
      this.linearizeHistory = linearizeHistory;
   }

   public boolean isLinearizeHistory() {
      return linearizeHistory;
   }

   @JsonInclude(JsonInclude.Include.NON_NULL)
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

   public boolean isExcludeLog4jSlf4jImpl() {
      return excludeLog4jSlf4jImpl;
   }

   public void setExcludeLog4jSlf4jImpl(final boolean excludeLog4jSlf4jImpl) {
      this.excludeLog4jSlf4jImpl = excludeLog4jSlf4jImpl;
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
   
   public boolean isClearMockitoCaches() {
      return clearMockitoCaches;
   }
   
   public void setClearMockitoCaches(boolean clearMockitoCaches) {
      this.clearMockitoCaches = clearMockitoCaches;
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

   @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = TestTransformerFilter.class)
   public String getTestTransformer() {
      return testTransformer;
   }

   public void setTestTransformer(final String testTransformer) {
      this.testTransformer = testTransformer;
   }

   @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = TestExecutorFilter.class)
   public String getTestExecutor() {
      return testExecutor;
   }

   public void setTestExecutor(final String testExecutor) {
      this.testExecutor = testExecutor;
   }

   @JsonInclude(JsonInclude.Include.NON_NULL)
   public String getGitCryptKey() {
      return gitCryptKey;
   }

   public void setGitCryptKey(final String gitCryptKey) {
      this.gitCryptKey = gitCryptKey;
   }

   @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = ClazzFoldersFilter.class)
   public List<String> getClazzFolders() {
      return clazzFolders;
   }

   public void setClazzFolders(final List<String> clazzFolders) {
      this.clazzFolders = clazzFolders;
   }

   @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = TestClazzFoldersFilter.class)
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

   @JsonInclude(JsonInclude.Include.NON_NULL)
   public String getProperties() {
      return properties;
   }

   public void setProperties(final String properties) {
      this.properties = properties;
   }

   @JsonInclude(value = JsonInclude.Include.NON_NULL)
   public String getGradleJavaPluginName() {
      return null;
   }

   public void setGradleJavaPluginName(final String gradleJavaPluginName) {
   }

   @JsonInclude(value = JsonInclude.Include.NON_NULL)
   public String getGradleSpringBootPluginName() {
      return null;
   }

   public void setGradleSpringBootPluginName(final String gradleSpringBootPluginName) {
   }

   @JsonIgnore
   public List<String> getAllClazzFolders() {
      List<String> allFolders = new LinkedList<>();
      allFolders.addAll(clazzFolders);
      allFolders.addAll(testClazzFolders);
      return allFolders;
   }

   public boolean isUseAnbox() {
      return useAnbox;
   }

   @JsonInclude(value = JsonInclude.Include.NON_DEFAULT)
   public void setUseAnbox(boolean useAnbox) {
      this.useAnbox = useAnbox;
   }

   @JsonInclude(JsonInclude.Include.NON_NULL)
   public String getAndroidCompileSdkVersion() {
      return androidCompileSdkVersion;
   }

   public void setAndroidCompileSdkVersion(String androidCompileSdkVersion) {
      this.androidCompileSdkVersion = androidCompileSdkVersion;
   }
   
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public String getAndroidMinSdkVersion() {
      return androidMinSdkVersion;
   }

   public void setAndroidMinSdkVersion(String androidMinSdkVersion) {
      this.androidMinSdkVersion = androidMinSdkVersion;
   }
   
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public String getAndroidTargetSdkVersion() {
      return androidTargetSdkVersion;
   }
   
   public void setAndroidTargetSdkVersion(String androidTargetSdkVersion) {
      this.androidTargetSdkVersion = androidTargetSdkVersion;
   }

   @JsonInclude(JsonInclude.Include.NON_NULL)
   public String getAndroidManifest() {
      return androidManifest;
   }

   public void setAndroidManifest(String androidManifest) {
      this.androidManifest = androidManifest;
   }

   public String getXmx() {
      return xmx;
   }

   @JsonInclude(value = JsonInclude.Include.NON_NULL)
   public void setXmx(String xmx) {
      if (xmx != null && !xmx.isEmpty() && !xmx.matches("[0-9]*g") && !xmx.matches("[0-9]*m")) {
         throw new RuntimeException("Please specify heap size either in Gigabyte (e.g. 5g) or Megabyte (e.g. 2048m), but was " + xmx);
      }
      if (xmx != null && !xmx.isEmpty()) {
         this.xmx = xmx;
      }
   }
   
   @JsonInclude(Include.NON_EMPTY)
   public List<String> getAndroidGradleTasks() {
      return androidGradleTasks;
   }

   public void setAndroidGradleTasks(final List<String> androidGradleTasks) {
      this.androidGradleTasks = androidGradleTasks;
   }

   @JsonInclude(JsonInclude.Include.NON_NULL)
   public String getAndroidGradleVersion() {
      return androidGradleVersion;
   }

   public void setAndroidGradleVersion(String androidGradleVersion) {
      this.androidGradleVersion = androidGradleVersion;
   }

   @JsonInclude(JsonInclude.Include.NON_NULL)
   public String getAndroidTestPackageName() {
      return androidTestPackageName;
   }

   public void setAndroidTestPackageName(String androidTestPackageName) {
      this.androidTestPackageName = androidTestPackageName;
   }

   @JsonInclude(Include.NON_EMPTY)
   public List<String> getIncreaseVariableValues() {
      return increaseVariableValues;
   }

   public void setIncreaseVariableValues(final List<String> increaseVariableValues) {
      this.increaseVariableValues = increaseVariableValues;
   }

   /**
    * The following boilerplate classes are not very nice; since we want to save most values and eventually change the default values, we cannot use JsonInclude(NON_DEFAULT) at
    * class level; therefore, we need to use custom filters.
    * 
    * @author DaGeRe
    *
    */
   private static final class TestExecutorFilter {
      @Override
      public boolean equals(Object obj) {
         if (obj instanceof String && DEFAULT_TEST_EXECUTOR.equals(obj)) {
            return true;
         }
         return super.equals(obj);
      }
   }

   private static final class TestTransformerFilter {
      @Override
      public boolean equals(Object obj) {
         if (obj instanceof String && DEFAULT_TEST_TRANSFORMER.equals(obj)) {
            return true;
         }
         return super.equals(obj);
      }
   }

   private static final class ClazzFoldersFilter {
      @Override
      public boolean equals(Object obj) {
         if (obj instanceof List) {
            List list = (List) obj;
            if (list.size() == 2 && list.get(0).equals(SRC_MAIN_JAVA) && list.get(1).equals(SRC_JAVA)) {
               return true;
            }
         }
         return super.equals(obj);
      }
   }

   private static final class TestClazzFoldersFilter {
      @Override
      public boolean equals(Object obj) {
         if (obj instanceof List) {
            List list = (List) obj;
            if (list.size() == 3 && list.get(0).equals(SRC_TEST_JAVA) && list.get(1).equals(SRC_TEST) && list.get(2).equals(SRC_ANDROID_TEST_JAVA)) {
               return true;
            }
         }
         return super.equals(obj);
      }
   }
}
