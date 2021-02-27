package de.peass;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.properties.PropertyReader;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.parallel.PartialDependenciesMerger;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.reader.DependencyParallelReader;
import de.peass.dependency.traces.ViewGenerator;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.vcs.GitCommit;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

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

   @Mixin
   private DependencyReaderConfig config;

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
      final String project = config.getProjectFolder().getName();
      
      final List<GitCommit> commits = DependencyReadingStarter.getGitCommits(config.getStartversion(), config.getEndversion(), config.getProjectFolder());
      VersionComparator.setVersions(commits);
      
      readExecutions(project, commits);
      return null;
   }

   public void readExecutions(final String project, final List<GitCommit> commits) throws InterruptedException, IOException, JsonGenerationException, JsonMappingException, JAXBException {
      final DependencyParallelReader reader = new DependencyParallelReader(config.getProjectFolder(), config.getResultBaseFolder(), project, commits, 
            config.getThreads(), config.getTimeout(), config.getTestGoal());
      final File[] outFiles = reader.readDependencies();

      LOG.debug("Files: {}", outFiles);

      final File out = new File(config.getResultBaseFolder(), "deps_" + project + ".json");
      final Dependencies all = PartialDependenciesMerger.mergeVersions(out, outFiles);

      final PeASSFolders folders = new PeASSFolders(config.getProjectFolder());
      final File dependencyTempFiles = new File(folders.getTempProjectFolder().getParentFile(), "dependencyTempFiles");
      folders.getTempProjectFolder().renameTo(dependencyTempFiles);

      final File executeOut = new File(config.getResultBaseFolder(), "execute_" + project + ".json");
      final File viewFolder = new File(config.getResultBaseFolder(), "views_" + project);

      final ViewGenerator viewGenerator = new ViewGenerator(config.getProjectFolder(), all, executeOut, viewFolder, config.getThreads(), config.getTimeout(), config.getTestGoal());
      viewGenerator.processCommandline();
      
      final File propertyFolders = new File(config.getResultBaseFolder(), "properties_" + project);
      final PropertyReader propertyReader = new PropertyReader(propertyFolders, config.getProjectFolder(), viewFolder);
      propertyReader.readAllTestsProperties(viewGenerator.getChangedTraceMethods());
   }

   
}
