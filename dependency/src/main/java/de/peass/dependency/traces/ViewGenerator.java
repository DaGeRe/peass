package de.peass.dependency.traces;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.DependencyReadingStarter;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.TestResultManager;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependency.persistence.Version;
import de.peass.dependency.reader.DependencyReaderBase;
import de.peass.dependencyprocessors.PairProcessor;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.utils.OptionConstants;
import de.peass.utils.StreamGobbler;
import de.peass.vcs.GitUtils;

public class ViewGenerator extends PairProcessor {

   private static final Logger LOG = LogManager.getLogger(ViewGenerator.class);

   private final File viewFolder;
   private final File executeFile;
   private final ExecutionData changedTraceMethods = new ExecutionData();
   // private final TestResultManager resultsManager;

   public ViewGenerator(final File projectFolder, final Dependencies dependencies, final File executefile, final File viewFolder, final int threads, final int timeout) {
      super(projectFolder, dependencies, timeout);
      this.viewFolder = viewFolder;
      this.executeFile = executefile;
      this.threads = threads;
      processInitialVersion(dependencies.getInitialversion());
      changedTraceMethods.setAndroid(dependencies.isAndroid());
      init();
   }

   //
   public ViewGenerator(final String[] args) throws ParseException, JAXBException, JsonParseException, JsonMappingException, IOException {
      super(args);
      final File resultFolder = DependencyReadingStarter.getResultFolder();
      final String projectName = folders.getProjectFolder().getName();
      init();

      viewFolder = new File(resultFolder, "views_" + projectName);
      if (!viewFolder.exists()) {
         viewFolder.mkdir();
      }
      if (line.hasOption(OptionConstants.OUT.getName())) {
         executeFile = new File(line.getOptionValue(OptionConstants.OUT.getName()));
      } else {
         executeFile = new File(viewFolder, "execute-" + projectName + ".json");
      }
   }

   public void init() {
      final String url = GitUtils.getURL(folders.getProjectFolder());
      changedTraceMethods.setUrl(url);
   }

   public void processVersion(final String version, final Version versioninfo, final ExecutorService threads) {
      LOG.info("View-Generation for Version {}", version);
      final Set<TestCase> testcases = versioninfo.getTests().getTests();

      final boolean beforeEndVersion = endversion == null || version.equals(endversion) || VersionComparator.isBefore(version, endversion);
      LOG.debug("Before End Version {}: {}", endversion, beforeEndVersion);

      final TestSet tests = new TestSet();
      for (final TestCase testcase : testcases) {
         if (!VersionComparator.isBefore(version, startversion) && beforeEndVersion) {
            if (lastTestcaseCalls.containsKey(testcase)) {
               tests.addTest(testcase);
            }
         }
         lastTestcaseCalls.put(testcase, version);
      }
      if (!tests.getTestcases().isEmpty()) {
         // int index= VersionComparator.getVersionIndex(versioninfo.getVersion());
         final String predecessor = getRunningPredecessor(version);
         final Runnable currentVersionAnalyser = createGeneratorRunnable(version, predecessor, tests);
         threads.submit(currentVersionAnalyser);
      }
   }

   private String getRunningPredecessor(final String version) {
      String predecessor = VersionComparator.getPreviousVersion(version);
      boolean running = isVersionRunning(predecessor);
      if (running) {
         return version + "~1";
      }
      while (!running && !predecessor.equals(VersionComparator.NO_BEFORE)) {
         predecessor = VersionComparator.getPreviousVersion(predecessor);
         running = isVersionRunning(predecessor);
      }
      return predecessor;
   }

   private boolean isVersionRunning(final String version) {
      boolean running = false;
      for (final Map.Entry<String, Version> previousCandidate : dependencies.getVersions().entrySet()) {
         if (previousCandidate.getKey().equals(version) && previousCandidate.getValue().isRunning()) {
            running = true;
         }
      }
      if (dependencies.getInitialversion().getVersion().equals(version)) {
         return true;
      }
      return running;
   }

   @Override
   public void processVersion(final String version, final Version versioninfo) {
      LOG.info("View-Generation for Version {} Index: {}", version, VersionComparator.getVersionIndex(version));
      final Set<TestCase> testcases = versioninfo.getTests().getTests();

      final boolean beforeEndVersion = endversion == null || version.equals(endversion) || VersionComparator.isBefore(version, endversion);
      LOG.debug("Before End Version {}: {}", endversion, beforeEndVersion);

      final TestSet tests = new TestSet();
      for (final TestCase testcase : testcases) {
         if ((startversion == null || !VersionComparator.isBefore(version, startversion)) && beforeEndVersion) {
            if (lastTestcaseCalls.containsKey(testcase)) {
               tests.addTest(testcase);
            }
         }
         lastTestcaseCalls.put(testcase, version);
      }
      if (tests.classCount() > 0) {
         final String predecessor = getRunningPredecessor(version);
         final Runnable currentVersionAnalyser = createGeneratorRunnable(version, predecessor, tests);
         currentVersionAnalyser.run();
      } else {
         LOG.debug("No testcase is executed in {}", version);
      }
   }

