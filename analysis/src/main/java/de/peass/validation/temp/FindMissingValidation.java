package de.peass.validation.temp;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.dependency.persistence.ExecutionData;
import de.peass.analysis.all.RepoFolders;
import de.peass.reexecutions.MissingExecutionFinder;
import de.peran.FolderSearcher;

public class FindMissingValidation {

   private static final Logger LOG = LogManager.getLogger(FindMissingValidation.class);

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException, JAXBException {
      final RepoFolders folders = new RepoFolders();

      File reexecuteFolder = new File("results/reexecute-validation");
      reexecuteFolder.mkdirs();

      for (String project : GetValidationExecutionFile.VALIDATION_PROJECTS) {
         LOG.info("Analyzing {}", project);
         File executionFile = GetValidationExecutionFile.getValidationExecutionFile(project);
         ExecutionData tests = FolderSearcher.MAPPER.readValue(executionFile, ExecutionData.class);

         MissingExecutionFinder missingExecutionFinder = new MissingExecutionFinder(project, reexecuteFolder, tests, "commentExec");
         missingExecutionFinder.findMissing(new File[] { folders.getValidationDataFolder(project) });
      }

   }
}
