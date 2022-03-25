package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.ci.NonIncludedTestRemover;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.config.parameters.ExecutionConfigMixin;
import de.dagere.peass.dependency.KiekerResultManager;
import de.dagere.peass.dependency.analysis.CalledMethodLoader;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.data.TraceElement;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.GitUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(description = "Generates traces without any additional information", name = "generateTraces")
public class TraceGeneratorStarter implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(TraceGeneratorStarter.class);

   @Mixin
   protected ExecutionConfigMixin executionMixin;

   @Option(names = { "-folder", "--folder" }, description = "Folder of the project that should be analyzed", required = true)
   protected File projectFolder;

   @Option(names = { "-staticSelectionFile", "--staticSelectionFile" }, description = "Path to the staticSelectionFile")
   protected File staticSelectionFile;

   public static void main(final String[] args) {
      final CommandLine commandLine = new CommandLine(new TraceGeneratorStarter());
      commandLine.execute(args);
   }

   private ModuleClassMapping mapping;

   @Override
   public Void call() throws Exception {
      StaticTestSelection staticTestSelection = Constants.OBJECTMAPPER.readValue(staticSelectionFile, StaticTestSelection.class);
      String newestVersion = staticTestSelection.getNewestVersion();

      VersionStaticSelection version = staticTestSelection.getVersions().get(newestVersion);
      TestSet tests = version.getTests();

      ExecutionConfig executionConfig = executionMixin.getExecutionConfig();
      NonIncludedTestRemover.removeNotIncluded(tests, executionConfig);

      GitUtils.reset(projectFolder);
      PeassFolders folders = new PeassFolders(projectFolder);

      KiekerResultManager resultsManager = runTests(newestVersion, tests, folders, executionConfig);

      LOG.info("Analyzing tests: {}", tests.getTests());
      mapping = new ModuleClassMapping(folders.getProjectFolder(), resultsManager.getExecutor().getModules(), executionConfig);
      for (TestCase testcase : tests.getTests()) {
         writeTestcase(newestVersion, folders, resultsManager, testcase);
      }

      return null;
   }

   private KiekerResultManager runTests(final String newestVersion, final TestSet tests, final PeassFolders folders, final ExecutionConfig executionConfig)
         throws IOException, XmlPullParserException, InterruptedException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
         InvocationTargetException, NoSuchMethodException, SecurityException {
      KiekerResultManager resultsManager = new KiekerResultManager(folders, executionConfig, new KiekerConfig(true), new EnvironmentVariables(executionConfig.getProperties()));
      resultsManager.executeKoPeMeKiekerRun(tests, newestVersion, folders.getDependencyLogFolder());
      return resultsManager;
   }

   private void writeTestcase(final String newestVersion, final PeassFolders folders, final KiekerResultManager resultsManager, final TestCase testcase)
         throws FileNotFoundException, IOException, XmlPullParserException, ViewNotFoundException {
      final File moduleResultFolder = KiekerFolderUtil.getModuleResultFolder(folders, testcase);
      final File kiekerResultFolder = KiekerFolderUtil.getClazzMethodFolder(testcase, moduleResultFolder)[0];

      final long size = FileUtils.sizeOfDirectory(kiekerResultFolder);
      final long sizeInMB = size / (1024 * 1024);

      if (sizeInMB < 100) {
         LOG.debug("Writing " + testcase);
         final List<TraceElement> shortTrace = new CalledMethodLoader(kiekerResultFolder, mapping, new KiekerConfig()).getShortTrace("");

         writeTrace(newestVersion, testcase, shortTrace);
      } else {
         LOG.info("Not writing " + testcase + " since size is " + sizeInMB + " mb");
      }
   }

   private void writeTrace(final String newestVersion, final TestCase testcase, final List<TraceElement> shortTrace) throws IOException {
      ResultsFolders results = new ResultsFolders(new File("results"), projectFolder.getName());

      String shortVersion = TraceWriter.getShortVersion(newestVersion);
      File methodDir = results.getViewMethodDir(newestVersion, testcase);

      final File methodExpandedTrace = new File(methodDir, shortVersion + OneTraceGenerator.METHOD_EXPANDED);
      Files.write(methodExpandedTrace.toPath(), shortTrace
            .stream()
            .map(value -> value.toString()).collect(Collectors.toList()));
   }
}
