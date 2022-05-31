package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.traces.coverage.TraceCallSummary;
import de.dagere.peass.dependency.traces.coverage.TraceSummaryTransformer;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
import de.dagere.requitur.content.RuleContent;

public class TraceWriter {

   private static final Logger LOG = LogManager.getLogger(TraceWriter.class);

   private final String version;
   private final TestCase testcase;
   private final ResultsFolders resultsFolders;
   private final TraceFileMapping traceFileMapping;

   private final TestSelectionConfig testSelectionConfig;

   public TraceWriter(final String version, final TestCase testcase, final ResultsFolders resultsFolders, final TraceFileMapping traceFileMapping,
         TestSelectionConfig testSelectionConfig) {
      this.version = version;
      this.testcase = testcase;
      this.resultsFolders = resultsFolders;
      this.traceFileMapping = traceFileMapping;
      this.testSelectionConfig = testSelectionConfig;
   }

   public void writeTrace(final String versionCurrent, final long sizeInMB, final TraceMethodReader traceMethodReader, final TraceWithMethods trace)
         throws IOException {
      final File methodDir = resultsFolders.getViewMethodDir(version, testcase);
      String shortVersion = getShortVersion(versionCurrent);
      final File methodTrace = writeTraces(sizeInMB, traceMethodReader, trace, methodDir, shortVersion);
      LOG.debug("Datei {} existiert: {}", methodTrace.getAbsolutePath(), methodTrace.exists());
   }

   public static String getShortVersion(final String versionCurrent) {
      String shortVersion = versionCurrent.substring(0, 6);
      if (versionCurrent.endsWith("~1")) {
         shortVersion = shortVersion + "~1";
      }
      return shortVersion;
   }

   private File writeTraces(final long sizeInMB, final TraceMethodReader traceMethodReader, final TraceWithMethods trace, final File methodDir,
         final String shortVersion) throws IOException {
      TraceFileManager fileManager = new TraceFileManager(methodDir, shortVersion, testSelectionConfig);

      traceFileMapping.addTraceFile(testcase, fileManager.getWholeTraceFile());

      writeStringToFile(fileManager.getWholeTraceFile(), trace.getWholeTrace(), StandardCharsets.UTF_8);
      writeStringToFile(fileManager.getNocommentTraceFile(), trace.getNocommentTrace(), StandardCharsets.UTF_8);
      writeStringToFile(fileManager.getMethodTraceFile(), trace.getTraceMethods(), StandardCharsets.UTF_8);

      if (sizeInMB < 5) {
         writeExpandedTrace(traceMethodReader, fileManager);
      } else {
         LOG.debug("Do not write expanded trace - size: {} MB", sizeInMB);
      }
      File summaryFile = new File(methodDir, shortVersion + OneTraceGenerator.SUMMARY + ".json");
      TraceCallSummary traceSummary = TraceSummaryTransformer.transform(testcase, traceMethodReader.getExpandedTrace());
      Constants.OBJECTMAPPER.writeValue(summaryFile, traceSummary);
      return fileManager.getMethodTraceFile();
   }

   private void writeExpandedTrace(final TraceMethodReader traceMethodReader, TraceFileManager fileManager) throws IOException, FileNotFoundException {
      try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(fileManager.getMethodExpandedTraceFile()));
            WritableByteChannel channel = Channels.newChannel(zipStream)) {
         ZipEntry entry = new ZipEntry("trace.txt");
         zipStream.putNextEntry(entry);
         traceMethodReader.getExpandedTrace()
               .stream()
               .filter(value -> !(value instanceof RuleContent))
               .map(value -> value.toString())
               .forEach(line -> {
                  ByteBuffer bytebuffer = StandardCharsets.UTF_8.encode(line);
                  try {
                     channel.write(bytebuffer);
                  } catch (IOException e) {
                     e.printStackTrace();
                  }
               });
      }
   }

   public void writeStringToFile(final File goalFile, final String trace, final Charset charset) throws FileNotFoundException, IOException {
      if (goalFile.getName().endsWith(TraceFileManager.ZIP_ENDING)) {
         ByteBuffer bytebuffer = charset.encode(trace);

         try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(goalFile));
               WritableByteChannel channel = Channels.newChannel(zipStream)) {
            ZipEntry entry = new ZipEntry("trace.txt");
            zipStream.putNextEntry(entry);
            channel.write(bytebuffer);
         }
      } else {
         ByteBuffer bytebuffer = charset.encode(trace);

         try (FileOutputStream outputStream = new FileOutputStream(goalFile);
               WritableByteChannel channel = Channels.newChannel(outputStream)) {
            channel.write(bytebuffer);
         }
      }
   }
}