   private Runnable createGeneratorRunnable(final String version, final String predecessor, final TestSet testset) {
      LOG.info("Starting {}", version);
      final Runnable currentVersionAnalyser = new Runnable() {

         @Override
         public void run() {
            try {
               final File viewResultsFolder = new File(viewFolder, "view_" + version);
               if (!viewResultsFolder.exists()) {
                  viewResultsFolder.mkdir();
               }

               final File diffFolder = new File(viewResultsFolder, "diffs");
               if (!diffFolder.exists()) {
                  diffFolder.mkdirs();
               }
               final File projectFolderTemp = new File(folders.getTempProjectFolder(), "" + VersionComparator.getVersionIndex(version));
               GitUtils.clone(folders, projectFolderTemp);
               final PeASSFolders folders = new PeASSFolders(projectFolderTemp);
               final Map<String, List<File>> traceFileMap = new HashMap<>();
               final boolean tracesWorked = generateTraces(folders, version, testset, predecessor, traceFileMap);

               for (final TestCase testcase : testset.getTests()) {
                  if (tracesWorked && traceFileMap.size() > 0) {
                     LOG.debug("Generating Diff " + testcase.getClazz() + "#" + testcase.getMethod() + " " + predecessor + " .." + version + " " + traceFileMap.size());
                     final boolean somethingChanged = generateDiffFiles(testcase, diffFolder, traceFileMap);

                     if (somethingChanged) {
                        synchronized (changedTraceMethods) {
                           changedTraceMethods.addCall(version, predecessor, testcase);
                        }
                     }
                  } else {
                     LOG.debug("Missing: " + testcase + " Worked: " + tracesWorked);
                  }
               }

               synchronized (changedTraceMethods) {
                  LOG.debug("Writing");
                  try (FileWriter fw = new FileWriter(executeFile)) {
                     fw.write(DependencyReaderBase.OBJECTMAPPER.writeValueAsString(changedTraceMethods));
                     fw.flush();
                  } catch (final IOException e) {
                     e.printStackTrace();
                  }
               }
            } catch (final Throwable t) {
               LOG.error("There appeared an error in {} compared to {}, Test {}", version, predecessor, testset);
               t.printStackTrace();
            }
         }

      };
      return currentVersionAnalyser;
   }

   protected boolean generateTraces(final PeASSFolders folders, final String version, final TestSet testset, final String versionOld, final Map<String, List<File>> traceFileMap)
         throws IOException, InterruptedException, com.github.javaparser.ParseException, ViewNotFoundException, XmlPullParserException {
      boolean gotAllData = true;

      final TestResultManager resultsManager = new TestResultManager(folders.getProjectFolder(), timeout);
      for (final String githash : new String[] { versionOld, version }) {
         LOG.debug("Checkout... {}", folders.getProjectFolder());
         GitUtils.goToTag(githash, folders.getProjectFolder());

         LOG.debug("Calling Maven-Kieker...");

         resultsManager.getExecutor().loadClasses();
         resultsManager.executeKoPeMeKiekerRun(testset, githash);

         LOG.debug("Trace-Analysis..");

         boolean worked = false;
         final File xmlFileFolder = resultsManager.getXMLFileFolder(folders.getProjectFolder());
         for (final TestCase testcase : testset.getTests()) {
            File moduleFolder;
            if (testcase.getModule() != null) {
               moduleFolder = resultsManager.getXMLFileFolder(new File(folders.getProjectFolder(), testcase.getModule()));
            } else {
               moduleFolder = xmlFileFolder;
            }
            final OneTraceGenerator oneViewGenerator = new OneTraceGenerator(viewFolder, folders, testcase, traceFileMap, version, moduleFolder,
                  resultsManager.getExecutor().getModules());
            final boolean workedLocal = oneViewGenerator.generateTrace(githash);
            if (!workedLocal) {
               LOG.error("Problem in " + testcase);
            } else {
               worked = true;
            }
         }

         if (!worked) {
            gotAllData = false;
         }

         resultsManager.deleteTempFiles();
         for (final File moduleFolder : resultsManager.getExecutor().getModules()) {
            final File xmlFileFolder2 = resultsManager.getXMLFileFolder(moduleFolder);
            LOG.debug("Deleting folder: {}", xmlFileFolder2);
            FileUtils.deleteDirectory(xmlFileFolder2);
         }
      }
      LOG.debug("Finished: {} (Part-)Success: {}", version, gotAllData);
      return gotAllData;
   }

