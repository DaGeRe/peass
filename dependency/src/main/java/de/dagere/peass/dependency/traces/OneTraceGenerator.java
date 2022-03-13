package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import de.dagere.peass.statisticlogger.ExceededTraceLogger;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.analysis.CalledMethodLoader;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TraceElement;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;

public class OneTraceGenerator {

   static final String METHOD = "_method";
   static final String METHOD_EXPANDED = "_method_expanded";
   public static final String NOCOMMENT = "_nocomment";
   public static final String SUMMARY = "_summary";

   private static final Logger LOG = LogManager.getLogger(OneTraceGenerator.class);

   private final PeassFolders folders;
   private final TestCase testcase;
   private final TraceFileMapping traceFileMapping;
   private final String version;
   private final ResultsFolders resultsFolders;
   private final List<File> classpathFolders;
   private final ModuleClassMapping moduleClassMapping;
   private final KiekerConfig kiekerConfig;

   public OneTraceGenerator(final ResultsFolders resultsFolders, final PeassFolders folders, final TestCase testcase, final TraceFileMapping traceFileMapping, final String version,
         final List<File> classpathFolders, final ModuleClassMapping mapping, final KiekerConfig kiekerConfig) {
      this.resultsFolders = resultsFolders;
      this.folders = folders;
      this.testcase = testcase;
      this.traceFileMapping = traceFileMapping;
      this.version = version;
      this.classpathFolders = classpathFolders;
      this.moduleClassMapping = mapping;
      this.kiekerConfig = kiekerConfig;
   }

   public boolean generateTrace(final String versionCurrent)
         throws com.github.javaparser.ParseException, IOException, XmlPullParserException {
      boolean success = false;
      try {
         final File moduleResultsFolder = KiekerFolderUtil.getModuleResultFolder(folders, testcase);
         final File[] kiekerResultFolders = KiekerFolderUtil.getClazzMethodFolder(testcase, moduleResultsFolder);
         LOG.debug("Searching for: {}", kiekerResultFolders[0]);
         if (kiekerResultFolders[0].exists() && kiekerResultFolders[0].isDirectory()) {
            success = generateTraceFiles(versionCurrent, kiekerResultFolders);
         } else {
            LOG.error("Error: {} does not produce {}", versionCurrent, kiekerResultFolders[0].getAbsolutePath());
         }
      } catch (final RuntimeException e) {
         e.printStackTrace();
      }
      return success;
   }

   private boolean generateTraceFiles(final String versionCurrent, final File[] kiekerResultFolders)
         throws FileNotFoundException, IOException, XmlPullParserException, com.github.javaparser.ParseException {
      boolean success = false;
      TraceWithMethods trace = null;
      TraceMethodReader traceMethodReader = null;
      int overallSizeInMb = 0;
      for (File kiekerResultFolder : kiekerResultFolders) {
         final long size = FileUtils.sizeOfDirectory(kiekerResultFolder);
         final long sizeInMB = size / (1024 * 1024);
         overallSizeInMb += sizeInMB;
         LOG.debug("Filesize: {} ({})", sizeInMB, size);
         if (sizeInMB < kiekerConfig.getTraceSizeInMb()) {
            CalledMethodLoader calledMethodLoader = new CalledMethodLoader(kiekerResultFolder, moduleClassMapping, kiekerConfig);
            final List<TraceElement> shortTrace = calledMethodLoader.getShortTrace("");
            if (shortTrace != null) {
               LOG.debug("Short Trace: {} Folder: {} Project: {}", shortTrace.size(), kiekerResultFolder.getAbsolutePath(), folders.getProjectFolder());
               if (shortTrace.size() > 0) {
                  traceMethodReader = new TraceMethodReader(shortTrace, classpathFolders.toArray(new File[0]));
                  if (trace == null) {
                     trace = traceMethodReader.getTraceWithMethods();
                  } else {
                     TraceWithMethods additionalTrace = traceMethodReader.getTraceWithMethods();
                     trace.append(additionalTrace);
                  }

                  success = true;
               } else {
                  LOG.error("Trace empty!");
               }
            }
         } else {
            LOG.error("File size exceeds {} MB", kiekerConfig.getTraceSizeInMb());
         }
      }
      if (success) {
         writeTrace(versionCurrent, overallSizeInMb, traceMethodReader, trace);
      }
      return success;
   }

   private void writeTrace(final String versionCurrent, final long sizeInMB, final TraceMethodReader traceMethodReader, final TraceWithMethods trace) throws IOException {
      TraceWriter traceWriter = new TraceWriter(version, testcase, resultsFolders, traceFileMapping);
      traceWriter.writeTrace(versionCurrent, sizeInMB, traceMethodReader, trace);
   }
}
