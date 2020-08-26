package de.peass;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.properties.PropertyReader;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.parallel.Merger;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.traces.ViewGenerator;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.vcs.GitCommit;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * First reads all dependencies and afterwards determines the views and creates the execution file. Both is parallelized. This is the class that should be used if a project as a
 * whole should be analyzed.
 * 
 * @author reichelt
 *
 */
@Command(description = "Executes the regression test selection. Creates the executionfile, which defines the tests-version-pairs that need to be executed in each version", name = "select")
public class DependencyExecutionReader implements Callable<Void>{

   private static final Logger LOG = LogManager.getLogger(DependencyExecutionReader.class);

   @Option(names = {"-folder", "--folder"}, description = "Folder that should be analyzed", required = true)
   private File projectFolder;

   @Option(names = {"-out","--out"}, description = "Folder for results")
   private File resultBaseFolder = new File("results");

   @Option(names = {"-timeout", "--timeout"}, description = "Timeout for each VM start")
   private int timeout = 5;

   @Option(names = {"-threads", "--threads"}, description = "Number of parallel threads for analysis")
   private int threads = 4;

   @Option(names = {"-startversion", "--startversion"}, description = "First version that should be analysed")
   private String startversion;

   @Option(names = {"-endversion", "--endversion"}, description = "Last version that should be analysed")
   private String endversion;

   public static void main(final String[] args) {
      try {
         final CommandLine commandLine = new CommandLine(new DependencyExecutionReader());
         commandLine.execute(args);
      } catch (final Throwable t) {
         t.printStackTrace();
      }
   }
   
   @Override
   public Void call() throws Exception {
      final String project = projectFolder.getName();
      
      final List<GitCommit> commits = DependencyReadingStarter.getGitCommits(startversion, endversion, projectFolder);
      VersionComparator.setVersions(commits);
      
      readExecutions(project, commits);
      return null;
   }

   public void readExecutions(final String project, final List<GitCommit> commits) throws InterruptedException, IOException, JsonGenerationException, JsonMappingException, ParseException, JAXBException {
      final DependencyParallelReader reader = new DependencyParallelReader(projectFolder, resultBaseFolder, project, commits, threads, timeout);
      final File[] outFiles = reader.readDependencies();

      LOG.debug("Files: {}", outFiles);

      final File out = new File(resultBaseFolder, "deps_" + project + ".json");
      final Dependencies all = Merger.mergeVersions(out, outFiles);

      final PeASSFolders folders = new PeASSFolders(projectFolder);
      final File dependencyTempFiles = new File(folders.getTempProjectFolder().getParentFile(), "dependencyTempFiles");
      folders.getTempProjectFolder().renameTo(dependencyTempFiles);

      final File executeOut = new File(resultBaseFolder, "execute_" + project + ".json");
      final File viewFolder = new File(resultBaseFolder, "views_" + project);

      final ViewGenerator viewGenerator = new ViewGenerator(projectFolder, all, executeOut, viewFolder, threads, timeout);
      viewGenerator.processCommandline();
      
      final File propertyFolders = new File(resultBaseFolder, "properties_" + project);
      final PropertyReader propertyReader = new PropertyReader(propertyFolders, projectFolder, viewFolder);
      propertyReader.readAllTestsProperties(viewGenerator.getChangedTraceMethods());
   }

   
}
