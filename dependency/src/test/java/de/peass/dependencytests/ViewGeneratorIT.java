package de.peass.dependencytests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Test;

import com.github.javaparser.ParseException;

import de.peass.dependency.KiekerResultManager;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.CalledMethodLoader;
import de.peass.dependency.analysis.ModuleClassMapping;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.analysis.data.TraceElement;
import de.peass.dependency.traces.KiekerFolderUtil;
import de.peass.dependency.traces.TraceMethodReader;
import de.peass.dependency.traces.TraceWithMethods;
import de.peass.dependencyprocessors.ViewNotFoundException;

public class ViewGeneratorIT {

   private static final Logger LOG = LogManager.getLogger(ViewGeneratorIT.class);

   public static final File VIEW_IT = new File("target", "view_it");
   public static final File BASIC = new File("src/test/resources/viewtests/basic");
   public static final File REPETITION = new File("src/test/resources/viewtests/repetition");
   private static final File REPETITION_MULTIPLE = new File("src/test/resources/viewtests/repetition_multiple");
   private static final File REPETITION_DEEP = new File("src/test/resources/viewtests/repetition_deep");
   private static final File REPETITION_REPETITION = new File("src/test/resources/viewtests/repetition_of_repetition");

   public static final File projectFolder = new File(VIEW_IT, "current");
   private static final File viewFolder = new File(VIEW_IT, "views");

