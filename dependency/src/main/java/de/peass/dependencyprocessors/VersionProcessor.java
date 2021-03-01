package de.peass.dependencyprocessors;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.execution.ExecutionConfigMixin;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependency.persistence.InitialVersion;
import de.peass.dependency.persistence.Version;
import de.peass.dependency.traces.ViewGenerator;
import de.peass.utils.Constants;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionControlSystem;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * Basic class for all classes that operate somehow on an folder and it's dependencyfile.
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

   protected PeASSFolders folders;
   protected VersionControlSystem vcs;
   protected Dependencies dependencies;
   protected ExecutionData executionData;

   protected String startversion;
   protected String endversion;
   protected String version;

   @Option(names = { "-threads", "--threads" }, description = "Number of parallel threads for analysis")
   protected int threads = 1;

   @Option(names = { "-dependencyfile", "--dependencyfile" }, description = "Path to the dependencyfile")
   protected File dependencyFile;

   @Option(names = { "-executionfile", "--executionfile" }, description = "Path to the executionfile")
   protected File executionfile;

   public VersionProcessor(final File projectFolder, final Dependencies dependencies) {
      this.folders = new PeASSFolders(projectFolder);
      this.dependencies = dependencies;
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
         startversion = executionMixin.getStartversion();
         endversion = executionMixin.getEndversion();
         version = executionMixin.getVersion();
      }
   }

   public void processCommandline() throws JAXBException {
      LOG.debug("Processing initial");
      processInitialVersion(dependencies.getInitialversion());

      if (threads != 1) {
         final ExecutorService service = Executors.newFixedThreadPool(threads);
         if (this instanceof ViewGenerator) {
            final ViewGenerator generator = (ViewGenerator) this;
            for (final Map.Entry<String, Version> version : dependencies.getVersions().entrySet()) {
               generator.processVersion(version.getKey(), version.getValue(), service);
            }
         } else {
            throw new RuntimeException("Parallel processing is not possible or implemented; do not set threads!");
         }

         service.shutdown();
         try {
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
         } catch (final InterruptedException e) {
            e.printStackTrace();
         }

      } else {
         for (final Map.Entry<String, Version> version : dependencies.getVersions().entrySet()) {
            LOG.debug("Processing {}", version.getKey());
            processVersion(version.getKey(), version.getValue());
         }
      }
      postEvaluate();
   }

   protected void processInitialVersion(final InitialVersion version) {

   }

   protected abstract void processVersion(String key, Version version);

   protected void postEvaluate() {

   }

   @Override
   public Void call() throws Exception {
      initVersionProcessor();
      return null;
   }

   protected void initVersionProcessor() throws IOException, JsonParseException, JsonMappingException {
      if (dependencyFile != null) {
         dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
         VersionComparator.setDependencies(dependencies);
         executionData = new ExecutionData(dependencies);
      }
      if (executionfile != null) {
         executionData = Constants.OBJECTMAPPER.readValue(executionfile, ExecutionData.class);
         dependencies = new Dependencies(executionData);
      }
      if (executionData == null && dependencies == null) {
         throw new RuntimeException("Dependencyfile and executionfile not readable - one needs to be defined!");
      }

      folders = new PeASSFolders(projectFolder);
      if (!projectFolder.exists()) {
         GitUtils.downloadProject(dependencies.getUrl(), projectFolder);
      }

      if (startversion != null || endversion != null) {
         LOG.info("Version: " + startversion + " - " + endversion);
      }

      if (executionMixin.getVersionOld() != null && startversion == null) {
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

      VersionComparator.setDependencies(dependencies);
      vcs = VersionControlSystem.getVersionControlSystem(folders.getProjectFolder());
   }
}
