package de.peass.dependency.traces;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.config.ExecutionConfig;
import de.peass.dependency.KiekerResultManager;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.CalledMethodLoader;
import de.peass.dependency.analysis.ModuleClassMapping;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.analysis.data.TraceElement;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependency.execution.ExecutionConfigMixin;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.Version;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.utils.Constants;
import de.peass.vcs.GitUtils;
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

   @Option(names = { "-dependencyfile", "--dependencyfile" }, description = "Path to the dependencyfile")
   protected File dependencyFile;

   public static void main(final String[] args) {
      final CommandLine commandLine = new CommandLine(new TraceGeneratorStarter());
      commandLine.execute(args);
   }

   @Override
   public Void call() throws Exception {
      Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
      String newestVersion = dependencies.getNewestVersion();

      Version version = dependencies.getVersions().get(newestVersion);
      TestSet tests = version.getTests();

      GitUtils.reset(projectFolder);
      PeASSFolders folders = new PeASSFolders(projectFolder);
      
      KiekerResultManager resultsManager = runTests(newestVersion, tests, folders);

      for (TestCase testcase : tests.getTests()) {
         writeTestcase(newestVersion, folders, resultsManager, testcase);
      }

      return null;
   }

   private KiekerResultManager runTests(final String newestVersion, final TestSet tests, final PeASSFolders folders) throws IOException, XmlPullParserException, InterruptedException {
      ExecutionConfig executionConfig = new ExecutionConfig(executionMixin);

      KiekerResultManager resultsManager = new KiekerResultManager(folders, executionConfig, new EnvironmentVariables());
      resultsManager.executeKoPeMeKiekerRun(tests, newestVersion);
      return resultsManager;
   }

   private void writeTestcase(final String newestVersion, final PeASSFolders folders, final KiekerResultManager resultsManager, final TestCase testcase)
         throws FileNotFoundException, IOException, XmlPullParserException, ViewNotFoundException {
      final File moduleResultFolder = KiekerFolderUtil.getModuleResultFolder(folders, testcase);
      final File kiekerResultFolder = KiekerFolderUtil.getClazzMethodFolder(testcase, moduleResultFolder);

      final long size = FileUtils.sizeOfDirectory(kiekerResultFolder);
      final long sizeInMB = size / (1024 * 1024);

      if (sizeInMB < CalledMethodLoader.TRACE_MAX_SIZE_IN_MB) {
         LOG.debug("Writing " + testcase);
         final ModuleClassMapping mapping = new ModuleClassMapping(folders.getProjectFolder(), resultsManager.getExecutor().getModules());
         final List<TraceElement> shortTrace = new CalledMethodLoader(kiekerResultFolder, mapping).getShortTrace("");

         writeTrace(newestVersion, testcase, shortTrace);
      } else {
         LOG.info("Not writing " + testcase + " since size is " + sizeInMB + " mb");
      }
   }

   private void writeTrace(final String newestVersion, final TestCase testcase, final List<TraceElement> shortTrace) throws IOException {
      File viewFolder = new File("results", "views_" + projectFolder.getName());
      viewFolder.mkdirs();
      TraceWriter tw = new TraceWriter(newestVersion, testcase, viewFolder);

      String shortVersion = TraceWriter.getShortVersion(newestVersion);
      File methodDir = tw.getMethodDir();

      final File methodExpandedTrace = new File(methodDir, shortVersion + OneTraceGenerator.METHOD_EXPANDED);
      Files.write(methodExpandedTrace.toPath(), shortTrace
            .stream()
            .map(value -> value.toString()).collect(Collectors.toList()));
   }
}
