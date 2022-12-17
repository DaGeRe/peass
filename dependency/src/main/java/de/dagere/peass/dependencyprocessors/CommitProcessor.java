package de.dagere.peass.dependencyprocessors;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.config.parameters.ExecutionConfigMixin;
import de.dagere.peass.config.parameters.FixedCommitMixin;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.InitialCommit;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionControlSystem;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * Basic class for all classes that operate somehow on an folder and it's static selection file.
 * 
 * @author reichelt
 *
 */
public abstract class CommitProcessor implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(CommitProcessor.class);

   @Mixin
   protected ExecutionConfigMixin executionMixin;
   
   @Mixin
   protected FixedCommitMixin fixedCommitMixin;

   @Option(names = { "-folder", "--folder" }, description = "Folder of the project that should be analyzed", required = true)
   protected File projectFolder;

   protected PeassFolders folders;
   protected VersionControlSystem vcs;
   protected StaticTestSelection staticTestSelection;
   protected ExecutionData executionData;

   protected String startcommit;
   protected String endcommit;
   protected String commit;

   @Option(names = { "-threads", "--threads" }, description = "Number of parallel threads for analysis")
   protected int threads = 1;

   @Option(names = { "-staticSelectionFile", "--staticSelectionFile" }, description = "Path to the staticSelectionFile")
   protected File staticSelectionFile;

   /**
    * The file defining the selected tests, i.e. a traceTestSelection or coverageSelection file. Peass CLI options should be camel case, therefore the not camel case variant is deprecated and will be removed.
    */
   @Option(names = { "-executionFile", "--executionFile" }, description = "Path to the executionfile (may be trace based selection or coverage selection file)")
   protected File executionFile;

   public CommitProcessor(final File projectFolder, final StaticTestSelection dependencies) {
      this.folders = new PeassFolders(projectFolder);
      this.staticTestSelection = dependencies;
      startcommit = null;
      endcommit = null;
      threads = 1;
   }

   public void setStartversion(final String startversion) {
      this.startcommit = startversion;
   }

   public void setEndversion(final String endversion) {
      this.endcommit = endversion;
   }

   public CommitProcessor() {
      if (executionMixin != null) {
         startcommit = executionMixin.getStartcommit();
         endcommit = executionMixin.getEndcommit();
         commit = fixedCommitMixin.getCommit();
      }
   }

   public void processCommandline() {
      LOG.debug("Processing initial");
      processInitialCommit(staticTestSelection.getInitialcommit());

      if (threads != 1) {
         throw new RuntimeException("Parallel processing is not possible or implemented; do not set threads!");
      } else {
         for (final Map.Entry<String, CommitStaticSelection> version : staticTestSelection.getCommits().entrySet()) {
            LOG.debug("Processing {}", version.getKey());
            processCommit(version.getKey(), version.getValue());
         }
      }
      postEvaluate();
   }

   protected void processInitialCommit(final InitialCommit version) {

   }

   protected abstract void processCommit(String key, CommitStaticSelection version);

   protected void postEvaluate() {

   }

   @Override
   public Void call() throws Exception {
      initCommitProcessor();
      return null;
   }

   protected void initCommitProcessor() throws IOException, JsonParseException, JsonMappingException {
      if (executionMixin != null) {
         startcommit = executionMixin.getStartcommit();
         endcommit = executionMixin.getEndcommit();
         commit = fixedCommitMixin.getCommit();
      }

      if (staticSelectionFile != null) {
         staticTestSelection = Constants.OBJECTMAPPER.readValue(staticSelectionFile, StaticTestSelection.class);
         VersionComparator.setDependencies(staticTestSelection);
         executionData = new ExecutionData(staticTestSelection);
      }
      if (executionFile != null) {
         executionData = Constants.OBJECTMAPPER.readValue(executionFile, ExecutionData.class);
         staticTestSelection = new StaticTestSelection(executionData);
      }
      if (executionData == null && staticTestSelection == null) {
         throw new RuntimeException("Static test selection file and executionfile not readable - one needs to be defined!");
      }

      if (!projectFolder.exists()) {
         GitUtils.downloadProject(staticTestSelection.getUrl(), projectFolder);
      }
      folders = new PeassFolders(projectFolder);

      if (startcommit != null || endcommit != null) {
         LOG.info("Commit: {} - {}", startcommit, endcommit);
      }

      if (fixedCommitMixin.getCommitOld() != null && startcommit == null) {
         throw new RuntimeException("If commitOld is specified, always specify commit!");
      }

      if (commit != null) {
         if (startcommit != null || endcommit != null) {
            throw new RuntimeException("Both, commit and (startcommit or endcommit), are defined - define either commit or startcommit/endcommit!");
         }
         startcommit = commit;
         endcommit = commit;
         LOG.info("Commit: {} - {}", startcommit, endcommit);
      }

      VersionComparator.setDependencies(staticTestSelection);
      vcs = VersionControlSystem.getVersionControlSystem(folders.getProjectFolder());
   }
}
