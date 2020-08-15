package de.peass.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.statistics.DependencyStatisticAnalyzer;

public class DivideAllVersions {

   private static final Logger LOG = LogManager.getLogger(DivideAllVersions.class);

   public static void main(String[] args) throws JAXBException, JsonParseException, JsonMappingException, IOException {
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
         File dependencyFile = new File(dependencyFolder, "deps_" + project + ".json");
         File executionFile = new File(dependencyFolder, "execute_" + project + ".json");
         if (dependencyFile.exists() && executionFile.exists()) {
            LOG.debug("Loading: " + project);
            final File executeCommandsFile = new File(resultFolder, "execute-" + project + ".sh");
            final Dependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
            final ExecutionData changedTests = Constants.OBJECTMAPPER.readValue(executionFile, ExecutionData.class);
            DivideVersions.generateExecuteCommands(dependencies, changedTests, "validation", new PrintStream(new FileOutputStream(executeCommandsFile)));
         } else {
            LOG.error("File not existing: " + project + " " + Constants.defaultUrls.get(project) + " " + dependencyFile.getAbsolutePath());
         }
      }

   }
}
