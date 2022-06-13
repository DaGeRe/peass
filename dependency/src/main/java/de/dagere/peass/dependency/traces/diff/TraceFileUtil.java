package de.dagere.peass.dependency.traces.diff;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.dependency.traces.TraceFileManager;

/**
 * This provides utilities for handling trace files. By convention, trace files ending with .txt are text files and .zip are zip files with exactly one file which is called
 * trace.txt. If the .zip file is extracted, the trace.txt should be renamed to the original name.
 * 
 * @author DaGeRe
 *
 */
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

   public static File unzip(File zipFile) {
      try {
         byte[] buffer = new byte[1024];
         ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));

         ZipEntry zipEntry = zis.getNextEntry();

         File destDir = zipFile.getParentFile();
         File txtTraceFile = new File(destDir, zipFile.getName().replace(TraceFileManager.ZIP_ENDING, TraceFileManager.TXT_ENDING));

         // write file content
         FileOutputStream fos = new FileOutputStream(txtTraceFile);
         int len;
         while ((len = zis.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
         }
         fos.close();

         zis.closeEntry();
         zis.close();

         return txtTraceFile;
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public static void writeZippedOutput(final File goalFile, String result) throws IOException, FileNotFoundException {
      try (final FileWriter fw = new FileWriter(goalFile)) {

         try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(goalFile));
               WritableByteChannel channel = Channels.newChannel(zipStream)) {
            ZipEntry entry = new ZipEntry("trace.txt");
            zipStream.putNextEntry(entry);

            // System.out.println(result);
            ByteBuffer bytebuffer = StandardCharsets.UTF_8.encode(result);
            channel.write(bytebuffer);
         }
      }
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
