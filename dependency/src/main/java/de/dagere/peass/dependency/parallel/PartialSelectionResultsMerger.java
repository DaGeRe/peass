package de.dagere.peass.dependency.parallel;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.reader.DependencyParallelReader;
import de.dagere.peass.dependency.reader.DependencyReaderUtil;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;

public class PartialSelectionResultsMerger {

   private static final Logger LOG = LogManager.getLogger(DependencyParallelReader.class);

   private PartialSelectionResultsMerger() {

   }

   public static StaticTestSelection mergePartFiles(final File out, final File[] partFiles, CommitComparatorInstance comparator)
         throws IOException, JsonGenerationException, JsonMappingException {
      final List<StaticTestSelection> staticTestSelection = readStaticTestSelection(partFiles);
      StaticTestSelection merged = mergeDependencies(staticTestSelection, comparator);

      Constants.OBJECTMAPPER.writeValue(out, merged);
      return merged;
   }

   public static void mergeSelectionResults(final File out, final ResultsFolders[] partFolders, CommitComparatorInstance comparator)
         throws IOException, JsonGenerationException, JsonMappingException {
      File[] partFiles = new File[partFolders.length];
      for (int i = 0; i < partFolders.length; i++) {
         partFiles[i] = partFolders[i].getStaticTestSelectionFile();
      }
      mergePartFiles(out, partFiles, comparator);
   }

   static List<StaticTestSelection> readStaticTestSelection(final File[] partFiles) {
      final List<StaticTestSelection> staticTestSelection = new LinkedList<>();
      for (int i = 0; i < partFiles.length; i++) {
         try {
            LOG.debug("Reading: {}", partFiles[i]);
            final StaticTestSelection currentStaticSelection = Constants.OBJECTMAPPER.readValue(partFiles[i], StaticTestSelection.class);
            staticTestSelection.add(currentStaticSelection);
            LOG.debug("Size: {}", staticTestSelection.get(staticTestSelection.size() - 1).getCommits().size());
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }
      return staticTestSelection;
   }

   public static StaticTestSelection mergeDependencies(final List<StaticTestSelection> staticTestSelections, CommitComparatorInstance comparator) {
      LOG.debug("Sorting {} dependencies", staticTestSelections.size());
      staticTestSelections.sort(new Comparator<StaticTestSelection>() {
         @Override
         public int compare(final StaticTestSelection o1, final StaticTestSelection o2) {
            final int indexOf = comparator.getVersionIndex(o1.getInitialcommit().getCommit());
            final int indexOf2 = comparator.getVersionIndex(o2.getInitialcommit().getCommit());
            return indexOf - indexOf2;
         }
      });
      StaticTestSelection merged;
      if (staticTestSelections.size() > 0) {
         merged = staticTestSelections.get(0);
         if (staticTestSelections.size() > 1) {
            for (int i = 1; i < staticTestSelections.size(); i++) {
               final StaticTestSelection newMergedSelection = staticTestSelections.get(i);
               LOG.debug("Merge: {} Vals: {}", i, newMergedSelection.getCommitNames());
               if (newMergedSelection != null) {
                  merged = DependencyReaderUtil.mergeStaticSelection(merged, newMergedSelection, comparator);
               }
            }
         }
      } else {
         merged = new StaticTestSelection();
      }
      return merged;
   }

   public static ExecutionData mergeExecutiondata(final List<ExecutionData> executionData) {
      ExecutionData merged = new ExecutionData();
      for (ExecutionData data : executionData) {
         if (merged.getUrl() == null && data.getUrl() != null) {
            merged.setUrl(data.getUrl());
         }
         merged.getCommits().putAll(data.getCommits());
      }
      return merged;
   }

   public static ExecutionData mergeExecutions(final ResultsFolders mergedOut, final ResultsFolders[] outFiles) throws JsonParseException, JsonMappingException, IOException {
      List<File> executionOutFiles = new LinkedList<>();
      List<File> coverageSelectionOutFiles = new LinkedList<>();
      List<File> twiceExecutableOutFiles = new LinkedList<>();
      for (ResultsFolders resultFolder : outFiles) {
         if (resultFolder != null) {
            if (resultFolder.getTraceTestSelectionFile().exists()) {
               executionOutFiles.add(resultFolder.getTraceTestSelectionFile());
            }
            if (resultFolder.getCoverageSelectionFile() != null && resultFolder.getCoverageSelectionFile().exists()) {
               coverageSelectionOutFiles.add(resultFolder.getCoverageSelectionFile());
            }
            if (resultFolder.getTwiceExecutableFile() != null && resultFolder.getTwiceExecutableFile().exists()) {
               twiceExecutableOutFiles.add(resultFolder.getTwiceExecutableFile());
            }
         }
      }
      ExecutionData mergedExecutions = mergeExecutionFiles(executionOutFiles);
      Constants.OBJECTMAPPER.writeValue(mergedOut.getTraceTestSelectionFile(), mergedExecutions);

      if (coverageSelectionOutFiles.size() > 0) {
         ExecutionData mergedCoverage = mergeExecutionFiles(coverageSelectionOutFiles);
         Constants.OBJECTMAPPER.writeValue(mergedOut.getCoverageSelectionFile(), mergedCoverage);
      }

      if (twiceExecutableOutFiles.size() > 0) {
         ExecutionData mergedTwiceExecutable = mergeExecutionFiles(twiceExecutableOutFiles);
         Constants.OBJECTMAPPER.writeValue(mergedOut.getTwiceExecutableFile(), mergedTwiceExecutable);
      }

      return mergedExecutions;
   }

   private static ExecutionData mergeExecutionFiles(final List<File> executionOutFiles) throws IOException {
      List<ExecutionData> executionData = new LinkedList<>();
      for (File file : executionOutFiles) {
         ExecutionData currentData = Constants.OBJECTMAPPER.readValue(file, ExecutionData.class);
         executionData.add(currentData);
      }
      ExecutionData merged = mergeExecutiondata(executionData);
      return merged;
   }
}
