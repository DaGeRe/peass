package de.peass.reexecutions;

import java.io.File;
import java.io.FileNotFoundException;
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

   public static final String NAME = "reexecute-missing";

   private static final Logger LOG = LogManager.getLogger(FindMissingExecutions.class);

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException, JAXBException {
      final RepoFolders folders = new RepoFolders();
      File reexecuteFolder = new File(folders.getResultsFolder(), NAME);
      reexecuteFolder.mkdirs();

      for (final String project : new String[] { "commons-compress", "commons-csv", "commons-dbcp", "commons-fileupload", "commons-jcs",
            "commons-imaging", "commons-io", "commons-numbers", "commons-pool", "commons-text" }) {
         LOG.info("Searching: {}", project);
         findMissing(project, reexecuteFolder, folders);
      }

   }

   static void findMissing(final String project, File reexecuteFolder, RepoFolders folders) throws IOException, JsonParseException, JsonMappingException, JAXBException {
      final File dependencyfile = new File(folders.getDependencyFolder(), "deps_" + project + ".json");
      final Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyfile, Dependencies.class);
      VersionComparator.setDependencies(dependencies);

      final ExecutionData tests = folders.getExecutionData(project);

      final File dataFolder = new File(folders.getCleanDataFolder(), project);
      MissingExecutionFinder missingExecutionFinder = new MissingExecutionFinder(project, reexecuteFolder, tests, NAME);
      missingExecutionFinder.findMissing(dataFolder);
   }
   
}
