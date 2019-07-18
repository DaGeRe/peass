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

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.KiekerResultManager;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.utils.Constants;
import de.peass.utils.StreamGobbler;
import de.peass.vcs.GitUtils;

public class ViewGeneratorThread implements Runnable {

   private static final Logger LOG = LogManager.getLogger(ViewGenerator.class);

   private final String version, predecessor;
   private final PeASSFolders folders;
   private final  File viewFolder, executeFile;
   private final TestSet testset;
   private final ExecutionData changedTraceMethods;
   private final int timeout;
   
   

   public ViewGeneratorThread(String version, String predecessor, PeASSFolders folders, File viewFolder, File executeFile, TestSet testset, ExecutionData changedTraceMethods,
         int timeout) {
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
   
   protected boolean generateTraces(final PeASSFolders folders, final String version, final TestSet testset, final String versionOld, final Map<String, List<File>> traceFileMap)
         throws IOException, InterruptedException, com.github.javaparser.ParseException, ViewNotFoundException, XmlPullParserException {
      boolean gotAllData = true;

      final KiekerResultManager resultsManager = new KiekerResultManager(folders.getProjectFolder(), timeout);
      LOG.info("View Comparing {} against {}", version, versionOld);
      for (final String githash : new String[] { versionOld, version }) {
         LOG.debug("Checkout... {}", folders.getProjectFolder());
         GitUtils.goToTag(githash, folders.getProjectFolder());

         LOG.debug("Calling Maven-Kieker...");

         resultsManager.getExecutor().loadClasses();
         resultsManager.executeKoPeMeKiekerRun(testset, githash);

         LOG.debug("Trace-Analysis..");

         boolean worked = generateVersionTraces(folders, version, testset, traceFileMap, resultsManager, githash);

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

   public boolean generateVersionTraces(final PeASSFolders folders, final String version, final TestSet testset, final Map<String, List<File>> traceFileMap,
         final KiekerResultManager resultsManager, final String githash) throws FileNotFoundException, IOException, XmlPullParserException, ParseException, ViewNotFoundException {
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

}
