package de.dagere.peass.measurement.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;

public class DivideAllVersions {

   private static final Logger LOG = LogManager.getLogger(DivideAllVersions.class);

   public static void main(final String[] args) throws JAXBException, JsonParseException, JsonMappingException, IOException {
      String repoName = System.getenv(Constants.PEASS_REPOS);
      File repos = new File(repoName);
      if (!repos.exists()) {
         LOG.error("Repofolder " + repos.getAbsolutePath() + " not found!");
      }

      final File resultFolder = new File("results");
      if (!resultFolder.exists()) {
         resultFolder.mkdirs();
      }

      File dependencyFolder = new File(repos, "dependencies-final");
      for (String project : Constants.defaultUrls.keySet()) {
         File dependencyFile = new File(dependencyFolder, ResultsFolders.STATIC_SELECTION_PREFIX + project + ".json");
         File executionFile = new File(dependencyFolder, ResultsFolders.TRACE_SELECTION_PREFIX + project + ".json");
         if (dependencyFile.exists() && executionFile.exists()) {
            LOG.debug("Loading: " + project);
            final File executeCommandsFile = new File(resultFolder, "execute-" + project + ".sh");
            final StaticTestSelection dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, StaticTestSelection.class);
            final ExecutionData changedTests = Constants.OBJECTMAPPER.readValue(executionFile, ExecutionData.class);
            CreateScriptStarter.generateExecuteCommands(dependencies, changedTests, "validation", new PrintStream(new FileOutputStream(executeCommandsFile)));
         } else {
            LOG.error("File not existing: " + project + " " + Constants.defaultUrls.get(project) + " " + dependencyFile.getAbsolutePath());
         }
      }

   }
}
