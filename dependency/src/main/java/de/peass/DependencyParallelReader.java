package de.peass;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.reader.DependencyReader;
import de.peass.dependency.reader.DependencyReaderBase;
import de.peass.dependency.reader.DependencyReaderUtil;
import de.peass.dependency.reader.VersionKeeper;
import de.peass.dependencyprocessors.VersionComparator;
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
      final File[] outFiles = getDependencies(projectFolder, resultBaseFolder, project, commits, timeout, threads);

      final File dependencyOut = DependencyReadingStarter.getDependencyFile(line, projectFolder);

      mergeVersions(dependencyOut, outFiles);

   }

   public static File[] getDependencies(final File projectFolder, final File resultBaseFolder, final String project, final List<GitCommit> commits, final int timeout, final int count)
         throws InterruptedException, IOException {
      final String url = GitUtils.getURL(projectFolder);
      LOG.debug(url);

      final File[] outFiles = new File[count];
      final PeASSFolders folders = new PeASSFolders(projectFolder);
      final ExecutorService service = Executors.newFixedThreadPool(count);
      // final File resultsFolder = new File("results");
      // if (!resultsFolder.exists()) {
      // resultsFolder.mkdirs();
      // }
      final File tempResultFolder = new File(resultBaseFolder, "temp_" + project);
      if (!tempResultFolder.exists()) {
         tempResultFolder.mkdirs();
      }

      final VersionKeeper nonRunning = new VersionKeeper(new File(tempResultFolder, "nonRunning_" + project + ".json"));
      final VersionKeeper nonChanges = new VersionKeeper(new File(tempResultFolder, "nonChanges_" + project + ".json"));

      final int size = commits.size() / count;
      for (int i = 0; i < count; i++) {
         outFiles[i] = new File(tempResultFolder, "deps_" + project + "_" + i + ".json");
         final File projectFolderTemp = new File(folders.getTempProjectFolder(), "" + i);
         GitUtils.clone(folders, projectFolderTemp);
         final int max = Math.min((i + 1) * size + 3, commits.size() - 1);// Assuming one in three commits should contain a source-change
         final List<GitCommit> currentCommits = commits.subList(i * size, max);
         final List<GitCommit> reserveCommits = commits.subList(max, commits.size() - 1);
         final GitCommit minimumCommit = commits.get(i * size);
         final File currentOutFile = outFiles[i];
         final Runnable current = new Runnable() {
            @Override
            public void run() {
               try {
                  final VersionIterator iterator = new VersionIteratorGit(projectFolderTemp, currentCommits, null);

                  final DependencyReader reader = new DependencyReader(projectFolderTemp, currentOutFile, url, iterator, timeout, nonRunning, nonChanges);
                  LOG.debug("Reader initalized: " + currentOutFile);
                  final boolean readingSuccess = reader.readDependencies();
                  if (readingSuccess) {
                     String newest = reader.getDependencies().getNewestVersion();
                     final VersionIteratorGit reserveIterator = new VersionIteratorGit(projectFolderTemp, reserveCommits, null);
                     reader.setIterator(reserveIterator);
                     while (reserveIterator.hasNextCommit() && VersionComparator.isBefore(newest, minimumCommit.getTag())) {
                        reserveIterator.goToNextCommit();
                        try {
                           reader.readVersion();
                        } catch (final IOException e) {
                           e.printStackTrace();
                        }
                        newest = reader.getDependencies().getNewestVersion();
                     }
                  }
               } catch (final Throwable e) {
                  e.printStackTrace();
               }
            }
         };
         service.submit(current);
         Thread.sleep(5);
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

   public static Dependencies mergeVersions(final File out, final File[] partFiles) throws IOException, JsonGenerationException, JsonMappingException {
      final List<Dependencies> deps = new LinkedList<>();
      for (int i = 0; i < partFiles.length; i++) {
         try {
            LOG.debug("Reading: {}", partFiles[i]);
            final Dependencies currentDependencies = DependencyReaderBase.OBJECTMAPPER.readValue(partFiles[i], Dependencies.class);
            deps.add(currentDependencies);
            LOG.debug("Size: {}", deps.get(deps.size() - 1).getVersions().size());
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }

      LOG.debug("Sorting {} files", deps.size());
      deps.sort(new Comparator<Dependencies>() {

         @Override
         public int compare(final Dependencies o1, final Dependencies o2) {
            final int indexOf = VersionComparator.getVersionIndex(o1.getInitialversion().getVersion());
            final int indexOf2 = VersionComparator.getVersionIndex(o2.getInitialversion().getVersion());
            return indexOf - indexOf2;
         }
      });
      Dependencies merged;
      if (deps.size() > 1) {
         merged = DependencyReaderUtil.mergeDependencies(deps.get(0), deps.get(1));
         for (int i = 2; i < deps.size(); i++) {
            LOG.debug("Merge: {}", i);
            if (deps.get(i) != null) {
               merged = DependencyReaderUtil.mergeDependencies(merged, deps.get(i));
            }
         }
      } else {
         merged = deps.get(0);
      }

      DependencyReaderBase.OBJECTMAPPER.writeValue(out, merged);
      return merged;
   }
}
