package de.dagere.peass.dependency.traces.diff;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import de.dagere.peass.utils.StreamGobbler;

public class DiffUtilUnix {

   /**
    * Generates a diff file for two traces
    * 
    * @param traceFiles assumed order: old (0), new (1)
    */
   public static void generateDiffFile(final File goalFile, final List<File> traceFiles, final String appendix) throws IOException {
      String ending = TraceFileUtil.getEndingFromFile(goalFile);
      File file1 = new File(TraceFileUtil.getNameFromFile(traceFiles.get(0)) + appendix + ending);
      File file2 = new File(TraceFileUtil.getNameFromFile(traceFiles.get(1)) + appendix + ending);
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
