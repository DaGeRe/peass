package de.dagere.peass.measurement.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.parameters.ExecutionConfigMixin;
import de.dagere.peass.config.parameters.KiekerConfigMixin;
import de.dagere.peass.config.parameters.MeasurementConfigurationMixin;
import de.dagere.peass.config.parameters.StatisticsConfigMixin;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * Creates a script for running a set of tests based on a dependencyfile (and optionally an executionfile) in order to start test executions.
 * 
 * @author reichelt
 *
 */
@Command(description = "Creates a script (bash or slurm) to run a set of tests", name = "createScript")
public class CreateScriptStarter implements Callable<Void> {

   @Option(names = { "-experimentId", "--experimentId" }, description = "Id of the experiment")
   protected String experimentId = "default";
   
   @Option(names = { "-dependencyfile", "--dependencyfile", "-dependencyFile", "--dependencyFile"  }, description = "Path to the dependencyfile")
   protected File dependencyFile;

   @Option(names = { "-executionfile", "--executionfile", "-executionFile", "--executionFile" }, description = "Path to the executionfile")
   protected File executionfile;
   
   @Option(names = { "-useSlurm", "--useSlurm" }, description = "Use slurm (if not specified, a bash script is created)")
   protected Boolean useSlurm = false;
   
   @Mixin
   MeasurementConfigurationMixin measurementConfigMixin;
   
   @Mixin
   ExecutionConfigMixin executionConfigMixin;

   private StaticTestSelection dependencies;
   private ExecutionData executionData;
   
   public static void main(final String[] args) throws  JsonParseException, JsonMappingException, IOException {
      final CreateScriptStarter command = new CreateScriptStarter();
      final CommandLine commandLine = new CommandLine(command);
      commandLine.execute(args);
   }
   
   @Override
   public Void call() throws Exception {
      if (dependencyFile != null) {
         dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, StaticTestSelection.class);
      }
      if (executionfile != null) {
         executionData = Constants.OBJECTMAPPER.readValue(executionfile, ExecutionData.class);
         dependencies = new StaticTestSelection(executionData);
      }
      if (executionData == null && dependencies == null) {
         throw new RuntimeException("Dependencyfile and executionfile not readable - one needs to be defined!");
      }

      MeasurementConfig config = new MeasurementConfig(measurementConfigMixin, executionConfigMixin, new StatisticsConfigMixin(), new KiekerConfigMixin());
      
      PrintStream destination = System.out;
      RunCommandWriter writer;
      if (useSlurm) {
         destination.println("timestamp=$(date +%s)");
         writer = new RunCommandWriterSlurm(config, System.out, experimentId, dependencies);
      } else {
         writer = new RunCommandWriter(config, destination, experimentId, dependencies);
      }

      generateExecuteCommands(dependencies, executionData, experimentId, writer);
      
      return null;
   }

   public static void generateExecuteCommands(final StaticTestSelection dependencies, final ExecutionData changedTests, final String experimentId, final PrintStream goal) throws IOException {
      generateExecuteCommands(dependencies, changedTests, experimentId, new RunCommandWriterSlurm(new MeasurementConfig(30), goal, experimentId, dependencies));
   }

   public static void generateExecuteCommands(final ExecutionData changedTests, final String experimentId, final PrintStream goal) throws IOException {
      generateExecuteCommands(changedTests, experimentId, new RunCommandWriterSlurm(new MeasurementConfig(30), goal, experimentId, changedTests));
   }

   public static void generateExecuteCommands(final ExecutionData changedTests, final String experimentId, final RunCommandWriter writer)
         throws IOException {
      int i = 0;
      for (Map.Entry<String, TestSet> entry : changedTests.getCommits().entrySet()) {
         for (final Map.Entry<TestCase, Set<String>> testcase : entry.getValue().getTestcases().entrySet()) {
            for (final String method : testcase.getValue()) {
               final String testcaseName = testcase.getKey().getClazz() + "#" + method;
               writer.createSingleMethodCommand(i, entry.getKey(), testcaseName);

            }
         }
         i++;
      }
   }

   public static void generateExecuteCommands(final StaticTestSelection dependencies, final ExecutionData changedTests, final String experimentId, final RunCommandWriter writer)
         throws IOException {
      final String[] versions = dependencies.getCommitNames();
      for (int versionIndex = 0; versionIndex < versions.length; versionIndex++) {
         final String endversion = versions[versionIndex];
         // System.out.println("-startversion " + startversion + " -endversion " + endversion);
         if (changedTests == null) {
            final Set<TestCase> tests = dependencies.getCommits().get(endversion).getTests().getTests();
            writer.createFullVersionCommand(versionIndex, endversion, tests);
         } else if (changedTests != null && changedTests.getCommits().containsKey(endversion)) {
            for (final Map.Entry<TestCase, Set<String>> testcase : changedTests.getCommits().get(endversion).getTestcases().entrySet()) {
               for (final String method : testcase.getValue()) {
                  final String testcaseName = testcase.getKey().getClazz() + "#" + method;
                  writer.createSingleMethodCommand(versionIndex, endversion, testcaseName);

               }
            }
         }
      }
   }
}
