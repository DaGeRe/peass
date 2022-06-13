package de.dagere.peass.dependency.traces.diff;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.dependency.traces.TraceFileManager;
import de.dagere.peass.utils.StreamGobbler;

public class DiffUtilUnix {

   /**
    * Generates a diff file for two traces
    * 
    * @param traceFiles assumed order: old (0), new (1)
    */
   public static void generateDiffFile(final File goalFile, final List<File> traceFiles, final String appendix) throws IOException {
      String ending = TraceFileUtil.getEndingFromFile(goalFile);

      if (TraceFileManager.TXT_ENDING.equals(ending)) {
         File file1 = new File(TraceFileUtil.getNameFromFile(traceFiles.get(0)) + appendix + ending);
         File file2 = new File(TraceFileUtil.getNameFromFile(traceFiles.get(1)) + appendix + ending);
         generateDiff(goalFile, file1, file2);
      } else if (TraceFileManager.ZIP_ENDING.equals(ending)) {
         File zipFile1 = new File(TraceFileUtil.getNameFromFile(traceFiles.get(0)) + appendix + ending);
         File zipFile2 = new File(TraceFileUtil.getNameFromFile(traceFiles.get(1)) + appendix + ending);
         
         if (zipFile1.exists() && zipFile2.exists()) {
            File file1 = TraceFileUtil.unzip(zipFile1);
            File file2 = TraceFileUtil.unzip(zipFile2);
            
            File interimGoalFile = new File(TraceFileUtil.getNameFromFile(goalFile) + TraceFileManager.TXT_ENDING);
            
            generateDiff(interimGoalFile, file1, file2);
            
            file1.delete();
            file2.delete();
            
            String diff = FileUtils.readFileToString(interimGoalFile, StandardCharsets.UTF_8);
            File finalGoalFile = new File(TraceFileUtil.getNameFromFile(goalFile) + TraceFileManager.ZIP_ENDING);
            TraceFileUtil.writeZippedOutput(finalGoalFile, diff);
            interimGoalFile.delete();
         }
      }
   }

   private static void generateDiff(final File goalFile, File file1, File file2) throws IOException {
      final ProcessBuilder processBuilder2 = new ProcessBuilder("diff",
            "--minimal", "--ignore-all-space", "-y", "-W", "200",
            file1.getAbsolutePath(),
            file2.getAbsolutePath());
      final Process p2 = processBuilder2.start();
      final String result2 = StreamGobbler.getFullProcess(p2, false);

      try {
         int exitCode = p2.waitFor();
         if (exitCode > 1) {
            throw new RuntimeException("diff did not work correctly " + result2);
         }
      } catch (InterruptedException e) {
         throw new RuntimeException(e);
      }

      try (final FileWriter fw = new FileWriter(goalFile)) {
         fw.write(result2);
      }
   }

   public static String getDiff(final File file1, final File file2) {
      try {
         final ProcessBuilder processBuilder2 = new ProcessBuilder("diff",
               "--ignore-all-space",
               file1.getAbsolutePath(),
               file2.getAbsolutePath());
         Process checkDiff = processBuilder2.start();
         final String isDifferent = StreamGobbler.getFullProcess(checkDiff, false);
         return isDifferent;
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public static boolean isAvailable() {
      try {
         final ProcessBuilder processBuilder = new ProcessBuilder("diff", "--version");
         Process process = processBuilder.start();
         StreamGobbler.showFullProcess(process);
         int returnCode = process.waitFor();
         return returnCode == 0;
      } catch (IOException | InterruptedException e) {
         throw new RuntimeException(e);
      }
   }

   public static boolean isDifferentDiff(File oldFile, File newFile) {
      return getDiff(oldFile, newFile).length() > 0;
   }
}
