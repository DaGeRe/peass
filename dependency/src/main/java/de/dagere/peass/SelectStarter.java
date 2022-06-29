package de.dagere.peass;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.analysis.properties.PropertyReader;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.config.parameters.ExecutionConfigMixin;
import de.dagere.peass.config.parameters.KiekerConfigMixin;
import de.dagere.peass.config.parameters.TestSelectionConfigMixin;
import de.dagere.peass.dependency.parallel.PartialDependenciesMerger;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.reader.DependencyParallelReader;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.vcs.CommitUtil;
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
@Command(description = "Executes the regression test selection. Creates the executionfile, which defines the tests-commit-pairs that need to be executed in each commit", name = "select")
public class SelectStarter implements Callable<Void>{

   private static final Logger LOG = LogManager.getLogger(SelectStarter.class);

   @Mixin
   private TestSelectionConfigMixin config;
   
   @Mixin
   private KiekerConfigMixin kiekerConfigMixin;
   
   @Mixin
   private ExecutionConfigMixin executionConfigMixin;

   public static void main(final String[] args) {
      try {
         final CommandLine commandLine = new CommandLine(new SelectStarter());
         commandLine.execute(args);
      } catch (final Throwable t) {
         t.printStackTrace();
      }
   }
   
   @Override
   public Void call() throws Exception {
      final String project = config.getProjectFolder().getName();
      
      final List<String> commits = CommitUtil.getGitCommits(executionConfigMixin.getStartcommit(), executionConfigMixin.getEndcommit(), config.getProjectFolder());
      VersionComparator.setVersions(commits);

      CommitComparatorInstance comparator = new CommitComparatorInstance(commits);
      readExecutions(project, comparator);
      return null;
   }

   public void readExecutions(final String project, final CommitComparatorInstance comparator) throws InterruptedException, IOException, JsonGenerationException, JsonMappingException {
      KiekerConfig kiekerConfig = kiekerConfigMixin.getKiekerConfig();
      ExecutionConfig executionConfig = executionConfigMixin.getExecutionConfig();
      
      final DependencyParallelReader reader = new DependencyParallelReader(config.getProjectFolder(), config.getResultBaseFolder(), project, comparator, 
            config.getDependencyConfig(), executionConfig, kiekerConfig, new EnvironmentVariables(executionConfig.getProperties()));
      final ResultsFolders[] outFiles = reader.readDependencies();

      LOG.debug("Files: {}", outFiles);

      ResultsFolders mergedFolders = new ResultsFolders(config.getResultBaseFolder(), project);
      
      final File out = mergedFolders.getStaticTestSelectionFile();
      PartialDependenciesMerger.mergeVersions(out, outFiles, comparator);

      final PeassFolders folders = new PeassFolders(config.getProjectFolder());
      final File dependencyTempFiles = new File(folders.getTempProjectFolder().getParentFile(), "dependencyTempFiles");
      FileUtils.moveDirectory(folders.getTempProjectFolder(), dependencyTempFiles);

      ExecutionData executionData = PartialDependenciesMerger.mergeExecutions(mergedFolders, outFiles);
      
      mergeViews(outFiles, mergedFolders);
      
      if (!config.isDoNotGenerateProperties()) {
         ResultsFolders resultsFolders = new ResultsFolders(config.getResultBaseFolder(), project);
         PeassFolders propertyFolder = folders.getTempFolder("propertyReadFolder");
         final PropertyReader propertyReader = new PropertyReader(resultsFolders, propertyFolder.getProjectFolder(), executionData, executionConfig);
         propertyReader.readAllTestsProperties();
         FileUtils.forceDelete(propertyFolder.getProjectFolder());
      }
   }

   private void mergeViews(final ResultsFolders[] outFiles, final ResultsFolders mergedFolders) throws IOException {
      for (ResultsFolders resultsFolders : outFiles) {
         for (File viewFolder : resultsFolders.getViewFolder().listFiles()) {
            File dest = new File(mergedFolders.getViewFolder(), viewFolder.getName());
            if (!dest.exists()) {
               FileUtils.moveDirectory(viewFolder, dest);
            }
         }
      }
   }
}
