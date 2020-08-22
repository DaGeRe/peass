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

      final File[] dataFolder = getDataFolder(project, folders);
      MissingExecutionFinder missingExecutionFinder = new MissingExecutionFinder(project, reexecuteFolder, tests, NAME);
      missingExecutionFinder.findMissing(dataFolder);
   }

   private File[] getDataFolder(final String project, RepoFolders folders) {
      final File[] dataFolders;
      if (data.length == 0) {
         dataFolders = new File[1];
         dataFolders[0] = new File(folders.getCleanDataFolder(), project);
      } else {
         dataFolders = new File[data.length];
         for (int i = 0; i < data.length; i++) {
            File candidate = new File(data[i], project);
            if (candidate.exists()) {
               dataFolders[i] = new File(data[i], project);
            } else {
               dataFolders[i] = data[i];
            }
         }
      }
      return dataFolders;
   }

   @Override
   public Void call() throws Exception {
      final RepoFolders folders = new RepoFolders();
      File reexecuteFolder = new File(folders.getResultsFolder(), NAME);
      reexecuteFolder.mkdirs();

      for (final String project : new String[] { "commons-fileupload" }) {
         LOG.info("Searching: {}", project);
         findMissing(project, reexecuteFolder, folders);
      }
      return null;
   }

}
