package de.dagere.peass;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.analysis.all.RepoFolders;
import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.analysis.properties.ChangeProperty;
import de.dagere.peass.analysis.properties.VersionChangeProperties;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.measurement.utils.RunCommandWriter;
import de.dagere.peass.measurement.utils.RunCommandWriterRCA;
import de.dagere.peass.measurement.utils.RunCommandWriterSlurm;
import de.dagere.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Selects performance measurement experiments", name = "experimentselector")
public class ExperimentSelector implements Callable<Integer> {
   
   private static final Logger LOG = LogManager.getLogger(ExperimentSelector.class);

   @Option(names = { "-properties", "--properties" }, description = "Propertyfile for selection of experiments", required = false)
   protected File propertyFile;

   @Option(names = { "-changes", "--changes" }, description = "Changefile for selection of experiments", required = false)
   protected File changesFile;

   public static void main(final String[] args) {
      final ExperimentSelector command = new ExperimentSelector();
      final CommandLine commandLine = new CommandLine(command);
      commandLine.execute(args);
   }

   public ExperimentSelector() {

   }

   private int all;
   private int noTestchange;
   private int measurementChange;
   private int measurementAll;

   private int versionIndex = 0;

   private boolean printOnlyChanged = true;

   @Override
   public Integer call() throws Exception {

      if (propertyFile == null) {
         final RepoFolders repoFolders = new RepoFolders();
         final File propertiesFolder = new File(repoFolders.getPropertiesFolder(), "properties");
         for (final File projectFile : propertiesFolder.listFiles()) {
            LOG.info("Searching in {}", projectFile);
            final File currentPropertyFile = projectFile.listFiles((FilenameFilter) new WildcardFileFilter("*.json"))[0];
            selectForProject(currentPropertyFile, null);
         }
      } else {
         selectForProject(propertyFile, changesFile);
      }

      return 0;
   }

   private void selectForProject(final File currentPropertyFile, final File currentChangeFile) throws IOException, JsonParseException, JsonMappingException, FileNotFoundException {
      final RepoFolders repoFolders = new RepoFolders();

      final VersionChangeProperties properties = Constants.OBJECTMAPPER.readValue(currentPropertyFile, VersionChangeProperties.class);
      final ProjectChanges changes;

      if (currentChangeFile != null && currentChangeFile.exists()) {
         changes = Constants.OBJECTMAPPER.readValue(currentChangeFile, ProjectChanges.class);
      } else {
         changes = null;
      }

      final String projectName = currentChangeFile != null ? currentChangeFile.getName().replace(".json", "") : currentPropertyFile.getParentFile().getName();

      final File fileSlurm = new File(repoFolders.getRCAScriptFolder(), "rca-slurm-" + projectName + ".sh");
      final File fileJava = new File(repoFolders.getRCAScriptFolder(), "rca-java-" + projectName + ".sh");

      final ExecutionData executionData = repoFolders.getExecutionData(projectName);
      final RunCommandWriter writerSlurm = new RunCommandWriterSlurm(new MeasurementConfig(30), new PrintStream(fileSlurm), "cause1", executionData, RunCommandWriterSlurm.EXECUTE_RCA);
      final RunCommandWriter writer = new RunCommandWriterRCA(new MeasurementConfig(30), new PrintStream(fileJava), "cause1", executionData);

      properties.executeProcessor((version, testcase, change, changeProperties) -> {
         all++;
      });

      if (changes != null) {
         changes.executeProcessor((version, testcase, change) -> {
            measurementAll++;
         });
      } else {
         printOnlyChanged = false;
      }

      writeExecutions(properties, changes, writerSlurm, writer);
      System.out.println("All: " + executionData.getAllExecutions());
      System.out.println("All: " + all + " No testchange: " + noTestchange + " Measurement change: " + measurementChange + " All: " + measurementAll);
   }

   private void writeExecutions(final VersionChangeProperties properties, final ProjectChanges changes, final RunCommandWriter writerSlurm, final RunCommandWriter writer) {
      properties.executeProcessor((version, testcase, change, changeProperties) -> {
         if (!change.isAffectsTestSource()) {
            if (printOnlyChanged) {
               final Change measuredChange = findMeasuredChange(changes, version, testcase, change);
               if (measuredChange != null) {
                  printExecuteCommand(writerSlurm, writer, version, testcase, change);
               }
            } else {
               printExecuteCommand(writerSlurm, writer, version, testcase, change);
            }

            noTestchange++;
         }
      });
   }

   private Change findMeasuredChange(final ProjectChanges changes, final String version, final String testcase, final ChangeProperty change) {
      Change statChange = null;
      final Changes versionChanges = changes.getCommitChanges().get(version);
      if (versionChanges != null) {
         final List<Change> testcaseChanges = versionChanges.getTestcaseChanges().get(testcase);
         if (testcaseChanges != null) {
            statChange = findStatChange(change, testcaseChanges);
         }
      }
      return statChange;
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
