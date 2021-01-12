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

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.parallel.OneReader;
import de.peass.dependency.traces.TemporaryProjectFolderUtil;
import de.peass.vcs.GitCommit;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionIterator;
import de.peass.vcs.VersionIteratorGit;

public class DependencyParallelReader {
   private static final Logger LOG = LogManager.getLogger(DependencyParallelReader.class);

   private final String url;
   private final int timeout;
   private final VersionKeeper nonRunning;
   private final VersionKeeper nonChanges;
   private final List<GitCommit> commits;
   private final PeASSFolders folders;
   private final int sizePerThread;
   private final File[] outFiles;
   private final File tempResultFolder;
   private final String project;

   public DependencyParallelReader(final File projectFolder, final File resultBaseFolder, final String project, final List<GitCommit> commits, final int threadCount,
         final int timeout) {
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

      sizePerThread = commits.size() > 2 * threadCount ? commits.size() / threadCount : 2;
      outFiles = commits.size() > 2 * threadCount ? new File[threadCount] : new File[1];

      LOG.debug("Threads: {} Size per Thread: {} OutFile: {}", threadCount, sizePerThread, outFiles.length);

      this.timeout = timeout;
   }

   public File[] readDependencies() throws InterruptedException, IOException {
      final ExecutorService service = Executors.newFixedThreadPool(outFiles.length, new ThreadFactory() {

         int threadcount = 0;

         @Override
         public Thread newThread(final Runnable r) {
            threadcount++;
            return new Thread(r, "dependencypool-" + threadcount);
         }
      });

      for (int i = 0; i < outFiles.length; i++) {
         final int readableIndex = i + 1;
         outFiles[i] = new File(tempResultFolder, "deps_" + project + "_" + readableIndex + ".json");
         final File projectFolderTemp = new File(folders.getTempProjectFolder(), "" + readableIndex);
         TemporaryProjectFolderUtil.cloneForcefully(folders, projectFolderTemp);
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
      final int min = i * sizePerThread;
      final int max = Math.min((i + 1) * sizePerThread + 1, commits.size());
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
      LOG.debug("Start: {} End: {}", currentCommits.get(0), currentCommits.get(currentCommits.size() - 1));
      LOG.debug(currentCommits);
      final VersionIterator iterator = new VersionIteratorGit(projectFolderTemp, currentCommits, null);
      FirstRunningVersionFinder finder = new FirstRunningVersionFinder(new PeASSFolders(projectFolderTemp), nonRunning, iterator, timeout);
      final DependencyReader reader = new DependencyReader(projectFolderTemp, currentOutFile, url, iterator, timeout, nonChanges);
      final VersionIteratorGit reserveIterator = new VersionIteratorGit(projectFolderTemp, reserveCommits, null);
      final Runnable current = new OneReader(minimumCommit, currentOutFile, reserveIterator, reader, finder);
      service.submit(current);
      Thread.sleep(5);
   }

}
