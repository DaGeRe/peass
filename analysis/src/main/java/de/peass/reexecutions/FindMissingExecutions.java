package de.peass.reexecutions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.analysis.all.RepoFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependency.reader.DependencyReader;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.Constants;
import de.peass.utils.DivideVersions;

public class FindMissingExecutions {
   
   private static final String NAME = "reexecute-missing";
   
   private static final Logger LOG = LogManager.getLogger(FindMissingExecutions.class);
   
   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException, JAXBException {
      final RepoFolders folders = new RepoFolders(args);
      File reexecuteFolder = new File(folders.getResultsFolder(), NAME);
      reexecuteFolder.mkdirs();
      
      for (final String project : new String[] { "commons-compress", "commons-csv", "commons-dbcp", "commons-fileupload", "commons-jcs",
            "commons-imaging", "commons-io", "commons-numbers", "commons-pool", "commons-text" }) {
         findMissing(project, reexecuteFolder, folders);
      }

      
   }

   static void findMissing(final String project, File reexecuteFolder, RepoFolders folders) throws IOException, JsonParseException, JsonMappingException, JAXBException {
      final File dependencyfile = new File(folders.getDependencyFolder(), "deps_" + project + ".json");
      final Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyfile, Dependencies.class);
      VersionComparator.setDependencies(dependencies);

      final File executefile = new File(folders.getDependencyFolder(), "execute_" + project + ".json");
      final ExecutionData tests = Constants.OBJECTMAPPER.readValue(executefile, ExecutionData.class);

      final File folder = new File(folders.getDataFolder(), project);
      if (folder.exists()) {
         int countAll = allExecutions(tests);

         System.out.println(folder.getAbsolutePath());

         removeMissingExecutions(tests, folder);

         System.out.println();
         System.out.println("Missing Tests");

         int countNotExecuted = allExecutions(tests);
         System.out.println("Not executed: " + countNotExecuted + " All defined executions: " + countAll);

         final PrintStream outputStream = new PrintStream(new FileOutputStream(new File(reexecuteFolder, "slurm-" + project + ".sh")));
         DivideVersions.generateExecuteCommands(dependencies, tests, NAME, new File("execute-" + project + ".sh"), outputStream);
         outputStream.flush();
         outputStream.close();
      }
   }

   public static void removeMissingExecutions(final ExecutionData tests, final File folder) throws JAXBException {
      for (final File measurementFile : folder.listFiles()) {
         if (measurementFile.getName().endsWith(".xml")) {
            System.out.println("File:" + measurementFile);
            final Kopemedata data = new XMLDataLoader(measurementFile).getFullData();
            for (final TestcaseType testcase : data.getTestcases().getTestcase()) {
               final String testmethod = testcase.getName();
               for (final Chunk c : testcase.getDatacollector().get(0).getChunk()) {
                  final String version = findVersion(c);
                  final TestSet testSet = tests.getVersions().get(version);
                  if (testSet != null) {
                     removeTestFromTestSet(data, testmethod, testSet);
                  }
               }
            }
         }
      }
   }

   public static void removeTestFromTestSet(final Kopemedata data, final String testmethod, final TestSet testSet) {
      LOG.trace(testSet.classCount());
      final ChangedEntity ce = new ChangedEntity(data.getTestcases().getClazz(), "");
      testSet.removeTest(ce, testmethod);
      LOG.trace(testSet.classCount());
   }

   public static String findVersion(final Chunk c) {
      final int size = c.getResult().size();
      final Result r = c.getResult().get(size - 1);
      final String version = r.getVersion().getGitversion();
      LOG.trace("Version: " + version);
      return version;
   }

   public static int allExecutions(final ExecutionData tests) {
      int count2 = 0;
      for (final Entry<String, TestSet> entry : tests.getVersions().entrySet()) {
         System.out.println(entry.getKey());
         System.out.println(entry.getValue());
         count2 += entry.getValue().getTests().size();
      }
      return count2;
   }
}
