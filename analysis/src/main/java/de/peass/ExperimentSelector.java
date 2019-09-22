package de.peass;

import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.Callable;

import de.peass.analysis.all.RepoFolders;
import de.peass.analysis.changes.Change;
import de.peass.analysis.changes.Changes;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.analysis.properties.ChangeProperty;
import de.peass.analysis.properties.VersionChangeProperties;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.utils.Constants;
import de.peass.utils.RunCommandWriter;
import de.peass.utils.RunCommandWriterSearchCause;
import de.peass.utils.RunCommandWriterSlurm;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Selects performance measurement experiments", name = "experimentselector")
public class ExperimentSelector implements Callable<Integer> {

   @Option(names = { "-properties", "--properties" }, description = "Propertyfile for selection of experiments", required = true)
   protected File propertyFile;

   @Option(names = { "-changes", "--changes" }, description = "Changefile for selection of experiments", required = true)
   protected File changesFile;

   public static void main(final String[] args) {
      final ExperimentSelector command = new ExperimentSelector();
      final CommandLine commandLine = new CommandLine(command);
      commandLine.execute(args);
   }

   public ExperimentSelector() {

   }

   int all;
   int noTestchange;
   int measurementChange;
   int measurementAll;

   int versionIndex = 0;

   @Override
   public Integer call() throws Exception {
      final RepoFolders repoFolders = new RepoFolders();

      final VersionChangeProperties properties = Constants.OBJECTMAPPER.readValue(propertyFile, VersionChangeProperties.class);
      final ProjectChanges changes = Constants.OBJECTMAPPER.readValue(changesFile, ProjectChanges.class);
      final String projectName = changesFile.getName().replace(".json", "");

      final File fileSlurm = new File(repoFolders.getRCAScriptFolder(), "rca-slurm-" + projectName + ".sh");
      final File fileJava = new File(repoFolders.getRCAScriptFolder(), "rca-java-" + projectName + ".sh");

      final ExecutionData executionData = repoFolders.getExecutionData(projectName);
      final RunCommandWriter writerSlurm = new RunCommandWriterSlurm(new PrintStream(fileSlurm), "cause1", executionData, RunCommandWriterSlurm.EXECUTE_RCA);
      final RunCommandWriter writer = new RunCommandWriterSearchCause(new PrintStream(fileJava), "cause1", executionData);

      properties.executeProcessor((version, testcase, change, changeProperties) -> {
         all++;
      });

      changes.executeProcessor((version, testcase, change) -> {
         measurementAll++;
      });

      writeExecutions(properties, changes, writerSlurm, writer);
      System.out.println("All: " + executionData.getAllExecutions());
      System.out.println("All: " + all + " No testchange: " + noTestchange + " Measurement change: " + measurementChange + " All: " + measurementAll);
      return 0;
   }

   boolean printOnlyChanged = true;

   private void writeExecutions(final VersionChangeProperties properties, final ProjectChanges changes, final RunCommandWriter writerSlurm, final RunCommandWriter writer) {
      properties.executeProcessor((version, testcase, change, changeProperties) -> {
         if (!change.isAffectsTestSource()) {
            if (printOnlyChanged) {
               Change statChange = null;
               final Changes versionChanges = changes.getVersionChanges().get(version);
               if (versionChanges != null) {
                  final List<Change> testcaseChanges = versionChanges.getTestcaseChanges().get(testcase);
                  if (testcaseChanges != null) {
                     statChange = findStatChange(change, testcaseChanges);

                  }
               }
               if (statChange != null) {
                  printExecuteCommand(writerSlurm, writer, version, testcase, change);
               }
            } else {
               printExecuteCommand(writerSlurm, writer, version, testcase, change);
            }

            noTestchange++;
         }
      });
   }

   private void printExecuteCommand(final RunCommandWriter writerSlurm, final RunCommandWriter writer, final String version, final String testcase, final ChangeProperty change) {
      versionIndex++;
      System.out.println("Possible experiment: " + version + " " + testcase + "#" + change.getMethod());
      measurementChange++;
      writer.createSingleMethodCommand(versionIndex, version, testcase + "#" + change.getMethod());
      writerSlurm.createSingleMethodCommand(versionIndex, version, testcase + "#" + change.getMethod());
   }

   private Change findStatChange(final ChangeProperty change, final List<Change> testcaseChanges) {
      Change statChange = null;
      for (final Change changeStat : testcaseChanges) {
         if (changeStat.getMethod().equals(change.getMethod())) {
            statChange = changeStat;
         }
      }
      return statChange;
   }
}
