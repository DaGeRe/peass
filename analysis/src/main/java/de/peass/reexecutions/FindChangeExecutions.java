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

/**
 * If a measurement is disturbed, e.g. by operation system tasks, it is likely to identify two executions as changed. Therefore, it is safer to re-execute the same measurements
 * later. This class generates the runscript.
 * 
 * @author reichelt
 *
 */
public class FindChangeExecutions {

   private static final String NAME = "reexecute-change";

   public static void main(String[] args) throws JAXBException, JsonParseException, JsonMappingException, IOException {
      final RepoFolders folders = new RepoFolders(args);
      File reexecuteFolder = new File(folders.getResultsFolder(), NAME);
      reexecuteFolder.mkdirs();

      for (final String project : new String[] { "commons-compress", "commons-csv", "commons-dbcp", "commons-fileupload", "commons-jcs",
            "commons-imaging", "commons-io", "commons-numbers", "commons-pool", "commons-text" }) {
         File projectChangeFile = new File(folders.getResultsFolder(), project + File.separator + project + ".json");
         File dependencyFile = new File(folders.getDependencyFolder(), "deps_" + project + ".json");
         if (projectChangeFile.exists() && dependencyFile.exists()) {
            findProjectExecutions(reexecuteFolder, project, projectChangeFile, dependencyFile);
         }
      }
   }

   public static void findProjectExecutions(File reexecuteFolder, final String project, File projectChangeFile, File dependencyFile)
         throws FileNotFoundException, JAXBException, IOException, JsonParseException, JsonMappingException {
      System.out.println("Reading: " + project);
      File reexecuteProject = new File(reexecuteFolder, project + ".sh");
      PrintStream goal = new PrintStream(new FileOutputStream(reexecuteProject));
      final Dependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
      RunCommandWriter writer = new RunCommandWriter(goal, true, NAME, dependencies);

      writeExecutions(projectChangeFile, dependencies, writer);
   }

   public static void writeExecutions(File projectChangeFile, final Dependencies dependencies, RunCommandWriter writer)
         throws IOException, JsonParseException, JsonMappingException {
      List<String> versions = Arrays.asList(dependencies.getVersionNames());
      final ProjectChanges changes = Constants.OBJECTMAPPER.readValue(projectChangeFile, ProjectChanges.class);
      changes.executeProcessor((version, testclazz, change) -> {
         int versionIndex = versions.indexOf(version);
         writer.createSingleMethodCommand(versionIndex, version, testclazz + "#" + change.getMethod());
      });
   }
}
