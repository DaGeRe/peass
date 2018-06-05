package de.peran.dependencyprocessors;

import java.io.File;
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

import de.peran.dependency.PeASSFolders;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Initialversion;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.statistics.DependencyStatisticAnalyzer;
import de.peran.utils.OptionConstants;
import de.peran.vcs.GitUtils;
import de.peran.vcs.VersionControlSystem;

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
   protected final Versiondependencies dependencies;
   protected final CommandLine line;
   protected String startversion;
   protected String endversion;
   private final int threads;

   public VersionProcessor(File projectFolder, Versiondependencies dependencies) {
      this.folders = new PeASSFolders(projectFolder);
      this.dependencies = dependencies;
      line = null;
      startversion = null;
      endversion = null;
      threads = 1;
   }

   public void setStartversion(String startversion) {
      this.startversion = startversion;
   }

   public void setEndversion(String endversion) {
      this.endversion = endversion;
   }

   public VersionProcessor(final String[] args) throws ParseException, JAXBException {
      final Options options = OptionConstants.createOptions(OptionConstants.FOLDER, OptionConstants.DEPENDENCYFILE, OptionConstants.WARMUP, OptionConstants.ITERATIONS,
            OptionConstants.VMS,
            OptionConstants.STARTVERSION, OptionConstants.ENDVERSION,
            OptionConstants.EXECUTIONFILE, OptionConstants.REPETITIONS, OptionConstants.DURATION,
            OptionConstants.CHANGEFILE, OptionConstants.TEST, OptionConstants.USEKIEKER, OptionConstants.THREADS);
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

      VersionComparator.setDependencies(dependencies);
      vcs = VersionControlSystem.getVersionControlSystem(projectFolder);

      threads = Integer.parseInt(line.getOptionValue(OptionConstants.THREADS.getName(), "1"));
   }

   public void processCommandline() throws ParseException, JAXBException {
      processInitialVersion(dependencies.getInitialversion());

      if (threads != 1) {
         final ExecutorService service = Executors.newFixedThreadPool(threads);
         int index = 0;
         for (final Version versioninfo : dependencies.getVersions().getVersion()) {
            final boolean beforeEndVersion = endversion == null || versioninfo.getVersion().equals(endversion) || VersionComparator.isBefore(versioninfo.getVersion(), endversion);
            if (!VersionComparator.isBefore(versioninfo.getVersion(), startversion) && beforeEndVersion) {
               final File projectFolderTemp = new File(folders.getTempProjectFolder(), "" + index);
               final Runnable runVersion = processVersionParallel(versioninfo, projectFolderTemp);
               service.submit(runVersion);
            }

            index++;
         }
         service.shutdown();
         try {
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
         } catch (final InterruptedException e) {
            e.printStackTrace();
         }
         
      } else {
         for (final Version version : dependencies.getVersions().getVersion()) {
            processVersion(version);
         }
      }

   }
   
   protected Runnable processVersionParallel(Version version, final File projectFolderTemp) {
      throw new RuntimeException("Parallel processing is not possible or implemented; do not set threads!");
   }

   protected void processInitialVersion(final Initialversion version) {

   }

   protected abstract void processVersion(Version version);

   protected CommandLine getLine() {
      return line;
   }
}
