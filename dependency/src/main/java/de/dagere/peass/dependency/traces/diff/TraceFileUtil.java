package de.dagere.peass.dependency.traces.diff;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.dependency.traces.TraceFileManager;

public class TraceFileUtil {
   public static String getEndingFromFile(File firstFile) {
      String ending;
      if (firstFile.getName().endsWith(TraceFileManager.TXT_ENDING)) {
         ending = TraceFileManager.TXT_ENDING;
      } else if (firstFile.getName().endsWith(TraceFileManager.ZIP_ENDING)) {
         ending = TraceFileManager.ZIP_ENDING;
      } else {
         throw new RuntimeException("Unexpected ending: " + firstFile);
      }
      return ending;
   }
   
   public static String getNameFromFile(File file) {
      return file.getAbsolutePath()
            .replace(TraceFileManager.TXT_ENDING, "")
            .replace(TraceFileManager.ZIP_ENDING, "");
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

            List<String> lines = new ArrayList<>();
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
