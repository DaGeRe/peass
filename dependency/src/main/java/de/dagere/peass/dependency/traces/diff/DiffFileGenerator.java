package de.dagere.peass.dependency.traces.diff;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
import de.dagere.peass.dependency.traces.OneTraceGenerator;
import de.dagere.peass.dependency.traces.TraceFileManager;
import de.dagere.peass.dependency.traces.TraceFileMapping;

public class DiffFileGenerator {

   private static final Logger LOG = LogManager.getLogger(DiffFileGenerator.class);

   private final File diffFolder;
   private final boolean unixDiffAvailable;

   public DiffFileGenerator(final File diffFolder) {
      this.diffFolder = diffFolder;
      this.unixDiffAvailable = DiffUtilUnix.isAvailable();
   }

   public void generateAllDiffs(final String version, final VersionStaticSelection newVersionInfo, final TraceFileMapping mapping,
         final ExecutionData executionResult) throws IOException {
      for (TestCase testcase : newVersionInfo.getTests().getTests()) {
         boolean tracesChanged = tracesChanged(testcase, mapping);
         if (tracesChanged) {
            generateDiffFiles(testcase, mapping);
            executionResult.addCall(version, testcase);
         }
      }
   }

   public boolean tracesChanged(final TestCase testcase, final TraceFileMapping traceFileMap) throws IOException {
      List<File> traceFiles = traceFileMap.getTestcaseMap(testcase);
      if (traceFiles != null) {
         LOG.debug("Trace-Files: {}", traceFiles);
         if (traceFiles.size() > 1) {
            String firstName = TraceFileUtil.getNameFromFile(traceFiles.get(0));
            File oldFile = new File(firstName + OneTraceGenerator.NOCOMMENT + TraceFileManager.TXT_ENDING);
            if (!oldFile.exists()) {
               oldFile = new File(firstName + OneTraceGenerator.NOCOMMENT + TraceFileManager.ZIP_ENDING);
            }
            String secondName = TraceFileUtil.getNameFromFile(traceFiles.get(1));
            File newFile = new File(secondName + OneTraceGenerator.NOCOMMENT + TraceFileManager.TXT_ENDING);
            if (!newFile.exists()) {
               newFile = new File(secondName + OneTraceGenerator.NOCOMMENT + TraceFileManager.ZIP_ENDING);
            }

            final boolean isDifferent;
            if (unixDiffAvailable) {
               isDifferent = DiffUtilUnix.isDifferentDiff(oldFile, newFile);
            } else {
               isDifferent = DiffUtilJava.isDifferentDiff(oldFile, newFile);
            }
            if (isDifferent) {
               LOG.info("Trace changed.");
               return true;
            } else {
               LOG.info("No change; traces equal.");
               return false;
            }
         } else {
            LOG.info("Traces not existing: {}", testcase);
            return false;
         }
      } else {
         LOG.info("Traces not existing: {}", testcase);
         return false;
      }
   }

   /**
    * Generates a human-analysable diff-file from traces
    *
    * @param testcase Name of the testcase
    * @param diffFolder Goal-folder for the diff
    * @param traceFileMap Map for place where traces are saved
    * @return Whether a change happened
    * @throws IOException If files can't be read of written
    */
   public void generateDiffFiles(final TestCase testcase, final TraceFileMapping traceFileMap) throws IOException {
      final long size = FileUtils.sizeOfDirectory(diffFolder);
      final long sizeInMB = size / (1024 * 1024);
      LOG.debug("Filesize: {} ({})", sizeInMB, size);
      List<File> traceFiles = traceFileMap.getTestcaseMap(testcase);
      createAllDiffs(testcase, traceFiles);
   }

   private void createAllDiffs(final TestCase testcase, final List<File> traceFiles) throws IOException {
      final String testcaseName = testcase.getShortClazz() + "#" + testcase.getMethod();

      File firstFile = traceFiles.get(1);
      String ending = TraceFileUtil.getEndingFromFile(firstFile);

      if (unixDiffAvailable) {
         DiffUtilUnix.generateDiffFile(new File(diffFolder, testcaseName + ending), traceFiles, "");
         DiffUtilUnix.generateDiffFile(new File(diffFolder, testcaseName + OneTraceGenerator.METHOD + ending), traceFiles, OneTraceGenerator.METHOD);
         DiffUtilUnix.generateDiffFile(new File(diffFolder, testcaseName + OneTraceGenerator.NOCOMMENT + ending), traceFiles,
               OneTraceGenerator.NOCOMMENT);
         DiffUtilUnix.generateDiffFile(new File(diffFolder, testcaseName + OneTraceGenerator.METHOD_EXPANDED + ending), traceFiles,
               OneTraceGenerator.METHOD_EXPANDED);
      } else {
         DiffUtilJava.generateDiffFile(new File(diffFolder, testcaseName + ending), traceFiles, "");
         DiffUtilJava.generateDiffFile(new File(diffFolder, testcaseName + OneTraceGenerator.METHOD + ending), traceFiles, OneTraceGenerator.METHOD);
         DiffUtilJava.generateDiffFile(new File(diffFolder, testcaseName + OneTraceGenerator.NOCOMMENT + ending), traceFiles,
               OneTraceGenerator.NOCOMMENT);
         DiffUtilJava.generateDiffFile(new File(diffFolder, testcaseName + OneTraceGenerator.METHOD_EXPANDED + ending), traceFiles,
               OneTraceGenerator.METHOD_EXPANDED);
      }

   }

}
