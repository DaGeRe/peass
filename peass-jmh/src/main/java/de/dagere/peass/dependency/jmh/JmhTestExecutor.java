package de.dagere.peass.dependency.jmh;

import java.io.File;
import java.io.IOException;

import de.dagere.nodeDiffDetector.data.TestCase;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.config.MeasurementStrategy;
import de.dagere.peass.execution.kieker.ArgLineBuilder;
import de.dagere.peass.execution.maven.AllModulePomPreparer;
import de.dagere.peass.execution.maven.MavenCleaner;
import de.dagere.peass.execution.maven.MavenRunningTester;
import de.dagere.peass.execution.maven.pom.MavenPomUtil;
import de.dagere.peass.execution.maven.pom.MavenTestExecutor;
import de.dagere.peass.execution.processutils.ProcessBuilderHelper;
import de.dagere.peass.execution.utils.CommandConcatenator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.TestTransformer;

/**
 * Supports the execution of JMH tests, which is necessary to do their regression test selection. Currently only supports maven projects.
 * 
 * @author reichelt
 *
 */
public class JmhTestExecutor extends TestExecutor {

   private final JmhTestTransformer transformer;

   public JmhTestExecutor(final PeassFolders folders, final TestTransformer testTransformer, final EnvironmentVariables env) {
      super(folders, testTransformer, env);
      this.transformer = (JmhTestTransformer) testTransformer;
   }

   @Override
   public void prepareKoPeMeExecution(final File logFile) {

      prepareKiekerSource();
      AllModulePomPreparer allModulePomPreparer = new AllModulePomPreparer(testTransformer, getModules(), folders);
      allModulePomPreparer.preparePom();

      String[] basicParameters = new String[] { env.fetchMavenCall(folders.getProjectFolder()),
            "--batch-mode",
            "clean", "package",
            "-DskipTests",
            "-Dmaven.test.skip.exec" };
      String[] withMavendefaults = CommandConcatenator.concatenateCommandArrays(basicParameters, CommandConcatenator.mavenCheckDeactivation);
      String[] withPl = MavenTestExecutor.addMavenPl(testTransformer.getConfig().getExecutionConfig(), withMavendefaults);

      ProcessBuilderHelper builder = new ProcessBuilderHelper(env, folders);
      Process process = builder.buildFolderProcess(folders.getProjectFolder(), logFile, withPl);
      execute("jmh-package", transformer.getConfig().getTimeoutInSeconds(), process);
   }

   @Override
   public void executeTest(String javaAgent, final TestMethodCall test, final File logFolder, final long timeoutInSeconds) {
      throw new RuntimeException("Not implemented yet");
   }

   @Override
   public void executeTest(final TestMethodCall test, final File logFolder, final long timeoutInSeconds) {
      checkConfiguration(timeoutInSeconds);

      File jsonResultFile = new File(folders.getTempMeasurementFolder(), test.getMethod() + ".json");
      String[] mergedParameters = buildParameterString(test, jsonResultFile);

      ProcessBuilderHelper builder = new ProcessBuilderHelper(env, folders);
      File logFile = getMethodLogFile(logFolder, test);
      Process process = builder.buildFolderProcess(folders.getProjectFolder(), logFile, mergedParameters);

      execute(test.getExecutable(), transformer.getConfig().getTimeoutInSeconds(), process);

      new JmhResultMover(folders, transformer.getConfig()).moveToMethodFolder(test, jsonResultFile);
   }

   private void checkConfiguration(final long timeoutInSeconds) {
      if (testTransformer.getConfig().getAllIterations() * testTransformer.getConfig().getRepetitions() > timeoutInSeconds) {
         throw new RuntimeException("Your configured warmup+iterations " +
               testTransformer.getConfig().getAllIterations() + " and duration "
               + testTransformer.getConfig().getRepetitions() + " are expected to take longer than the given timeout " + timeoutInSeconds + " seconds. "
               + "Please be aware that the repetitions parameter in JMH is used as iteration duration!");
      }
   }

   private String[] buildParameterString(final TestMethodCall test, final File jsonResultFile) {
      String[] basicParameters;
      if (testTransformer.getConfig().getKiekerConfig().isUseKieker()) {
         basicParameters = buildKiekerParameters(test);
      } else {
         basicParameters = new String[] { "java" };
      }
      if (testTransformer.getConfig().getMeasurementStrategy() == MeasurementStrategy.PARALLEL) {
         basicParameters = CommandConcatenator.concatenateCommandArrays(basicParameters, new String[] { "-Djmh.ignoreLock=true" });
      }
      String jarPath = (test.getModule() == null || test.getModule().equals("")) ? "target/benchmarks.jar" : test.getModule() + File.separator + "target/benchmarks.jar";
      String executable = test.getMethod() != null ? test.getClazz() + "." + test.getMethod() : test.getClazz();
      String benchmarkMode;
      if (testTransformer.getConfig().getAllIterations() == 1) {
         /**
          * SingleShotTime is set if only one iteration is done, cause this normaly implicates this is a rts trace getting run
          * 
          * Dirty hack, since somebody might also realy want to execute one iteration for throughput
          */
         benchmarkMode = "SingleShotTime";
      } else {
         benchmarkMode = "Throughput";
      }

      String[] jmhParameters = new String[] {
            "-jar",
            jarPath,
            executable,
            "-bm",
            benchmarkMode,
            "-f", "1",
            "-i",
            Integer.toString(transformer.getConfig().getAllIterations()),
            "-wi",
            Integer.toString(0),
            "-r",
            Integer.toString(testTransformer.getConfig().getRepetitions()),
            "-rf",
            "json", // JSON format is needed, since VM-internal measurement values are required
            "-rff",
            jsonResultFile.getAbsolutePath() };
      String[] mergedParameters = CommandConcatenator.concatenateCommandArrays(basicParameters, jmhParameters);
      return mergedParameters;
   }

   private String[] buildKiekerParameters(final TestCase test) {
      String[] basicParameters;
      File moduleFolder = new File(folders.getProjectFolder(), test.getModule());
      folders.getTempMeasurementFolder().mkdirs();

      File lastTmpFile = folders.getTempMeasurementFolder();
      ArgLineBuilder builder = new ArgLineBuilder(transformer, moduleFolder);
      String originalArgLine = builder.buildArglineMaven(lastTmpFile);
      String argLine = originalArgLine
            .replace("'", "") // jmh does not accept ' surrounding the path
            .replace("\"", "")
            .replace("${user.home}", System.getProperty("user.home"));
      String[] splittedArgs = argLine.split(" ");
      basicParameters = CommandConcatenator.concatenateCommandArrays(new String[] { "java" }, splittedArgs);
      return basicParameters;
   }

   @Override
   public boolean doesBuildfileExist() {
      File pomFile = new File(folders.getProjectFolder(), "pom.xml");
      boolean buildfileExists = pomFile.exists();
      return buildfileExists;
   }

   @Override
   public boolean isCommitRunning(final String version) {
      MavenRunningTester mavenRunningTester = new MavenRunningTester(folders, testTransformer.getConfig(), env, getModules());
      boolean isRunning = mavenRunningTester.isCommitRunning(version, this);
      return isRunning;
   }

   @Override
   public ProjectModules getModules() {
      File pomFile = new File(folders.getProjectFolder(), "pom.xml");
      return MavenPomUtil.getModules(pomFile, transformer.getConfig().getExecutionConfig());
   }

   @Override
   protected void clean(final File logFile) throws IOException, InterruptedException {
      new MavenCleaner(folders, env).clean(logFile);
   }

}
