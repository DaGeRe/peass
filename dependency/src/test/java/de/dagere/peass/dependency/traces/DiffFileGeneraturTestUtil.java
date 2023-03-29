package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;

import de.dagere.nodeDiffDetector.data.TestMethodCall;

public class DiffFileGeneraturTestUtil {
   public static TraceFileMapping generateFiles(File rawFileFolder, TestMethodCall test, String ending, boolean writeExpanded, boolean change) throws IOException {
      TraceFileMapping mapping = writeBasicFiles(rawFileFolder, test, ending, change);

      String[] variants;
      if (writeExpanded) {
         variants = new String[] { OneTraceGenerator.NOCOMMENT, OneTraceGenerator.METHOD, OneTraceGenerator.METHOD_EXPANDED, OneTraceGenerator.SUMMARY };
      } else {
         variants = new String[] { OneTraceGenerator.NOCOMMENT, OneTraceGenerator.METHOD, OneTraceGenerator.SUMMARY };
      }

      writeVariants(rawFileFolder, ending, variants, change);

      return mapping;
   }

   private static void writeVariants(File rawFileFolder, String ending, String[] variants, boolean change) throws IOException {
      for (String variant : variants) {
         File version1variantFile = new File(rawFileFolder, "version1" + variant + ending);
         write(version1variantFile, "de.dagere.peass.ExampleTest#test\nSomeSource");

         File version2variantFile = new File(rawFileFolder, "version2" + variant + ending);
         if (change) {
            write(version2variantFile, "de.dagere.peass.ExampleTest#test\nChangedSource");
         } else {
            write(version2variantFile, "de.dagere.peass.ExampleTest#test\nSomeSource");
         }

      }
   }

   private static TraceFileMapping writeBasicFiles(File rawFileFolder, TestMethodCall test, String ending, boolean change) throws IOException {
      TraceFileMapping mapping = new TraceFileMapping();

      File version1trace = new File(rawFileFolder, "version1" + ending);
      write(version1trace, "de.dagere.peass.ExampleTest#test\nSomeSource");
      mapping.addTraceFile(test, version1trace);

      File version2trace = new File(rawFileFolder, "version2" + ending);
      if (change) {
         write(version2trace, "de.dagere.peass.ExampleTest#test\nChangedSource");
      } else {
         write(version2trace, "de.dagere.peass.ExampleTest#test\nSomeSource");
      }
      mapping.addTraceFile(test, version2trace);
      return mapping;
   }

   private static void write(File goal, String content) throws IOException {
      if (goal.getName().endsWith(TraceFileManager.TXT_ENDING)) {
         FileUtils.writeStringToFile(goal, content, StandardCharsets.UTF_8);
      } else if (goal.getName().endsWith(TraceFileManager.ZIP_ENDING)) {
         ByteBuffer bytebuffer = StandardCharsets.UTF_8.encode(content);

         try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(goal));
               WritableByteChannel channel = Channels.newChannel(zipStream)) {
            ZipEntry entry = new ZipEntry("trace.txt");
            zipStream.putNextEntry(entry);
            channel.write(bytebuffer);
         }
      }
   }
}
