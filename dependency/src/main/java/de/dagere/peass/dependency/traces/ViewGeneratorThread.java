package de.dagere.peass.dependency.traces;

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

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.KiekerResultManager;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.ResultsFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.GitUtils;

public class ViewGeneratorThread implements Runnable {

   private static final Logger LOG = LogManager.getLogger(ViewGenerator.class);

   private final String version, predecessor;
   private final ExecutionConfig executionConfig;
   private final PeASSFolders folders;
   private final ResultsFolders resultsFolders;
   private final TestSet testset;
   private final ExecutionData changedTraceMethods;
   private final EnvironmentVariables env;

   public ViewGeneratorThread(final String version, final String predecessor, final PeASSFolders folders, final ResultsFolders resultsFolders, final TestSet testset,
         final ExecutionData changedTraceMethods,
         final ExecutionConfig executionConfig, final EnvironmentVariables env) {
      this.version = version;
      this.predecessor = predecessor;
      this.folders = folders;
      this.resultsFolders = resultsFolders;
      this.testset = testset;
      this.changedTraceMethods = changedTraceMethods;
      this.executionConfig = executionConfig;
      this.env = env;
   }

   @Override
   public void run() {
      try {
         final File viewResultsFolder = resultsFolders.getVersionViewFolder(version);

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
            try (FileWriter fw = new FileWriter(resultsFolders.getExecutionFile())) {
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
         DiffFileGenerator diffGenerator = new DiffFileGenerator();
         final boolean somethingChanged = diffGenerator.generateDiffFiles(testcase, diffFolder, traceFileMap);

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
      return folders.getTempFolder("" + VersionComparator.getVersionIndex(version));
   }

   protected boolean generateTraces(final PeASSFolders folders, final Map<String, List<File>> traceFileMap)
         throws IOException, InterruptedException, com.github.javaparser.ParseException, ViewNotFoundException, XmlPullParserException {
      boolean gotAllData = true;

      final KiekerResultManager resultsManager = new KiekerResultManager(folders, executionConfig, env);
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
         for (final File moduleFolder : resultsManager.getExecutor().getModules().getModules()) {
            final File xmlFileFolder2 = resultsManager.getXMLFileFolder(moduleFolder);
            LOG.debug("Deleting folder: {}", xmlFileFolder2);
            if (xmlFileFolder2.exists()) {
               FileUtils.forceDelete(xmlFileFolder2);
            }
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
         final OneTraceGenerator oneViewGenerator = new OneTraceGenerator(resultsFolders, folders, testcase, traceFileMap, version, moduleFolder,
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
}
