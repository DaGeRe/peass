package de.dagere.peass.config.parameters;

import java.util.List;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.WorkloadType;
import picocli.CommandLine.Option;

public class ExecutionConfigMixin {
   @Option(names = { "-timeout", "--timeout" }, description = "Timeout in minutes for each VM start")
   protected int timeout = 5;
   
   @Option(names = { "-includes", "--includes" }, description = "Testcases for inclusion (default: empty, includes all tests)")
   protected String[] includes;

   @Option(names = { "-excludes", "--excludes" }, description = "Testcases for exclusion (default: empty, excludes no test)")
   protected String[] excludes;

   @Option(names = { "-commit", "--commit" }, description = "Newer commit for regression test selection / measurement. Do not use together with startcommit / endcommit.")
   protected String commit;
   
   @Option(names = { "-commitOld", "--commitOld" }, description = "Older commit for regression test selection / measurement" +
         "If used, please always specify commit; only the difference of both will be analyzed, intermediary commits will be ignored. Do not use together with startcommit / endcommit.")
   protected String commitOld;
   
   @Option(names = { "-startcommit", "--startcommit" }, description = "First commit that should be analysed - do not use together with commit and commitOld!")
   protected String startcommit;

   @Option(names = { "-endcommit", "--endcommit" }, description = "Last commit that should be analysed - do not use together with commit and commitOld! ")
   protected String endcommit;

   @Option(names = { "-testGoal", "--testGoal" }, description = "Test goal that should be used; default testRelease for Android projects and test for all others. "
         + "If you want to use test<VariantName> for Android, please specify a goal (i.e. task name) here."
         + "If you want to run integration tests in maven e.g. by calling failsafe, also specify it here. ")
   protected String testGoal;

   @Option(names = { "-pl", "--pl" }, description = "Projectlist (-pl) argument for maven (e.g. :submodule) - only the submodule and its dependencies are analyzed (using -am)")
   protected String pl;

   @Option(names = { "-workloadType", "--workloadType" }, description = "Which workload should be executed - by default JUNIT, can be changed to JMH")
   public WorkloadType workloadType = WorkloadType.JUNIT;

   @Option(names = { "-testExecutor", "--testExecutor" }, description = "Set test executor (should be specified by plugin; not usable with pure Peass)")
   public String testExecutor;

   @Option(names = { "-testTransformer", "--testTransformer" }, description = "Set test transformer (should be specified by plugin; not usable with pure Peass)")
   public String testTransformer;

   @Option(names = { "-useTieredCompilation", "--useTieredCompilation" }, description = "Activate -XX:-TieredCompilation for all measured processes")
   protected boolean useTieredCompilation = false;

   @Option(names = { "-executeBeforeClassInMeasurement", "--executeBeforeClassInMeasurement" }, description = "Execute @BeforeClass / @BeforeAll in measurement loop")
   protected boolean executeBeforeClassInMeasurement = false;

   @Option(names = { "-removeSnapshots",
         "--removeSnapshots" }, description = "Activates removing SNAPSHOTS (if older versions should be analysed, this should be activated; for performance measurement in CI, this should not be activated)")
   protected boolean removeSnapshots = false;

   @Option(names = { "-skipDefaultConstructor",
         "--skipDefaultConstructor" }, description = "Deactivates creation of the default constructor (required if Lombok is used)")
   protected boolean skipDefaultConstructor = false;

   @Option(names = { "-useAlternativeBuildfile",
         "--useAlternativeBuildfile" }, description = "Use alternative buildfile when existing (searches for alternative_build.gradle and replaces build.gradle with the file; required e.g. if the default build process contains certification)")
   protected boolean useAlternativeBuildfile = false;

   @Option(names = { "-kiekerWaitTime", "--kiekerWaitTime" }, description = "Time that KoPeMe should wait until Kieker writing is finshed in seconds (default: 10)")
   protected int kiekerWaitTime = 5;

   @Option(names = { "-classFolder", "--classFolder" }, description = "Folder that contains java classes")
   protected String clazzFolder;

   @Option(names = { "-testClassFolder", "--testClassFolder" }, description = "Folder that contains test classes")
   protected String testClazzFolder;

   @Option(names = { "-excludeLog4jToSlf4j", "--excludeLog4jToSlf4j" }, description = "Exclude log4j-to-slf4j (required, if other logging implementation should be used)")
   protected boolean excludeLog4jToSlf4j = false;
   
   @Option(names = { "-excludeLog4jSlf4jImpl", "--excludeLog4jSlf4jImpl" }, description = "Exclude log4j-slf4j-impl (required, if other logging implementation should be used)")
   protected boolean excludeLog4jSlf4jImpl = false;

   @Option(names = { "-dontRedirectToNull",
         "--dontRedirectToNull" }, description = "Activates showing the standard output of the testcase (by default, it is redirected to null)")
   protected boolean dontRedirectToNull = false;

