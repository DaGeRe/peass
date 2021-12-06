package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;

public class DiffUtil {

   /**
    * Generates a diff file for two traces
    * 
    * @param traceFiles assumed order: old (0), new (1)
    */
   public static void generateDiffFile(final File goalFile, final List<File> traceFiles, final String appendix) throws IOException {
      File file1 = new File(traceFiles.get(0).getAbsolutePath() + appendix);
      File file2 = new File(traceFiles.get(1).getAbsolutePath() + appendix);

      List<String> file1text = FileUtils.readLines(file1, StandardCharsets.UTF_8)
            .stream()
            .map(line -> line.replaceAll(" ", ""))
            .collect(Collectors.toList());
      List<String> file2text = FileUtils.readLines(file2, StandardCharsets.UTF_8).stream()
            .map(line -> line.replaceAll(" ", ""))
            .collect(Collectors.toList());

      DiffRowGenerator diffRowGenerator = DiffRowGenerator.create()
            .build();

      List<DiffRow> diffRows = diffRowGenerator.generateDiffRows(file1text, file2text);

      try (final FileWriter fw = new FileWriter(goalFile)) {
         StringBuilder resultBuilder = new StringBuilder();
         for (DiffRow row : diffRows) {
            int length = 200;

            if (row.getOldLine().equals(row.getNewLine())) {
               String oldLine = fillToLength(length, row.getOldLine());
               String newLine = fillToLength(length, row.getNewLine());
               resultBuilder.append(oldLine + "   " + newLine + "\n");
            } else {
               String oldLine = fillToLength(length, row.getOldLine());
               String newLine = fillToLength(length, row.getNewLine());
               resultBuilder.append(oldLine + " | " + newLine + "\n");
            }

         }
         fw.write(resultBuilder.toString());
      }
   }

   private static String fillToLength(final int length, String oldLine) {
      if (oldLine.length() < length) {
         StringBuffer buffer = new StringBuffer();
         int missingSpaces = length - oldLine.length();
         for (int i = 0; i < missingSpaces; i++) {
            buffer.append(' ');
         }
         oldLine = oldLine + buffer.toString();
      } else {
         oldLine = oldLine.substring(0, 200);
      }
      return oldLine;
   }

   public static boolean isDifferentDiff(final File file1, final File file2) throws IOException {
      Patch<String> patch = getPatch(file1, file2);

      return patch.getDeltas().size() > 0;
   }

   private static Patch<String> getPatch(final File file1, final File file2) throws IOException {
      List<String> file1text = FileUtils.readLines(file1, StandardCharsets.UTF_8)
            .stream()
            .map(line -> line.replaceAll(" ", ""))
            .collect(Collectors.toList());
      List<String> file2text = FileUtils.readLines(file2, StandardCharsets.UTF_8).stream()
            .map(line -> line.replaceAll(" ", ""))
            .collect(Collectors.toList());
      Patch<String> patch = DiffUtils.diff(file1text, file2text);
      return patch;
   }
}
