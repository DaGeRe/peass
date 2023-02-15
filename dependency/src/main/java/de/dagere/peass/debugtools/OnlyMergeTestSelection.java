package de.dagere.peass.debugtools;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import de.dagere.peass.config.parameters.ExecutionConfigMixin;
import de.dagere.peass.config.parameters.TestSelectionConfigMixin;
import de.dagere.peass.dependency.parallel.PartialSelectionResultsMerger;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.vcs.CommitUtil;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * This merges all test selection data, if they are in different folders (e.g. results-0, results-1, results-2) and writes the merged data
 * into the folder given by -mergedFolder. A project folder with the commits needs to be passed as well.
 * @author DaGeRe
 *
 */
public class OnlyMergeTestSelection implements Callable<Void> {

   @Mixin
   private TestSelectionConfigMixin config;

   @Mixin
   private ExecutionConfigMixin executionConfigMixin;

   @Option(names = { "-baseFolder", "--baseFolder" }, description = "Folder of the results that should be merged", required = true)
   private File baseFolder;

   @Option(names = { "-mergedFolder", "--mergedFolder" }, description = "Folder of the merged results", required = true)
   private File mergedFolder;

   public static void main(final String[] args) {
      try {
         final CommandLine commandLine = new CommandLine(new OnlyMergeTestSelection());
         commandLine.execute(args);
      } catch (final Throwable t) {
         t.printStackTrace();
      }
   }

   @Override
   public Void call() throws Exception {
      final File projectFolder = config.getProjectFolder();

      final List<String> commits = CommitUtil.getGitCommits(executionConfigMixin.getStartcommit(), executionConfigMixin.getEndcommit(), projectFolder,
            executionConfigMixin.isLinearizeHistory());
      CommitComparatorInstance instance = new CommitComparatorInstance(commits);

      if (baseFolder == null) {
         final File[] files = config.getResultBaseFolder().listFiles((FilenameFilter) new WildcardFileFilter("staticTestSelection_*.json"));
         PartialSelectionResultsMerger.mergePartFiles(new File(config.getResultBaseFolder(), "staticTestSelection_merged.json"), files, instance);
      } else {
         File[] resultsFolders = findResultsFolder();
         ResultsFolders[] outFiles = new ResultsFolders[resultsFolders.length];
         for (int i = 0; i < outFiles.length; i++) {
            outFiles[i] = new ResultsFolders(resultsFolders[i], projectFolder.getName());
         }
         
         ResultsFolders mergedFolders = new ResultsFolders(mergedFolder, projectFolder.getName());

         PartialSelectionResultsMerger.mergePartialData(instance, outFiles, mergedFolders);
      }

      return null;
   }

   private File[] findResultsFolder() {
      File[] resultsFolders = baseFolder.listFiles();
      List<File> result = new ArrayList<>();
      for (int i = 0; i < resultsFolders.length; i++) {
         if (resultsFolders[i].isDirectory()) {
            try (Stream<Path> path = Files.walk(resultsFolders[i].toPath())) {
               path.filter(f -> f.getFileName().toString().startsWith("staticTestSelection")).forEach(f -> result.add(f.getParent().toFile()));
            } catch (IOException e) {
               throw new RuntimeException("staticTestSelection was not found in result folder", e);
            }
         }
      }
      return result.toArray(new File[result.size()]);
   }
}