   public static void init(final File folder) {
      try {
         if (!VIEW_IT.exists()) {
            VIEW_IT.mkdirs();
         }
         if (!viewFolder.exists()) {
            viewFolder.mkdir();
         }
         FileUtils.deleteDirectory(projectFolder);
         FileUtils.deleteDirectory(new File(projectFolder.getParentFile(), projectFolder.getName() + "_peass"));
         FileUtils.copyDirectory(folder, projectFolder);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   @Test
   public void testBasicView() throws ParseException, IOException, ViewNotFoundException, XmlPullParserException, InterruptedException {
      final File project = BASIC;
      final String githash = "1";
      executeTraceGetting(project, githash);
      // TODO Test Method-Source Compliance -> _method-file should contain same methods as file with source
   }

   @Test
   public void testRepetition() throws ParseException, IOException, ViewNotFoundException, XmlPullParserException, InterruptedException {
      final File project = REPETITION;
      final String githash = "2";
      executeTraceGetting(project, githash);
   }

   @Test
   public void testMultipleRepetition() throws ParseException, IOException, ViewNotFoundException, XmlPullParserException, InterruptedException {
      final File project = REPETITION_MULTIPLE;
      final String githash = "3";
      executeTraceGetting(project, githash);
   }

   @Test
   public void testDeepRepetition() throws ParseException, IOException, ViewNotFoundException, XmlPullParserException, InterruptedException {
      final File project = REPETITION_DEEP;
      final String githash = "4";
      executeTraceGetting(project, githash);
   }

   @Test
   public void testRepetitionRepetition() throws ParseException, IOException, ViewNotFoundException, XmlPullParserException, InterruptedException {
      final File project = REPETITION_REPETITION;
      final String githash = "5";
      executeTraceGetting(project, githash);

      final File viewFile = new File(viewFolder, "test_hash_5_method");

      final List<String> expectedCalls = new LinkedList<>();
      expectedCalls.add("viewtest.TestMe#test");
      expectedCalls.add("viewtest.TestMe$InnerClass#<init>([viewtest.TestMe])");
      expectedCalls.add("5x(2)");
      expectedCalls.add("viewtest.TestMe$InnerClass#method");
      expectedCalls.add("viewtest.TestMe#staticMethod");
      expectedCalls.add("2x(4)");
      expectedCalls.add("viewtest.TestMe#staticMethod");
      expectedCalls.add("5x(2)");
      expectedCalls.add("viewtest.TestMe$InnerClass#method");
      expectedCalls.add("viewtest.TestMe#staticMethod");
      expectedCalls.add("viewtest.TestMe#staticMethod");

      try (BufferedReader reader = new BufferedReader(new FileReader(viewFile))) {
         String line;
         while ((line = reader.readLine()) != null) {
            Assert.assertEquals(expectedCalls.remove(0), line.replaceAll(" ", ""));
         }
      }
   }

   private void executeTraceGetting(final File project, final String githash)
         throws IOException, ParseException, ViewNotFoundException, XmlPullParserException, InterruptedException {
      init(project);
      final KiekerResultManager tracereader = new KiekerResultManager(new PeASSFolders(projectFolder), 5000);
      final TestSet testset = new TestSet();
      testset.addTest(new TestCase("viewtest.TestMe", "test", ""));
      tracereader.getExecutor().loadClasses();
      tracereader.executeKoPeMeKiekerRun(testset, "1");

      LOG.debug("Trace-Analysis..");

      final boolean worked = analyseTrace(new TestCase("viewtest.TestMe", "test", ""), viewFolder, new HashMap<>(), githash, tracereader.getXMLFileFolder(projectFolder));
      Assert.assertEquals(true, worked);

      tracereader.deleteTempFiles();
   }

   public static boolean analyseTrace(final TestCase testcase, final File clazzDir, final Map<String, List<File>> traceFileMap, final String githash, final File resultsFolder)
         throws com.github.javaparser.ParseException, IOException, ViewNotFoundException {
      final File kiekerResultFolder = KiekerFolderUtil.getClazzMethodFolder(testcase, resultsFolder);
      // return kiekerResultFolder;
      // final File kiekerResultFolder = getMethodFolder(testcase, resultsFolder);

      boolean success = false;
      final long size = FileUtils.sizeOfDirectory(kiekerResultFolder.getParentFile());
      final long sizeInMB = size / (1024 * 1024);
      LOG.debug("Filesize: {} ({})", sizeInMB, size);
      if (sizeInMB < 2000) {

         executeReading(testcase, clazzDir, traceFileMap, githash, kiekerResultFolder);
         success = true;
      } else {
         LOG.error("File size exceeds 2000 MB");
      }
      FileUtils.deleteDirectory(resultsFolder);
      return success;
   }

   public static void executeReading(final TestCase testcase, final File clazzDir, final Map<String, List<File>> traceFileMap, final String githash, final File kiekerResultFolder)
         throws ParseException, IOException {
      final ArrayList<TraceElement> shortTrace = new CalledMethodLoader(kiekerResultFolder, ModuleClassMapping.SINGLE_MODULE_MAPPING).getShortTrace("");
      LOG.debug("Short Trace: {}", shortTrace.size());
      final TraceMethodReader traceMethodReader = new TraceMethodReader(shortTrace,
            new File(projectFolder, "src/main/java"), new File(projectFolder, "src/java"),
            new File(projectFolder, "src/test/java"), new File(projectFolder, "src/test"));
      final TraceWithMethods trace = traceMethodReader.getTraceWithMethods();
      List<File> traceFile = traceFileMap.get(testcase.getMethod());
      if (traceFile == null) {
         traceFile = new LinkedList<>();
         traceFileMap.put(testcase.getMethod(), traceFile);
      }
      final File currentTraceFile = new File(clazzDir, testcase.getMethod() + "_hash_" + githash);
      traceFile.add(currentTraceFile);
      try (final FileWriter fw = new FileWriter(currentTraceFile)) {
         fw.write(trace.getWholeTrace());
      }
      final File methodTrace = new File(clazzDir, testcase.getMethod() + "_hash_" + githash + "_method");
      try (final FileWriter fw = new FileWriter(methodTrace)) {
         LOG.debug("Methoden: " + trace.getTraceMethods().length());
         fw.write(trace.getTraceMethods());
      }
   }

   public static File getMethodFolder(final TestCase testcase, final File xmlFileFolder) throws ViewNotFoundException {
      final File kiekerResultFolder = KiekerFolderUtil.getClazzMethodFolder(testcase, xmlFileFolder);
//      final File[] possiblyMethodFolder = methodResult.listFiles();
//      final File kiekerResultFolder = possiblyMethodFolder[0];
      return kiekerResultFolder;
   }
}
