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

   @Option(names = { "-version", "--version" }, description = "Newer version for regression test selection / measurement. Do not use together with startversion / endversion.")
   protected String version;

   @Option(names = { "-versionOld", "--versionOld" }, description = "Older version for regression test selection / measurement" +
         "If used, please always specify version; only the difference of both will be analyzed, intermediary versions will be ignored. Do not use together with startversion / endversion.")
   protected String versionOld;

   @Option(names = { "-startversion", "--startversion" }, description = "First version that should be analysed - do not use together with version and versionOld!")
   protected String startversion;

   @Option(names = { "-endversion", "--endversion" }, description = "Last version that should be analysed - do not use together with version and versionOld! ")
   protected String endversion;

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

   @Option(names = { "-excludeLog4j", "--excludeLog4j" }, description = "Exclude log4j (required, if other logging implementation should be used)")
   protected boolean excludeLog4j = false;

   @Option(names = { "-dontRedirectToNull",
         "--dontRedirectToNull" }, description = "Activates showing the standard output of the testcase (by default, it is redirected to null)")
   protected boolean dontRedirectToNull = false;
   
   @Option(names = { "-properties", "--properties" }, description = "Sets the properties that should be passed to the test (e.g. \"-Dmy.var=5\")")
   public String properties;

   @Option(names = { "-forbiddenMethods", "--forbiddenMethods" }, description = "Unit tests, that call one of the methods, are excluded")
   protected String[] forbiddenMethods;

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

   public void setTestGoal(final String testGoal) {
      this.testGoal = testGoal;
   }

   public String getTestGoal() {
      return testGoal;
   }

   public String getStartversion() {
      return startversion;
   }

   public void setStartversion(final String startversion) {
      this.startversion = startversion;
   }

   public String getEndversion() {
      return endversion;
   }

   public void setEndversion(final String endversion) {
      this.endversion = endversion;
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

   public boolean isExcludeLog4j() {
      return excludeLog4j;
   }

   public void setExcludeLog4j(final boolean excludeLog4j) {
      this.excludeLog4j = excludeLog4j;
   }

   public boolean isDontRedirectToNull() {
      return dontRedirectToNull;
   }

   public void setDontRedirectToNull(final boolean dontRedirectToNull) {
      this.dontRedirectToNull = dontRedirectToNull;
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

      config.setVersion(version);
      config.setVersionOld(versionOld);
      config.setStartversion(getStartversion());
      config.setEndversion(getEndversion());
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

      if (getClazzFolder() != null) {
         List<String> clazzFolders = ExecutionConfig.buildFolderList(getClazzFolder());
         config.setClazzFolders(clazzFolders);
      }
      if (getTestClazzFolder() != null) {
         List<String> testClazzFolders = ExecutionConfig.buildFolderList(getTestClazzFolder());
         config.setTestClazzFolders(testClazzFolders);
      }

      config.setExcludeLog4j(excludeLog4j);
      config.setRedirectToNull(!dontRedirectToNull);

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
