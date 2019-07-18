package de.peass.reexecutions;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.all.RepoFolders;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "findmissing", description = "Finds missing executions of all projects and writes them to runfiles", mixinStandardHelpOptions = true)
public class FindMissingExecutions implements Callable<Void> {

   @Option(names = { "-data", "--data" }, description = "Path to datafolder")
   protected File data[];

   @Option(names = { "-useslurm", "---useslurm" }, description = "Whether to generate runfiles for slurm or for bash (default: slurm)")
   protected boolean useslurm = true;

   public static final String NAME = "reexecute-missing";

   private static final Logger LOG = LogManager.getLogger(FindMissingExecutions.class);

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException, JAXBException {
      CommandLine commandLine = new CommandLine(new FindMissingExecutions());
      commandLine.execute(args);

   }

   private void findMissing(final String project, File reexecuteFolder, RepoFolders folders) throws IOException, JsonParseException, JsonMappingException, JAXBException {
      final File dependencyfile = new File(folders.getDependencyFolder(), "deps_" + project + ".json");
      final Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyfile, Dependencies.class);
      VersionComparator.setDependencies(dependencies);

      final ExecutionData tests = folders.getExecutionData(project);

      final File dataFolder;
      if (data.length == 0) {
         dataFolder = new File(folders.getCleanDataFolder(), project);
      } else {
         dataFolder = new File(data[0], project);
      }
      MissingExecutionFinder missingExecutionFinder = new MissingExecutionFinder(project, reexecuteFolder, tests, NAME);
      missingExecutionFinder.findMissing(dataFolder);
   }

   @Override
   public Void call() throws Exception {
      final RepoFolders folders = new RepoFolders();
      File reexecuteFolder = new File(folders.getResultsFolder(), NAME);
      reexecuteFolder.mkdirs();

      for (final String project : new String[] {  "commons-fileupload", "commons-csv" }) {
         LOG.info("Searching: {}", project);
         findMissing(project, reexecuteFolder, folders);
      }
      return null;
   }

}
