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
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.InitialVersion;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
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
public abstract class VersionProcessor implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(VersionProcessor.class);

   @Mixin
   protected ExecutionConfigMixin executionMixin;

   @Option(names = { "-folder", "--folder" }, description = "Folder of the project that should be analyzed", required = true)
   protected File projectFolder;

   protected PeassFolders folders;
   protected VersionControlSystem vcs;
   protected StaticTestSelection staticTestSelection;
   protected ExecutionData executionData;

   protected String startversion;
   protected String endversion;
   protected String version;

   @Option(names = { "-threads", "--threads" }, description = "Number of parallel threads for analysis")
   protected int threads = 1;

   @Option(names = { "-staticSelectionFile", "--staticSelectionFile" }, description = "Path to the staticSelectionFile")
   protected File staticSelectionFile;

   @Option(names = { "-executionfile", "--executionfile" }, description = "Path to the executionfile (may be trace based selection or coverage selection file)")
   protected File executionfile;

   public VersionProcessor(final File projectFolder, final StaticTestSelection dependencies) {
      this.folders = new PeassFolders(projectFolder);
      this.staticTestSelection = dependencies;
      startversion = null;
      endversion = null;
      threads = 1;
   }

   public void setStartversion(final String startversion) {
      this.startversion = startversion;
   }

   public void setEndversion(final String endversion) {
      this.endversion = endversion;
   }

   public VersionProcessor() {
      if (executionMixin != null) {
         startversion = executionMixin.getStartcommit();
         endversion = executionMixin.getEndcommit();
         version = executionMixin.getCommit();
      }
   }

   public void processCommandline() {
      LOG.debug("Processing initial");
      processInitialVersion(staticTestSelection.getInitialversion());

      if (threads != 1) {
         throw new RuntimeException("Parallel processing is not possible or implemented; do not set threads!");
      } else {
         for (final Map.Entry<String, VersionStaticSelection> version : staticTestSelection.getVersions().entrySet()) {
            LOG.debug("Processing {}", version.getKey());
            processVersion(version.getKey(), version.getValue());
         }
      }
      postEvaluate();
   }

   protected void processInitialVersion(final InitialVersion version) {

   }

   protected abstract void processVersion(String key, VersionStaticSelection version);

   protected void postEvaluate() {

   }

   @Override
   public Void call() throws Exception {
      initVersionProcessor();
      return null;
   }

   protected void initVersionProcessor() throws IOException, JsonParseException, JsonMappingException {
      if (executionMixin != null) {
         startversion = executionMixin.getStartcommit();
         endversion = executionMixin.getEndcommit();
         version = executionMixin.getCommit();
      }
      
      if (staticSelectionFile != null) {
          staticTestSelection = Constants.OBJECTMAPPER.readValue(staticSelectionFile, StaticTestSelection.class);
          VersionComparator.setDependencies(staticTestSelection);
          executionData = new ExecutionData(staticTestSelection);
       }
      if (executionfile != null) {
         executionData = Constants.OBJECTMAPPER.readValue(executionfile, ExecutionData.class);
         staticTestSelection = new StaticTestSelection(executionData);
      }
      if (executionData == null && staticTestSelection == null) {
         throw new RuntimeException("Dependencyfile and executionfile not readable - one needs to be defined!");
      }
      
      if (!projectFolder.exists()) {
         GitUtils.downloadProject(staticTestSelection.getUrl(), projectFolder);
      }
      folders = new PeassFolders(projectFolder);

      if (startversion != null || endversion != null) {
         LOG.info("Version: " + startversion + " - " + endversion);
      }

      if (executionMixin.getCommitOld() != null && startversion == null) {
         throw new RuntimeException("If versionOld is specified, always specify version!");
      }

      if (version != null) {
         if (startversion != null || endversion != null) {
            throw new RuntimeException("Both, version and (startversion or endversion), are defined - define version, or startversion/endversion!");
         }
         startversion = version;
         endversion = version;
         LOG.info("Version: " + startversion + " - " + endversion);
      }

      VersionComparator.setDependencies(staticTestSelection);
      vcs = VersionControlSystem.getVersionControlSystem(folders.getProjectFolder());
   }
}