   @Option(names = { "-onlyMeasureWorkload", "--onlyMeasureWorkload" }, description = "Only measure workload (no @Before/@After)")
   protected boolean onlyMeasureWorkload = false;
   
   @Option(names = { "-properties", "--properties" }, description = "Sets the properties that should be passed to the test (e.g. \"-Dmy.var=5\")")
   public String properties;

   @Option(names = { "-forbiddenMethods", "--forbiddenMethods" }, description = "Unit tests, that call one of the methods, are excluded")
   protected String[] forbiddenMethods;

   @Option(names = { "-gradleJavaPluginName", "--gradleJavaPluginName" }, description = "Sets a custom gradle Java Plugin name")
   public String gradleJavaPluginName = ExecutionConfig.GRADLE_JAVA_DEFAULT_NAME;

   @Option(names = { "-gradleSpringBootPluginName", "--gradleSpringBootPluginName" }, description = "Sets a custom gradle SpringBoot Plugin name")
   public String gradleSpringBootPluginName = ExecutionConfig.GRADLE_SPRING_DEFAULT_NAME;

   public String getGradleJavaPluginName() {
      return gradleJavaPluginName;
   }

   public void setGradleJavaPluginName(String gradleJavaPluginName) {
      this.gradleJavaPluginName = gradleJavaPluginName;
   }

   public String getGradleSpringBootPluginName() {
      return gradleSpringBootPluginName;
   }

   public void setGradleSpringBootPluginName(String gradleSpringBootPluginName) {
      this.gradleSpringBootPluginName = gradleSpringBootPluginName;
   }

   public int getTimeout() {
      return timeout;
   }

   public void setTimeout(final int timeout) {
      this.timeout = timeout;
   }

   public void setIncludes(final String[] includes) {
      this.includes = includes;
   }

   public String[] getIncludes() {
      return includes;
   }

   public void setTestGoal(final String testGoal) {
      this.testGoal = testGoal;
   }

   public String getTestGoal() {
      return testGoal;
   }

   public String getCommit() {
      return commit;
   }

   public void setCommit(String commit) {
      this.commit = commit;
   }

   public String getCommitOld() {
      return commitOld;
   }

   public void setCommitOld(String commitOld) {
      this.commitOld = commitOld;
   }

   public String getStartcommit() {
      return startcommit ;
   }

   public void setStartcommit(String startcommit) {
      this.startcommit = startcommit;
   }

   public String getEndcommit() {
      return endcommit ;
   }

   public void setEndcommit(String endcommit) {
      this.endcommit = endcommit;
   }

   public void setPl(final String pl) {
      this.pl = pl;
   }

   public String getPl() {
      return pl;
   }

   public boolean isRemoveSnapshots() {
      return removeSnapshots;
   }

   public void setRemoveSnapshots(final boolean removeSnapshots) {
      this.removeSnapshots = removeSnapshots;
   }

   public boolean isSkipDefaultConstructor() {
      return skipDefaultConstructor;
   }

   public void setSkipDefaultConstructor(final boolean skipDefaultConstructor) {
      this.skipDefaultConstructor = skipDefaultConstructor;
   }

   public boolean isUseAlternativeBuildfile() {
      return useAlternativeBuildfile;
   }

   public void setUseAlternativeBuildfile(final boolean useAlternativeBuildfile) {
      this.useAlternativeBuildfile = useAlternativeBuildfile;
   }

   public String[] getExcludes() {
      return excludes;
   }

   public void setExcludes(final String[] excludes) {
      this.excludes = excludes;
   }

   public WorkloadType getWorkloadType() {
      return workloadType;
   }

   public void setWorkloadType(final WorkloadType workloadType) {
      this.workloadType = workloadType;
   }

   public String getTestExecutor() {
      return testExecutor;
   }

   public void setTestExecutor(final String testExecutor) {
      this.testExecutor = testExecutor;
   }

   public String getTestTransformer() {
      return testTransformer;
   }

   public void setTestTransformer(final String testTransformer) {
      this.testTransformer = testTransformer;
   }

   public boolean isUseTieredCompilation() {
      return useTieredCompilation;
   }

   public void setUseTieredCompilation(final boolean useTieredCompilation) {
      this.useTieredCompilation = useTieredCompilation;
   }

   public boolean isExecuteBeforeClassInMeasurement() {
      return executeBeforeClassInMeasurement;
   }

   public void setExecuteBeforeClassInMeasurement(final boolean executeBeforeClassInMeasurement) {
      this.executeBeforeClassInMeasurement = executeBeforeClassInMeasurement;
   }

   public int getKiekerWaitTime() {
      return kiekerWaitTime;
   }

   public void setKiekerWaitTime(final int kiekerWaitTime) {
      this.kiekerWaitTime = kiekerWaitTime;
   }

