package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import de.dagere.peass.utils.StreamGobbler;

public class DiffUtil {
   
   /**
    * Generates a diff file for two traces
    * @param traceFiles assumed order: old (0), new (1)
    */
   public static void generateDiffFile(final File goalFile, final List<File> traceFiles, final String appendix) throws IOException {
      final ProcessBuilder processBuilder2 = new ProcessBuilder("diff",
            "--minimal", "--ignore-all-space", "-y", "-W", "200",
            traceFiles.get(0).getAbsolutePath() + appendix,
            traceFiles.get(1).getAbsolutePath() + appendix);
      final Process p2 = processBuilder2.start();
      final String result2 = StreamGobbler.getFullProcess(p2, false);
      try (final FileWriter fw = new FileWriter(goalFile)) {
         fw.write(result2);
      }
   }
}
