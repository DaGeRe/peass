package de.dagere.peass.dependency.jmh;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.execution.CommandConcatenator;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.execution.MavenPomUtil;
import de.dagere.peass.dependency.execution.MavenTestExecutor;
import de.dagere.peass.dependency.execution.ProjectModules;
import de.dagere.peass.dependency.execution.TestExecutor;
import de.dagere.peass.execution.maven.MavenCleaner;
import de.dagere.peass.execution.maven.MavenRunningTester;
import de.dagere.peass.execution.maven.PomPreparer;
import de.dagere.peass.execution.processutils.ProcessBuilderHelper;
import de.dagere.peass.testtransformation.TestTransformer;

/**
 * Supports the execution of JMH tests, which is necessary to do their regression test selection. Currently only supports maven projects.
 * 
 * @author reichelt
 *
 */
public class JmhTestExecutor extends TestExecutor {

   private final JmhTestTransformer transformer;

   public JmhTestExecutor(final PeASSFolders folders, final TestTransformer testTransformer, final EnvironmentVariables env) {
      super(folders, testTransformer, env);
      this.transformer = (JmhTestTransformer) testTransformer;
   }
   
   @Override
   public void prepareKoPeMeExecution(final File logFile) throws IOException, InterruptedException, XmlPullParserException {
      prepareKiekerSource();
      new PomPreparer(testTransformer, getModules(), folders).preparePom();

      String[] basicParameters = new String[] { env.fetchMavenCall(),
            "clean", "package",
            "-DskipTests",
            "-Dmaven.test.skip.exec" };
      String[] withMavendefaults = CommandConcatenator.concatenateCommandArrays(basicParameters, CommandConcatenator.mavenCheckDeactivation);
      String[] withPl = MavenTestExecutor.addMavenPl(testTransformer.getConfig().getExecutionConfig(), withMavendefaults);

      ProcessBuilderHelper builder = new ProcessBuilderHelper(env, folders);
      Process process = builder.buildFolderProcess(folders.getProjectFolder(), logFile, withPl);
      execute("jmh-package", transformer.getConfig().getTimeoutInMinutes(), process);
   }

   @Override
   public void executeAllKoPeMeTests(final File logFile) throws IOException, XmlPullParserException, InterruptedException {
      throw new RuntimeException("Not implemented yet");
   }

   @Override
   public void executeTest(final TestCase test, final File logFolder, final long timeout) {
      try {
         File jsonResultFile = new File(folders.getTempMeasurementFolder(), test.getMethod()+ ".json");
         String[] mergedParameters = buildParameterString(test, jsonResultFile);

         ProcessBuilderHelper builder = new ProcessBuilderHelper(env, folders);
         File logFile = getMethodLogFile(logFolder, test);
         Process process = builder.buildFolderProcess(folders.getProjectFolder(), logFile, mergedParameters);

         execute(test.getExecutable(), transformer.getConfig().getTimeoutInMinutes(), process);

         new JmhResultMover(folders, transformer.getConfig()).moveToMethodFolder(test, jsonResultFile);

      } catch (InterruptedException | IOException e) {
         throw new RuntimeException(e);
      }
   }

   private String[] buildParameterString(final TestCase test, final File jsonResultFile) {
      String[] basicParameters;
      if (testTransformer.getConfig().isUseKieker()) {
         File moduleFolder = new File(folders.getProjectFolder(), test.getModule());
         folders.getTempMeasurementFolder().mkdirs();
         basicParameters = new String[] {
               "java",
               "-Dkieker.monitoring.configuration=" + moduleFolder.getAbsolutePath() + "/src/main/resources/META-INF/kieker.monitoring.properties",
               "-Djava.io.tmpdir=" + folders.getTempMeasurementFolder().getAbsolutePath() };
      } else {
         basicParameters = new String[] { "java" };
      }
      String jarPath = (test.getModule() == null || test.getModule().equals("")) ? "target/benchmarks.jar" : test.getModule() + File.separator + "target/benchmarks.jar";
      String executable = test.getMethod() != null ? test.getClazz() + "." + test.getMethod() : test.getClazz();
      String[] jmhParameters = new String[] {
            "-jar",
            jarPath,
            executable,
            "-bm", "SingleShotTime",
            "-f",
            Integer.toString(transformer.getConfig().getVms()),
            "-i",
            Integer.toString(transformer.getConfig().getIterations()),
            "-wi",
            Integer.toString(transformer.getConfig().getWarmup()),
            "-rf", 
            "json", // JSON format is needed, since VM-internal measurement values are required
            "-rff",
            jsonResultFile.getAbsolutePath()  };
      String[] mergedParameters = CommandConcatenator.concatenateCommandArrays(basicParameters, jmhParameters);
      return mergedParameters;
   }

   @Override
   public boolean isVersionRunning(final String version) {
      try {
         MavenRunningTester mavenRunningTester = new MavenRunningTester(folders, env, testTransformer.getConfig(), getModules());
         boolean isRunning = mavenRunningTester.isVersionRunning(version);
         buildfileExists = mavenRunningTester.isBuildfileExists();
         return isRunning;
      } catch (IOException | XmlPullParserException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public ProjectModules getModules() throws IOException, XmlPullParserException {
      File pomFile = new File(folders.getProjectFolder(), "pom.xml");
      return MavenPomUtil.getModules(pomFile);
   }

   @Override
   protected void clean(final File logFile) throws IOException, InterruptedException {
      new MavenCleaner(folders, env).clean(logFile);
   }

}
