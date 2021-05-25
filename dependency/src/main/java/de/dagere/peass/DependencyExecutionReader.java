package de.dagere.peass;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.analysis.properties.PropertyReader;
import de.dagere.peass.config.DependencyReaderConfigMixin;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.ResultsFolders;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.parallel.PartialDependenciesMerger;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.reader.DependencyParallelReader;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.vcs.GitCommit;
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
   private DependencyReaderConfigMixin config;

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
      
      final List<GitCommit> commits = CommitUtil.getGitCommits(config.getStartversion(), config.getEndversion(), config.getProjectFolder());
      VersionComparator.setVersions(commits);
      
      readExecutions(project, commits);
      return null;
   }

   public void readExecutions(final String project, final List<GitCommit> commits) throws InterruptedException, IOException, JsonGenerationException, JsonMappingException, JAXBException {
      final DependencyParallelReader reader = new DependencyParallelReader(config.getProjectFolder(), config.getResultBaseFolder(), project, commits, 
            config.getDependencyConfig(), config.getExecutionConfig(), new EnvironmentVariables());
      final ResultsFolders[] outFiles = reader.readDependencies();

      LOG.debug("Files: {}", outFiles);

      ResultsFolders mergedFolders = new ResultsFolders(config.getResultBaseFolder(), project);
      
      final File out = mergedFolders.getDependencyFile();
      final Dependencies all = PartialDependenciesMerger.mergeVersions(out, outFiles);

      final PeASSFolders folders = new PeASSFolders(config.getProjectFolder());
      final File dependencyTempFiles = new File(folders.getTempProjectFolder().getParentFile(), "dependencyTempFiles");
      folders.getTempProjectFolder().renameTo(dependencyTempFiles);

      final File executionOut = mergedFolders.getExecutionFile();
      ExecutionData executionData = PartialDependenciesMerger.mergeExecutions(executionOut, outFiles);
      
      mergeViews(outFiles, mergedFolders);
      
      ResultsFolders resultsFolders = new ResultsFolders(config.getResultBaseFolder(), project);
      final PropertyReader propertyReader = new PropertyReader(resultsFolders, config.getProjectFolder(), executionData);
      propertyReader.readAllTestsProperties();
   }

   private void mergeViews(final ResultsFolders[] outFiles, final ResultsFolders mergedFolders) {
      for (ResultsFolders resultsFolders : outFiles) {
         for (File viewFolder : resultsFolders.getViewFolder().listFiles()) {
            File dest = new File(mergedFolders.getViewFolder(), viewFolder.getName());
            if (!dest.exists()) {
               viewFolder.renameTo(dest);
            }
         }
      }
   }
}
