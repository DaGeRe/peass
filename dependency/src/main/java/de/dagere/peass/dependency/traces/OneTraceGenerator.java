package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.dependency.analysis.CalledMethodLoader;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TraceElement;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;

public class OneTraceGenerator {

   public static final String METHOD = "_method";
   public static final String METHOD_EXPANDED = "_method_expanded";
   public static final String NOCOMMENT = "_nocomment";
   public static final String SUMMARY = "_summary";

   private static final Logger LOG = LogManager.getLogger(OneTraceGenerator.class);

   private final PeassFolders folders;
   private final TestMethodCall testcase;
   private final TraceFileMapping traceFileMapping;
   private final String commit;
   private final ResultsFolders resultsFolders;
   private final List<File> classpathFolders;
   private final ModuleClassMapping moduleClassMapping;
   private final KiekerConfig kiekerConfig;
   private final TestSelectionConfig testSelectionConfig;

   public OneTraceGenerator(final ResultsFolders resultsFolders, final PeassFolders folders, final TestMethodCall testcase, final TraceFileMapping traceFileMapping, final String commit,
         final List<File> classpathFolders, final ModuleClassMapping mapping, final KiekerConfig kiekerConfig, TestSelectionConfig testSelectionConfig) {
      this.resultsFolders = resultsFolders;
      this.folders = folders;
      this.testcase = testcase;
      this.traceFileMapping = traceFileMapping;
      this.commit = commit;
      this.classpathFolders = classpathFolders;
      this.moduleClassMapping = mapping;

      this.kiekerConfig = kiekerConfig;
      this.testSelectionConfig = testSelectionConfig;
   }

   public void generateTrace(final String commitCurrent) {
      try {
         final File moduleResultsFolder = KiekerFolderUtil.getModuleResultFolder(folders, testcase);
         final File[] kiekerResultFolders = KiekerFolderUtil.getClazzMethodFolder(testcase, moduleResultsFolder);

         final File testclazzResultFolder = new File(moduleResultsFolder, testcase.getClazz());
         File methodJSON = new File(testclazzResultFolder, testcase.getMethodWithParams() + ".json");
         Kopemedata data = Constants.OBJECTMAPPER.readValue(methodJSON, Kopemedata.class);
         if (data.getFirstResult().isError()) {
            LOG.error("Testcase {} contained an error; not creating trace", testcase);
         } else {
            LOG.debug("Searching for: {}", kiekerResultFolders[0]);
            if (kiekerResultFolders[0].exists() && kiekerResultFolders[0].isDirectory()) {
               generateTraceFiles(commitCurrent, kiekerResultFolders);
            } else {
               LOG.error("Error: {} does not produce {}", commitCurrent, kiekerResultFolders[0].getAbsolutePath());
            }
         }
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   private void generateTraceFiles(final String commitCurrent, final File[] kiekerResultFolders)
         throws FileNotFoundException, IOException {
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
                  LOG.warn("Trace is empty! (Which is ok for first execution of a parameterized test.)");
               }
            }
         } else {
            LOG.error("File size exceeds {} MB", kiekerConfig.getTraceSizeInMb());
            FileUtils.deleteDirectory(kiekerResultFolder);
         }
      }
      if (success) {
         writeTrace(commitCurrent, overallSizeInMb, traceMethodReader, trace);
      }
   }

   private void writeTrace(final String versionCurrent, final long sizeInMB, final TraceMethodReader traceMethodReader, final TraceWithMethods trace) throws IOException {
      TraceWriter traceWriter = new TraceWriter(commit, testcase, resultsFolders, traceFileMapping, testSelectionConfig);
      traceWriter.writeTrace(versionCurrent, sizeInMB, traceMethodReader, trace);
   }
}
