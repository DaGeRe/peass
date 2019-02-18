package de.peass.dependencyprocessors;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.InitialVersion;
import de.peass.dependency.persistence.Version;
import de.peass.dependency.traces.ViewGenerator;
import de.peass.statistics.DependencyStatisticAnalyzer;
import de.peass.utils.OptionConstants;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionControlSystem;

/**
 * Basic class for all classes that operate somehow on an folder and it's dependencyfile.
 * 
 * @author reichelt
 *
 */
public abstract class VersionProcessor {

   private static final Logger LOG = LogManager.getLogger(VersionProcessor.class);

   protected PeASSFolders folders;
   protected VersionControlSystem vcs;
   protected final Dependencies dependencies;
   protected final CommandLine line;
   protected String startversion;
   protected String endversion;
   protected int threads;
   protected final int timeout;

   public VersionProcessor(final File projectFolder, final Dependencies dependencies, final int timeout) {
      this.folders = new PeASSFolders(projectFolder);
      this.dependencies = dependencies;
      line = null;
      startversion = null;
      endversion = null;
      threads = 1;
      this.timeout = timeout;
   }

   public void setStartversion(final String startversion) {
      this.startversion = startversion;
   }

   public void setEndversion(final String endversion) {
      this.endversion = endversion;
   }

   public VersionProcessor(final String[] args) throws ParseException, JAXBException {
      final Options options = OptionConstants.createOptions(OptionConstants.FOLDER, OptionConstants.DEPENDENCYFILE, OptionConstants.WARMUP, OptionConstants.ITERATIONS,
            OptionConstants.VMS,
            OptionConstants.STARTVERSION, OptionConstants.ENDVERSION,
            OptionConstants.EXECUTIONFILE, OptionConstants.REPETITIONS, OptionConstants.DURATION,
            OptionConstants.CHANGEFILE, OptionConstants.TEST, OptionConstants.USEKIEKER, OptionConstants.THREADS,
            OptionConstants.TIMEOUT, OptionConstants.OUT);
      final CommandLineParser parser = new DefaultParser();

      line = parser.parse(options, args);

      final File dependencyFile = new File(line.getOptionValue(OptionConstants.DEPENDENCYFILE.getName()));
      dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);

      final File projectFolder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));
      this.folders = new PeASSFolders(projectFolder);
      if (!projectFolder.exists()) {
         GitUtils.downloadProject(dependencies.getUrl(), projectFolder);
      }

      startversion = line.getOptionValue(OptionConstants.STARTVERSION.getName(), null);
      endversion = line.getOptionValue(OptionConstants.ENDVERSION.getName(), null);
      if (startversion != null || endversion != null) {
         LOG.info("Version: " + startversion + " - " + endversion);
      }

      VersionComparator.setDependencies(dependencies);
      vcs = VersionControlSystem.getVersionControlSystem(projectFolder);

      threads = Integer.parseInt(line.getOptionValue(OptionConstants.THREADS.getName(), "1"));
      timeout = Integer.parseInt(line.getOptionValue(OptionConstants.TIMEOUT.getName(), "60000"));
   }

   public void processCommandline() throws ParseException, JAXBException {
      processInitialVersion(dependencies.getInitialversion());

      if (threads != 1) {
         final ExecutorService service = Executors.newFixedThreadPool(threads);
         if (this instanceof ViewGenerator) {
            final ViewGenerator generator = (ViewGenerator) this;
            for (final Map.Entry<String, Version> version : dependencies.getVersions().entrySet()) {
               generator.processVersion(version.getKey(), version.getValue(), service);
            }
         }else {
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
            processVersion(version.getKey(), version.getValue());
         }
      }

   }

   protected void processInitialVersion(final InitialVersion version) {

   }

   protected abstract void processVersion(String key, Version version);

   protected CommandLine getLine() {
      return line;
   }
}