   /**
    * Generates a human-analysable diff-file from traces
    * 
    * @param testcase Name of the testcase
    * @param diffFolder Goal-folder for the diff
    * @param traceFileMap Map for place where traces are saved
    * @return Whether a change happened
    * @throws IOException If files can't be read of written
    */
   protected boolean generateDiffFiles(final TestCase testcase, final File diffFolder, final Map<String, List<File>> traceFileMap) throws IOException {
      final long size = FileUtils.sizeOfDirectory(diffFolder);
      final long sizeInMB = size / (1024 * 1024);
      LOG.debug("Filesize: {} ({})", sizeInMB, size);
      if (sizeInMB < 2000) {
         final List<File> traceFiles = traceFileMap.get(testcase.toString());
         if (traceFiles != null) {
            LOG.debug("Trace-Files: {}", traceFiles);
            if (traceFiles.size() > 1) {
               // final Process checkDiff = Runtime.getRuntime()
               // .exec("diff --ignore-all-space " + traceFiles.get(0).getAbsolutePath() + OneTraceGenerator.NOCOMMENT + " " + traceFiles.get(1).getAbsolutePath()
               // + OneTraceGenerator.NOCOMMENT);
               // final String isDifferent = StreamGobbler.getFullProcess(checkDiff, false);
               final String isDifferent = GitUtils.getDiff(new File(traceFiles.get(0).getAbsolutePath() + OneTraceGenerator.NOCOMMENT), new File(traceFiles.get(1).getAbsolutePath()
                     + OneTraceGenerator.NOCOMMENT));
               System.out.println(isDifferent);
               if (isDifferent.length() > 0) {
                  generateDiffFile(new File(diffFolder, testcase.getShortClazz() + "#" + testcase.getMethod() + ".txt"), traceFiles, "");
                  generateDiffFile(new File(diffFolder, testcase.getShortClazz() + "#" + testcase.getMethod() + OneTraceGenerator.METHOD), traceFiles, OneTraceGenerator.METHOD);
                  generateDiffFile(new File(diffFolder, testcase.getShortClazz() + "#" + testcase.getMethod() + OneTraceGenerator.NOCOMMENT), traceFiles,
                        OneTraceGenerator.NOCOMMENT);
                  generateDiffFile(new File(diffFolder, testcase.getShortClazz() + "#" + testcase.getMethod() + OneTraceGenerator.METHOD_EXPANDED), traceFiles,
                        OneTraceGenerator.METHOD_EXPANDED);
                  return true;
               } else {
                  LOG.info("No change; traces equal.");
                  return false;
               }
            } else {
               LOG.info("Traces not existing: {}", testcase);
               return false;
            }
         } else {
            LOG.info("Traces not existing: {}", testcase);
            return false;
         }

      } else {
         LOG.info("Tracefolder too big: {}", sizeInMB);
         return false;
      }
   }

   private void generateDiffFile(final File goalFile, final List<File> traceFiles, final String appendix) throws IOException {
      final ProcessBuilder processBuilder2 = new ProcessBuilder("diff",
            "--minimal", "--ignore-all-space", "-y", "-W", "200",
            traceFiles.get(0).getAbsolutePath() + appendix,
            traceFiles.get(1).getAbsolutePath() + appendix);
      final Process p2 = processBuilder2.start();
      final String result2 = StreamGobbler.getFullProcess(p2, false);
      try (final FileWriter fw = new FileWriter(goalFile)) {
         fw.write(result2);
      }
   }

   public File getExecuteFile() {
      return executeFile;
   }
   
   public static void main(final String[] args) throws IOException, InterruptedException, com.github.javaparser.ParseException, ViewNotFoundException, XmlPullParserException {
      final File projectFolder = new File("../../projekte/commons-io");
      final File viewFolder2 = new File("temp_views");
      viewFolder2.mkdirs();
      final ViewGenerator generator = new ViewGenerator(projectFolder, new Dependencies(), new File("temp.json"), viewFolder2, 1, 3);
      
      final TestSet testset = new TestSet();
      testset.addTest(new TestCase("org.apache.commons.io.DirectoryWalkerTestCase", "testFilterDirAndFile1", ""));
      generator.generateTraces(new PeASSFolders(projectFolder), "cecd08", testset, "d4c5044c7b7697d944a444470a296dcd15911595", new HashMap<>());
   }
}
