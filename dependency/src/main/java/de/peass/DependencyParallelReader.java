package de.peass;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.parallel.Merger;
import de.peass.dependency.parallel.OneReader;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.reader.DependencyReader;
import de.peass.dependency.reader.DependencyReaderUtil;
import de.peass.dependency.reader.VersionKeeper;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.Constants;
import de.peass.utils.OptionConstants;
import de.peass.vcs.GitCommit;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionIterator;
import de.peass.vcs.VersionIteratorGit;

public class DependencyParallelReader {
   private static final Logger LOG = LogManager.getLogger(DependencyParallelReader.class);

   public static void main(final String[] args) throws ParseException, JsonGenerationException, JsonMappingException, IOException, InterruptedException, JAXBException {
      final Options options = OptionConstants.createOptions(OptionConstants.FOLDER, OptionConstants.STARTVERSION, OptionConstants.ENDVERSION, OptionConstants.OUT,
            OptionConstants.TIMEOUT, OptionConstants.THREADS);

      final CommandLineParser parser = new DefaultParser();
      final CommandLine line = parser.parse(options, args);

      final File projectFolder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));
      if (!projectFolder.exists()) {
         throw new RuntimeException("Folder " + projectFolder.getAbsolutePath() + " does not exist.");
      }
      final String project = projectFolder.getName();

      final List<GitCommit> commits = DependencyReadingStarter.getGitCommits(line, projectFolder);
      VersionComparator.setVersions(commits);

      final int timeout = Integer.parseInt(line.getOptionValue(OptionConstants.TIMEOUT.getName(), "5"));
      final int threads = Integer.parseInt(line.getOptionValue(OptionConstants.THREADS.getName(), "4"));

      final File resultBaseFolder = new File(line.getOptionValue(OptionConstants.OUT.getName(), "results"));
      DependencyParallelReader reader = new DependencyParallelReader(projectFolder, resultBaseFolder, project, commits, threads, timeout);
      final File[] outFiles = reader.readDependencies();

      final File dependencyOut = DependencyReadingStarter.getDependencyFile(line, projectFolder);

      Merger.mergeVersions(dependencyOut, outFiles);
   }

   private final String url;
   private final int timeout;
   private final VersionKeeper nonRunning;
   private final VersionKeeper nonChanges;
   private final List<GitCommit> commits;
   private final PeASSFolders folders;
   private final int size;
   private final File[] outFiles;
   private final File tempResultFolder;
   private final String project;

   public DependencyParallelReader(final File projectFolder, final File resultBaseFolder, String project, final List<GitCommit> commits, int count, final int timeout) {
      url = GitUtils.getURL(projectFolder);
      LOG.debug(url);
      folders = new PeASSFolders(projectFolder);
      this.commits = commits;
      this.project = project;

      tempResultFolder = new File(resultBaseFolder, "temp_" + project);
      if (!tempResultFolder.exists()) {
         tempResultFolder.mkdirs();
      }
      LOG.info("Writing to: {}", tempResultFolder.getAbsolutePath());

      nonRunning = new VersionKeeper(new File(tempResultFolder, "nonRunning_" + project + ".json"));
      nonChanges = new VersionKeeper(new File(tempResultFolder, "nonChanges_" + project + ".json"));

      size = commits.size() > 2 * count ? commits.size() / count : 2;

      outFiles = new File[count];

      this.timeout = timeout;
   }

   public File[] readDependencies() throws InterruptedException, IOException {
      final ExecutorService service = Executors.newFixedThreadPool(outFiles.length, new ThreadFactory() {

         int threadcount = 0;

         @Override
         public Thread newThread(Runnable r) {
            threadcount++;
            return new Thread(r, "dependencypool-" + threadcount);
         }
      });

      for (int i = 0; i < outFiles.length; i++) {
         outFiles[i] = new File(tempResultFolder, "deps_" + project + "_" + i + ".json");
         final File projectFolderTemp = new File(folders.getTempProjectFolder(), "" + i);
         GitUtils.clone(folders, projectFolderTemp);
         final File currentOutFile = outFiles[i];
         startPartProcess(currentOutFile, service, i, projectFolderTemp);
      }
      service.shutdown();
      try {
         LOG.debug("Wait for finish");
         final boolean success = service.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
         LOG.debug("Finished Reading: {} - {}", success, service.isTerminated());
      } catch (final InterruptedException e) {
         e.printStackTrace();
      }

      return outFiles;
   }

   public void startPartProcess(final File currentOutFile, final ExecutorService service, final int i, final File projectFolderTemp) throws InterruptedException {
      int min = i * size;
      final int max = Math.min((i + 1) * size + 3, commits.size());// Assuming one in three commits should contain a source-change
      LOG.debug("Min: {} Max: {} Size: {}", min, max, commits.size());
      final List<GitCommit> currentCommits = commits.subList(min, max);
      final List<GitCommit> reserveCommits = commits.subList(max - 1, commits.size());
      final GitCommit minimumCommit = commits.get(Math.min(max, commits.size() - 1));

      if (currentCommits.size() > 0) {
         processCommits(currentOutFile, service, projectFolderTemp, currentCommits, reserveCommits, minimumCommit);
      }
   }

   void processCommits(final File currentOutFile, final ExecutorService service, final File projectFolderTemp, final List<GitCommit> currentCommits,
         final List<GitCommit> reserveCommits, final GitCommit minimumCommit) throws InterruptedException {
      final VersionIterator iterator = new VersionIteratorGit(projectFolderTemp, currentCommits, null);
      DependencyReader reader = new DependencyReader(projectFolderTemp, currentOutFile, url, iterator, timeout, nonRunning, nonChanges);
      VersionIteratorGit reserveIterator = new VersionIteratorGit(projectFolderTemp, reserveCommits, null);
      final Runnable current = new OneReader(minimumCommit, currentOutFile, reserveIterator, reader);
      service.submit(current);
      Thread.sleep(5);
   }

}