   public String getClazzFolder() {
      return clazzFolder;
   }

   public void setClazzFolder(final String clazzFolder) {
      this.clazzFolder = clazzFolder;
   }

   public String getTestClazzFolder() {
      return testClazzFolder;
   }

   public void setTestClazzFolder(final String testClazzFolder) {
      this.testClazzFolder = testClazzFolder;
   }
   
   public boolean isExcludeLog4jSlf4jImpl() {
      return excludeLog4jSlf4jImpl;
   }
   
   public void setExcludeLog4jSlf4jImpl(boolean excludeLog4jSlf4jImpl) {
      this.excludeLog4jSlf4jImpl = excludeLog4jSlf4jImpl;
   }
   
   public boolean isExcludeLog4jToSlf4j() {
      return excludeLog4jToSlf4j;
   }
   
   public void setExcludeLog4jToSlf4j(boolean excludeLog4jToSlf4j) {
      this.excludeLog4jToSlf4j = excludeLog4jToSlf4j;
   }

   public boolean isDontRedirectToNull() {
      return dontRedirectToNull;
   }

   public void setDontRedirectToNull(final boolean dontRedirectToNull) {
      this.dontRedirectToNull = dontRedirectToNull;
   }
   
   public boolean isOnlyMeasureWorkload() {
      return onlyMeasureWorkload;
   }
   
   public void setOnlyMeasureWorkload(boolean onlyMeasureWorkload) {
      this.onlyMeasureWorkload = onlyMeasureWorkload;
   }
   
   public String getProperties() {
      return properties;
   }
   
   public void setProperties(final String properties) {
      this.properties = properties;
   }

   public String[] getForbiddenMethods() {
      return forbiddenMethods;
   }

   public void setForbiddenMethods(String[] forbiddenMethods) {
      this.forbiddenMethods = forbiddenMethods;
   }

   public ExecutionConfig getExecutionConfig() {
      ExecutionConfig config = new ExecutionConfig(timeout);

      config.setCommit(getCommit());
      config.setCommitOld(getCommitOld());
      config.setStartcommit(getStartcommit());
      config.setEndcommit(getEndcommit());
      config.setTestGoal(getTestGoal());

      if (getIncludes() != null) {
         for (String include : getIncludes()) {
            config.getIncludes().add(include);
         }
      }
      if (getExcludes() != null) {
         for (String exclude : getExcludes()) {
            config.getExcludes().add(exclude);
         }
      }
      if (getPl() != null) {
         config.setPl(pl);
      }
      boolean transformerSet = getTestTransformer() != null;
      boolean executorSet = getTestExecutor() != null;
      if (transformerSet && executorSet) {
         config.setTestTransformer(getTestTransformer());
         config.setTestExecutor(getTestExecutor());
      } else if (transformerSet != executorSet) {
         throw new RuntimeException("If the test transformer is set by CLI parameters, the test executor needs also be set!");
      } else {
         config.setTestTransformer(getWorkloadType().getTestTransformer());
         config.setTestExecutor(getWorkloadType().getTestExecutor());
      }
      config.setUseTieredCompilation(useTieredCompilation);
      config.setRemoveSnapshots(removeSnapshots);
      config.setUseAlternativeBuildfile(useAlternativeBuildfile);
      config.setRemoveSnapshots(removeSnapshots);
      config.setCreateDefaultConstructor(!skipDefaultConstructor);
      config.setExecuteBeforeClassInMeasurement(executeBeforeClassInMeasurement);
      config.setKiekerWaitTime(kiekerWaitTime);
      config.setProperties(properties);
      config.setGradleJavaPluginName(gradleJavaPluginName);
      config.setGradleSpringBootPluginName(gradleSpringBootPluginName);

      if (getClazzFolder() != null) {
         List<String> clazzFolders = ExecutionConfig.buildFolderList(getClazzFolder());
         config.setClazzFolders(clazzFolders);
      }
      if (getTestClazzFolder() != null) {
         List<String> testClazzFolders = ExecutionConfig.buildFolderList(getTestClazzFolder());
         config.setTestClazzFolders(testClazzFolders);
      }

      config.setExcludeLog4jSlf4jImpl(excludeLog4jSlf4jImpl);
      config.setExcludeLog4jToSlf4j(excludeLog4jToSlf4j);
      config.setRedirectToNull(!dontRedirectToNull);
      config.setOnlyMeasureWorkload(onlyMeasureWorkload);
      
      if (config.isExecuteBeforeClassInMeasurement() && config.isOnlyMeasureWorkload()) {
         throw new RuntimeException("executeBeforeClassInMeasurement may only be activated if onlyMeasureWorkload is deactivated!");
      }
      
      if (getForbiddenMethods() != null) {
         for (String forbiddenMethod : getForbiddenMethods()) {
            config.getForbiddenMethods().add(forbiddenMethod);
         }
      }

      return config;
   }
}
