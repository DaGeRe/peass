package de.dagere.peass.reexecutions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.analysis.all.RepoFolders;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.measurement.utils.RunCommandWriter;
import de.dagere.peass.measurement.utils.RunCommandWriterRCA;
import de.dagere.peass.utils.Constants;

/**
 * If a measurement is disturbed, e.g. by operation system tasks, it is likely to identify two executions as changed. Therefore, it is safer to re-execute the same measurements
 * later. This class generates the runscript.
 * 
 * @author reichelt
 *
 */
public class FindChangeExecutions {

   private static final String NAME = "reexecute-change";

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      final RepoFolders folders = new RepoFolders();
      final File reexecuteFolder = new File(folders.getResultsFolder(), NAME);
      reexecuteFolder.mkdirs();

//      for (final String project : new String[] { "commons-compress", "commons-csv", "commons-dbcp", "commons-fileupload", "commons-jcs",
//            "commons-imaging", "commons-io", "commons-numbers", "commons-pool", "commons-text" }) {
    for (final String project : new String[] { "commons-fileupload" }) {
         final File projectChangeFile = new File(folders.getResultsFolder(), project + File.separator + project + ".json");
         final File dependencyFile = new File(folders.getDependencyFolder(), ResultsFolders.TRACE_SELECTION_PREFIX + project + ".json");
         if (projectChangeFile.exists() && dependencyFile.exists()) {
            findProjectExecutions(reexecuteFolder, project, projectChangeFile, dependencyFile);
         }
      }
   }

   public static void findProjectExecutions(final File reexecuteFolder, final String project, final File projectChangeFile, final File executionFile)
         throws FileNotFoundException, IOException, JsonParseException, JsonMappingException {
      System.out.println("Reading: " + project);
      final File reexecuteProject = new File(reexecuteFolder, "reexecute-change-" + project + ".sh");
      final PrintStream goal = new PrintStream(new FileOutputStream(reexecuteProject));
      final ExecutionData executions = Constants.OBJECTMAPPER.readValue(executionFile, ExecutionData.class);
//      RunCommandWriter writer = new RunCommandWriterSlurm(goal, NAME, dependencies);
      final RunCommandWriter writer = new RunCommandWriterRCA(goal, NAME, executions);
      writer.setNice(1000);

      writeExecutions(projectChangeFile, executions, writer);
   }

   public static void writeExecutions(final File projectChangeFile, final ExecutionData executions, final RunCommandWriter writer)
         throws IOException, JsonParseException, JsonMappingException {
      final List<String> versions = new LinkedList<String>(executions.getVersions().keySet());
      final ProjectChanges changes = Constants.OBJECTMAPPER.readValue(projectChangeFile, ProjectChanges.class);
      changes.executeProcessor((version, testclazz, change) -> {
         final int versionIndex = versions.indexOf(version);
         if (versionIndex != -1) {
            writer.createSingleMethodCommand(versionIndex, version, testclazz + "#" + change.getMethod());
         }
      });
   }
}
