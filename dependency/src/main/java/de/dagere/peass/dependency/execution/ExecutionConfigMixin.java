package de.dagere.peass.dependency.execution;

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
   
   @Option(names = {"-workloadType", "--workloadType"}, description = "Which workload should be executed - by default JUNIT, can be changed to JMH")
   public WorkloadType workloadType = WorkloadType.JUNIT;

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
}
