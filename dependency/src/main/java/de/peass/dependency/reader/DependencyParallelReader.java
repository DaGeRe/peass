package de.peass.dependency.reader;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.config.DependencyConfig;
import de.peass.config.ExecutionConfig;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependency.parallel.OneReader;
import de.peass.vcs.GitCommit;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionIterator;
import de.peass.vcs.VersionIteratorGit;

public class DependencyParallelReader {
   private static final Logger LOG = LogManager.getLogger(DependencyParallelReader.class);

   private final DependencyConfig dependencyConfig;
   private final String url;
   private final VersionKeeper nonRunning;
   private final VersionKeeper nonChanges;
   private final List<GitCommit> commits;
   private final PeASSFolders folders;
   private final int sizePerThread;
   private final File[] outFiles;
   private final File tempResultFolder;
   private final String project;
   private final ExecutionConfig executionConfig;
   private final EnvironmentVariables env;

   public DependencyParallelReader(final File projectFolder, final File resultBaseFolder, final String project, final List<GitCommit> commits, final DependencyConfig dependencyConfig,
         final int timeout, final ExecutionConfig executionConfig, final EnvironmentVariables env) {
      url = GitUtils.getURL(projectFolder);
      this.dependencyConfig = dependencyConfig;
      this.executionConfig = executionConfig;
      LOG.debug(url);
      folders = new PeASSFolders(projectFolder);
      this.commits = commits;
      this.project = project;
      this.env = env;

      tempResultFolder = new File(resultBaseFolder, "temp_" + project);
      if (!tempResultFolder.exists()) {
         tempResultFolder.mkdirs();
      }
      LOG.info("Writing to: {}", tempResultFolder.getAbsolutePath());

      nonRunning = new VersionKeeper(new File(tempResultFolder, "nonRunning_" + project + ".json"));
      nonChanges = new VersionKeeper(new File(tempResultFolder, "nonChanges_" + project + ".json"));

      sizePerThread = commits.size() > 2 * dependencyConfig.getThreads() ? commits.size() / dependencyConfig.getThreads() : 2;
      outFiles = commits.size() > 2 * dependencyConfig.getThreads() ? new File[dependencyConfig.getThreads()] : new File[1];

      LOG.debug("Threads: {} Size per Thread: {} OutFile: {}", dependencyConfig.getThreads(), sizePerThread, outFiles.length);
   }

   public File[] readDependencies() throws InterruptedException, IOException {
      final ExecutorService service = Executors.newFixedThreadPool(outFiles.length, new ThreadFactory() {

         int threadcount = 0;

         @Override
         public Thread newThread(final Runnable runnable) {
            threadcount++;
            return new Thread(runnable, "dependencypool-" + threadcount);
         }
      });

      startAllProcesses(service);
      service.shutdown();
      waitForAll(service);

      return outFiles;
   }

   private void startAllProcesses(final ExecutorService service) throws IOException, InterruptedException {
      for (int outfileIndex = 0; outfileIndex < outFiles.length; outfileIndex++) {
         final int readableIndex = outfileIndex + 1;
         outFiles[outfileIndex] = new File(tempResultFolder, "deps_" + project + "_" + readableIndex + ".json");
         PeASSFolders foldersTemp = folders.getTempFolder("" + readableIndex);
         final File currentOutFile = outFiles[outfileIndex];
         startPartProcess(currentOutFile, service, outfileIndex, foldersTemp);
      }
   }

   private void waitForAll(final ExecutorService service) {
      try {
         LOG.debug("Wait for finish");
         final boolean success = service.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
         LOG.debug("Finished Reading: {} - {}", success, service.isTerminated());
      } catch (final InterruptedException e) {
         e.printStackTrace();
      }
   }

   public void startPartProcess(final File currentOutFile, final ExecutorService service, final int outfileIndex, final PeASSFolders foldersTemp) throws InterruptedException {
      final int min = outfileIndex * sizePerThread;
      final int max = Math.min((outfileIndex + 1) * sizePerThread + 1, commits.size());
      LOG.debug("Min: {} Max: {} Size: {}", min, max, commits.size());
      final List<GitCommit> currentCommits = commits.subList(min, max);
      final List<GitCommit> reserveCommits = commits.subList(max - 1, commits.size());
      final GitCommit minimumCommit = commits.get(Math.min(max, commits.size() - 1));

      if (currentCommits.size() > 0) {
         processCommits(currentOutFile, service, foldersTemp, currentCommits, reserveCommits, minimumCommit);
      }
   }

   void processCommits(final File currentOutFile, final ExecutorService service, final PeASSFolders foldersTemp, final List<GitCommit> currentCommits,
         final List<GitCommit> reserveCommits, final GitCommit minimumCommit) throws InterruptedException {
      LOG.debug("Start: {} End: {}", currentCommits.get(0), currentCommits.get(currentCommits.size() - 1));
      LOG.debug(currentCommits);
      final VersionIterator iterator = new VersionIteratorGit(foldersTemp.getProjectFolder(), currentCommits, null);
      FirstRunningVersionFinder finder = new FirstRunningVersionFinder(foldersTemp, nonRunning, iterator, executionConfig, env);
      final DependencyReader reader = new DependencyReader(dependencyConfig, foldersTemp, currentOutFile, url, iterator, nonChanges, executionConfig, env);
      final VersionIteratorGit reserveIterator = new VersionIteratorGit(foldersTemp.getProjectFolder(), reserveCommits, null);
      final Runnable current = new OneReader(minimumCommit, currentOutFile, reserveIterator, reader, finder);
      service.submit(current);
      Thread.sleep(5);
   }

}
