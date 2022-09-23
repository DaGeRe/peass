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

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.dependency.parallel.OneReader;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.vcs.CommitIterator;
import de.dagere.peass.vcs.CommitIteratorGit;
import de.dagere.peass.vcs.GitUtils;

public class DependencyParallelReader {
   private static final Logger LOG = LogManager.getLogger(DependencyParallelReader.class);

   private final TestSelectionConfig dependencyConfig;
   private final String url;
   private final CommitKeeper nonRunning;
   private final CommitKeeper nonChanges;
   private final CommitComparatorInstance comparator;
   private final PeassFolders folders;
   private final int sizePerThread;
   private final ResultsFolders[] outFolders;
   private final File tempResultFolder;
   private final String project;
   private final ExecutionConfig executionConfig;
   private final KiekerConfig kiekerConfig;
   private final EnvironmentVariables env;

   public DependencyParallelReader(final File projectFolder, final File resultBaseFolder, final String project, final CommitComparatorInstance commits,
         final TestSelectionConfig dependencyConfig, final ExecutionConfig executionConfig, final KiekerConfig kiekerConfig, final EnvironmentVariables env) {
      url = GitUtils.getURL(projectFolder);
      this.dependencyConfig = dependencyConfig;
      this.executionConfig = executionConfig;
      this.kiekerConfig = kiekerConfig;
      LOG.debug(url);
      folders = new PeassFolders(projectFolder);
      this.comparator = commits;
      this.project = project;
      this.env = env;

      tempResultFolder = new File(resultBaseFolder, "temp_" + project);
      if (!tempResultFolder.exists()) {
         tempResultFolder.mkdirs();
      }
      LOG.info("Writing to: {}", tempResultFolder.getAbsolutePath());

      nonRunning = new CommitKeeper(new File(tempResultFolder, "nonRunning_" + project + ".json"));
      nonChanges = new CommitKeeper(new File(tempResultFolder, "nonChanges_" + project + ".json"));

      sizePerThread = commits.getCommits().size() > 2 * dependencyConfig.getThreads() ? commits.getCommits().size() / dependencyConfig.getThreads() : 2;
      outFolders = commits.getCommits().size() > 2 * dependencyConfig.getThreads() ? new ResultsFolders[dependencyConfig.getThreads()] : new ResultsFolders[1];

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
         PeassFolders foldersTemp = folders.getTempFolder("" + readableIndex, executionConfig.getGitCryptKey());
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

   public void startPartProcess(final ResultsFolders currentOutFolders, final ExecutorService service, final int outfileIndex, final PeassFolders foldersTemp)
         throws InterruptedException {
      final int min = outfileIndex * sizePerThread;
      final int max = Math.min((outfileIndex + 1) * sizePerThread + 1, comparator.getCommits().size());
      LOG.debug("Min: {} Max: {} Size: {}", min, max, comparator.getCommits().size());
      final List<String> currentCommits = comparator.getCommits().subList(min, max);
      final List<String> reserveCommits = comparator.getCommits().subList(max - 1, comparator.getCommits().size());
      final String minimumCommit = comparator.getCommits().get(Math.min(max, comparator.getCommits().size() - 1));

      if (currentCommits.size() > 0) {
         processCommits(currentOutFolders, service, foldersTemp, currentCommits, reserveCommits, minimumCommit);
      }
   }

   void processCommits(final ResultsFolders currentOutFolders, final ExecutorService service, final PeassFolders foldersTemp, final List<String> currentCommits,
         final List<String> reserveCommits, final String minimumCommit) throws InterruptedException {
      LOG.debug("Start: {} End: {}", currentCommits.get(0), currentCommits.get(currentCommits.size() - 1));
      LOG.debug(currentCommits);
      final CommitIterator iterator = new CommitIteratorGit(foldersTemp.getProjectFolder(), currentCommits, null);
      FirstRunningCommitFinder finder = new FirstRunningCommitFinder(foldersTemp, nonRunning, iterator, executionConfig, env);
      final DependencyReader reader = new DependencyReader(dependencyConfig, foldersTemp, currentOutFolders, url, iterator, nonChanges, executionConfig, kiekerConfig, env);
      final CommitIteratorGit reserveIterator = new CommitIteratorGit(foldersTemp.getProjectFolder(), reserveCommits, null);
      final Runnable current = new OneReader(minimumCommit, reserveIterator, reader, finder, comparator);
      service.submit(current);
      Thread.sleep(5);
   }

}
