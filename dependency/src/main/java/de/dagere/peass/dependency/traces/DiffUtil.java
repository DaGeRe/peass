package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;

public class DiffUtil {

   private static final Logger LOG = LogManager.getLogger(DiffUtil.class);

   /**
    * Generates a diff file for two traces
    * 
    * @param traceFiles assumed order: old (0), new (1)
    */
   public static void generateDiffFile(final File goalFile, final List<File> traceFiles, final String appendix) throws IOException {
      String ending = goalFile.getName().substring(goalFile.getName().length() - 4);
      File file1 = new File(DiffFileGenerator.getNameFromFile(traceFiles.get(0)) + appendix + ending);
      File file2 = new File(DiffFileGenerator.getNameFromFile(traceFiles.get(1)) + appendix + ending);

      if (file1.exists() && file2.exists()) {
         int length = 100;

         final List<String> file1text = getText(file1);
         final List<String> file2text = getText(file2);

         DiffRowGenerator diffRowGenerator = DiffRowGenerator.create()
               .build();

         final List<DiffRow> diffRows = diffRowGenerator.generateDiffRows(file1text, file2text);

         writeDiff(goalFile, length, diffRows);
      } else {
         LOG.error("Not both log files {} ({}) {} ({}) existed. ", file1, file1.exists(), file2, file2.exists());
      }

   }

   private static void writeDiff(final File goalFile, int length, final List<DiffRow> diffRows) throws IOException {
      StringBuilder resultBuilder = getDiff(length, diffRows);

      String result = resultBuilder.toString();
      if (goalFile.getName().endsWith(TraceFileManager.ZIP_ENDING)) {
         try (final FileWriter fw = new FileWriter(goalFile)) {

            try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(goalFile));
                  WritableByteChannel channel = Channels.newChannel(zipStream)) {
               ZipEntry entry = new ZipEntry("trace.txt");
               zipStream.putNextEntry(entry);

               System.out.println(result);
               ByteBuffer bytebuffer = StandardCharsets.UTF_8.encode(result);
               channel.write(bytebuffer);
            }
         }
      } else {
         try (final FileWriter fw = new FileWriter(goalFile)) {
            fw.write(result);
         }
      }
   }

   private static StringBuilder getDiff(int length, final List<DiffRow> diffRows) {
      StringBuilder resultBuilder = new StringBuilder();
      for (DiffRow row : diffRows) {
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
      return resultBuilder;
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
         oldLine = oldLine.substring(0, length);
      }
      return oldLine;
   }

   public static boolean isDifferentDiff(final File file1, final File file2) throws IOException {
      Patch<String> patch = getPatch(file1, file2);

      return patch.getDeltas().size() > 0;
   }

   private static Patch<String> getPatch(final File file1, final File file2) throws IOException {
      List<String> file1text = getText(file1);
      List<String> file2text = getText(file2);
      Patch<String> patch = DiffUtils.diff(file1text, file2text);
      return patch;
   }

   public static List<String> getText(File file) throws IOException {
      if (file.getName().endsWith(TraceFileManager.TXT_ENDING)) {
         List<String> filetext = FileUtils.readLines(file, StandardCharsets.UTF_8)
               .stream()
               .map(line -> line.trim())
               .collect(Collectors.toList());
         return filetext;
      } else if (file.getName().endsWith(TraceFileManager.ZIP_ENDING)) {
         try (InputStream input = new FileInputStream(file)) {
            ZipInputStream zip = new ZipInputStream(input);
            ZipEntry entry = zip.getNextEntry();

            List<String> lines = new LinkedList<>();
            Scanner sc = new Scanner(zip);
            while (sc.hasNextLine()) {
               lines.add(sc.nextLine());
            }
            return lines;
         }
      } else {
         List<String> filetext = FileUtils.readLines(file, StandardCharsets.UTF_8)
               .stream()
               .map(line -> line.trim())
               .collect(Collectors.toList());
         return filetext;
      }
   }
}
