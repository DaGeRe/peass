package de.peass.reexecutions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.all.RepoFolders;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.dependency.persistence.Dependencies;
import de.peass.statistics.DependencyStatisticAnalyzer;
import de.peass.utils.Constants;
import de.peass.utils.RunCommandWriter;
import de.peass.utils.RunCommandWriterSearchCause;

/**
 * If a measurement is disturbed, e.g. by operation system tasks, it is likely to identify two executions as changed. Therefore, it is safer to re-execute the same measurements
 * later. This class generates the runscript.
 * 
 * @author reichelt
 *
 */
public class FindChangeExecutions {

   private static final String NAME = "reexecute-change";

   public static void main(final String[] args) throws JAXBException, JsonParseException, JsonMappingException, IOException {
      final RepoFolders folders = new RepoFolders();
      final File reexecuteFolder = new File(folders.getResultsFolder(), NAME);
      reexecuteFolder.mkdirs();

//      for (final String project : new String[] { "commons-compress", "commons-csv", "commons-dbcp", "commons-fileupload", "commons-jcs",
//            "commons-imaging", "commons-io", "commons-numbers", "commons-pool", "commons-text" }) {
    for (final String project : new String[] { "commons-fileupload" }) {
         final File projectChangeFile = new File(folders.getResultsFolder(), project + File.separator + project + ".json");
         final File dependencyFile = new File(folders.getDependencyFolder(), "deps_" + project + ".json");
         if (projectChangeFile.exists() && dependencyFile.exists()) {
            findProjectExecutions(reexecuteFolder, project, projectChangeFile, dependencyFile);
         }
      }
   }

   public static void findProjectExecutions(final File reexecuteFolder, final String project, final File projectChangeFile, final File dependencyFile)
         throws FileNotFoundException, JAXBException, IOException, JsonParseException, JsonMappingException {
      System.out.println("Reading: " + project);
      final File reexecuteProject = new File(reexecuteFolder, "reexecute-change-" + project + ".sh");
      final PrintStream goal = new PrintStream(new FileOutputStream(reexecuteProject));
      final Dependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
//      RunCommandWriter writer = new RunCommandWriterSlurm(goal, NAME, dependencies);
      final RunCommandWriter writer = new RunCommandWriterSearchCause(goal, NAME, dependencies);
      writer.setNice(1000);

      writeExecutions(projectChangeFile, dependencies, writer);
   }

   public static void writeExecutions(final File projectChangeFile, final Dependencies dependencies, final RunCommandWriter writer)
         throws IOException, JsonParseException, JsonMappingException {
      final List<String> versions = Arrays.asList(dependencies.getVersionNames());
      final ProjectChanges changes = Constants.OBJECTMAPPER.readValue(projectChangeFile, ProjectChanges.class);
      changes.executeProcessor((version, testclazz, change) -> {
         final int versionIndex = versions.indexOf(version);
         writer.createSingleMethodCommand(versionIndex, version, testclazz + "#" + change.getMethod());
      });
   }
}
