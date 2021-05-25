package de.dagere.peass.dependency.reader;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.DependencyConfig;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.ResultsFolders;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.parallel.OneReader;
import de.dagere.peass.vcs.GitCommit;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionIterator;
import de.dagere.peass.vcs.VersionIteratorGit;

public class DependencyParallelReader {
   private static final Logger LOG = LogManager.getLogger(DependencyParallelReader.class);

   private final DependencyConfig dependencyConfig;
   private final String url;
   private final VersionKeeper nonRunning;
   private final VersionKeeper nonChanges;
   private final List<GitCommit> commits;
   private final PeASSFolders folders;
   private final int sizePerThread;
   private final ResultsFolders[] outFolders;
   private final File tempResultFolder;
   private final String project;
   private final ExecutionConfig executionConfig;
   private final EnvironmentVariables env;

   public DependencyParallelReader(final File projectFolder, final File resultBaseFolder, final String project, final List<GitCommit> commits,
         final DependencyConfig dependencyConfig, final ExecutionConfig executionConfig, final EnvironmentVariables env) {
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
      outFolders = commits.size() > 2 * dependencyConfig.getThreads() ? new ResultsFolders[dependencyConfig.getThreads()] : new ResultsFolders[1];

      LOG.debug("Threads: {} Size per Thread: {} OutFile: {}", dependencyConfig.getThreads(), sizePerThread, outFolders.length);
   }

   public ResultsFolders[] readDependencies() throws InterruptedException, IOException {
      final ExecutorService service = Executors.newFixedThreadPool(outFolders.length, new ThreadFactory() {

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

      return outFolders;
   }

   private void startAllProcesses(final ExecutorService service) throws IOException, InterruptedException {
      for (int outfileIndex = 0; outfileIndex < outFolders.length; outfileIndex++) {
         final int readableIndex = outfileIndex + 1;
         outFolders[outfileIndex] = new ResultsFolders(new File(tempResultFolder, "temp_" + project + "_" + readableIndex), project);
         PeASSFolders foldersTemp = folders.getTempFolder("" + readableIndex);
         final ResultsFolders currentOutFile = outFolders[outfileIndex];
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

   public void startPartProcess(final ResultsFolders currentOutFolders, final ExecutorService service, final int outfileIndex, final PeASSFolders foldersTemp)
         throws InterruptedException {
      final int min = outfileIndex * sizePerThread;
      final int max = Math.min((outfileIndex + 1) * sizePerThread + 1, commits.size());
      LOG.debug("Min: {} Max: {} Size: {}", min, max, commits.size());
      final List<GitCommit> currentCommits = commits.subList(min, max);
      final List<GitCommit> reserveCommits = commits.subList(max - 1, commits.size());
      final GitCommit minimumCommit = commits.get(Math.min(max, commits.size() - 1));

      if (currentCommits.size() > 0) {
         processCommits(currentOutFolders, service, foldersTemp, currentCommits, reserveCommits, minimumCommit);
      }
   }

   void processCommits(final ResultsFolders currentOutFolders, final ExecutorService service, final PeASSFolders foldersTemp, final List<GitCommit> currentCommits,
         final List<GitCommit> reserveCommits, final GitCommit minimumCommit) throws InterruptedException {
      LOG.debug("Start: {} End: {}", currentCommits.get(0), currentCommits.get(currentCommits.size() - 1));
      LOG.debug(currentCommits);
      final VersionIterator iterator = new VersionIteratorGit(foldersTemp.getProjectFolder(), currentCommits, null);
      FirstRunningVersionFinder finder = new FirstRunningVersionFinder(foldersTemp, nonRunning, iterator, executionConfig, env);
      final DependencyReader reader = new DependencyReader(dependencyConfig, foldersTemp, currentOutFolders, url, iterator, nonChanges, executionConfig, env);
      final VersionIteratorGit reserveIterator = new VersionIteratorGit(foldersTemp.getProjectFolder(), reserveCommits, null);
      final Runnable current = new OneReader(minimumCommit, reserveIterator, reader, finder);
      service.submit(current);
      Thread.sleep(5);
   }

}
