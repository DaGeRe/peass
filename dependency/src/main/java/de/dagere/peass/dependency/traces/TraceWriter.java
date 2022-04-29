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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

   public TraceWriter(final String version, final TestCase testcase, final ResultsFolders resultsFolders, final TraceFileMapping traceFileMapping) {
      this.version = version;
      this.testcase = testcase;
      this.resultsFolders = resultsFolders;
      this.traceFileMapping = traceFileMapping;
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
      final File currentTraceFile = new File(methodDir, shortVersion);
      traceFileMapping.addTraceFile(testcase, currentTraceFile);
      //writeStringToFile(currentTraceFile, trace.getWholeTrace(), StandardCharsets.UTF_8);
      final File commentlessTraceFile = new File(methodDir, shortVersion + OneTraceGenerator.NOCOMMENT);
      writeStringToFile(commentlessTraceFile, trace.getCommentlessTrace(), StandardCharsets.UTF_8);
      final File methodTrace = new File(methodDir, shortVersion + OneTraceGenerator.METHOD);
      writeStringToFile(methodTrace, trace.getTraceMethods(), StandardCharsets.UTF_8);
      if (sizeInMB < 5) {
         final File methodExpandedTrace = new File(methodDir, shortVersion + OneTraceGenerator.METHOD_EXPANDED);
         Files.write(methodExpandedTrace.toPath(), traceMethodReader.getExpandedTrace()
               .stream()
               .filter(value -> !(value instanceof RuleContent))
               .map(value -> value.toString()).collect(Collectors.toList()));
      } else {
         LOG.debug("Do not write expanded trace - size: {} MB", sizeInMB);
      }
      File summaryFile = new File(methodDir, shortVersion + OneTraceGenerator.SUMMARY);
      TraceCallSummary traceSummary = TraceSummaryTransformer.transform(testcase, traceMethodReader.getExpandedTrace());
      Constants.OBJECTMAPPER.writeValue(summaryFile, traceSummary);
      return methodTrace;
   }

   public void writeStringToFile(final File goalFile, final String trace, final Charset charset) throws FileNotFoundException, IOException {
      ByteBuffer bytebuffer = charset.encode(trace);

      try (FileOutputStream outputStream = new FileOutputStream(goalFile);
            WritableByteChannel channel = Channels.newChannel(outputStream)) {
         channel.write(bytebuffer);
      }
   }
}
