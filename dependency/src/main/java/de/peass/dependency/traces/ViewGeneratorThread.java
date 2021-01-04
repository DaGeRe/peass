package de.peass.dependency.traces;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.github.javaparser.ParseException;

import de.peass.dependency.KiekerResultManager;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.utils.Constants;
import de.peass.vcs.GitUtils;

public class ViewGeneratorThread implements Runnable {

   private static final Logger LOG = LogManager.getLogger(ViewGenerator.class);

   private final String version, predecessor;
   private final PeASSFolders folders;
   private final File viewFolder, executeFile;
   private final TestSet testset;
   private final ExecutionData changedTraceMethods;
   private final int timeout;

   public ViewGeneratorThread(final String version, final String predecessor, final PeASSFolders folders, final File viewFolder, final File executeFile, final TestSet testset,
         final ExecutionData changedTraceMethods,
         final int timeout) {
      super();
      this.version = version;
      this.predecessor = predecessor;
      this.folders = folders;
      this.viewFolder = viewFolder;
      this.executeFile = executeFile;
      this.testset = testset;
      this.changedTraceMethods = changedTraceMethods;
      this.timeout = timeout;
   }

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
         final PeASSFolders folders = initProjectFolder();
         final Map<String, List<File>> traceFileMap = new HashMap<>();
         final boolean tracesWorked = generateTraces(folders, traceFileMap);

         for (final TestCase testcase : testset.getTests()) {
            analyzeTestcase(diffFolder, traceFileMap, tracesWorked, testcase);
         }

         synchronized (changedTraceMethods) {
            LOG.debug("Writing");
            try (FileWriter fw = new FileWriter(executeFile)) {
               fw.write(Constants.OBJECTMAPPER.writeValueAsString(changedTraceMethods));
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

   private void analyzeTestcase(final File diffFolder, final Map<String, List<File>> traceFileMap, final boolean tracesWorked, final TestCase testcase) throws IOException {
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

   private PeASSFolders initProjectFolder() throws InterruptedException, IOException {
      final File projectFolderTemp = new File(folders.getTempProjectFolder(), "" + VersionComparator.getVersionIndex(version));
      if (projectFolderTemp.exists()) {
         LOG.warn("Deleting existing folder {}", projectFolderTemp);
         FileUtils.deleteDirectory(projectFolderTemp);
         File peassFolder = new File(projectFolderTemp.getParentFile(), projectFolderTemp.getName()+"_peass");
         if (peassFolder.exists()) {
            FileUtils.deleteDirectory(peassFolder);
         }
      }
      GitUtils.clone(folders, projectFolderTemp);
      final PeASSFolders folders = new PeASSFolders(projectFolderTemp);
      return folders;
   }

   protected boolean generateTraces(final PeASSFolders folders, final Map<String, List<File>> traceFileMap)
         throws IOException, InterruptedException, com.github.javaparser.ParseException, ViewNotFoundException, XmlPullParserException {
      boolean gotAllData = true;

      final KiekerResultManager resultsManager = new KiekerResultManager(folders, timeout);
      LOG.info("View Comparing {} against {}", version, predecessor);
      for (final String githash : new String[] { predecessor, version }) {
         LOG.debug("Checkout... {}", folders.getProjectFolder());
         GitUtils.goToTag(githash, folders.getProjectFolder());

         LOG.debug("Calling Maven-Kieker...");

         resultsManager.getExecutor().loadClasses();
         resultsManager.executeKoPeMeKiekerRun(testset, githash);

         LOG.debug("Trace-Analysis..");

         final boolean worked = generateVersionTraces(folders, traceFileMap, resultsManager, githash);

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

   public boolean generateVersionTraces(final PeASSFolders folders, final Map<String, List<File>> traceFileMap, final KiekerResultManager resultsManager, final String githash)
         throws FileNotFoundException, IOException, XmlPullParserException, ParseException, ViewNotFoundException {
      boolean worked = false;
      for (final TestCase testcase : testset.getTests()) {
         final File moduleFolder = KiekerFolderUtil.getModuleResultFolder(folders, testcase);
         final OneTraceGenerator oneViewGenerator = new OneTraceGenerator(viewFolder, folders, testcase, traceFileMap, version, moduleFolder,
               resultsManager.getExecutor().getModules());
         final boolean workedLocal = oneViewGenerator.generateTrace(githash);
         if (!workedLocal) {
            LOG.error("Problem in " + testcase);
         } else {
            worked = true;
         }
      }
      return worked;
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
               final String isDifferent = GitUtils.getDiff(new File(traceFiles.get(0).getAbsolutePath() + OneTraceGenerator.NOCOMMENT), new File(traceFiles.get(1).getAbsolutePath()
                     + OneTraceGenerator.NOCOMMENT));
               System.out.println(isDifferent);
               if (isDifferent.length() > 0) {
                  createAllDiffs(testcase, diffFolder, traceFiles);
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

   private void createAllDiffs(final TestCase testcase, final File diffFolder, final List<File> traceFiles) throws IOException {
      final String testcaseName = testcase.getShortClazz() + "#" + testcase.getMethod();
      DiffUtil.generateDiffFile(new File(diffFolder, testcaseName + ".txt"), traceFiles, "");
      DiffUtil.generateDiffFile(new File(diffFolder, testcaseName + OneTraceGenerator.METHOD), traceFiles, OneTraceGenerator.METHOD);
      DiffUtil.generateDiffFile(new File(diffFolder, testcaseName + OneTraceGenerator.NOCOMMENT), traceFiles,
            OneTraceGenerator.NOCOMMENT);
      DiffUtil.generateDiffFile(new File(diffFolder, testcaseName + OneTraceGenerator.METHOD_EXPANDED), traceFiles,
            OneTraceGenerator.METHOD_EXPANDED);
   }

}
