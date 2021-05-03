package de.dagere.peass.dependency.reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.javaparser.ParseException;

import de.dagere.peass.config.DependencyConfig;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.DependencyManager;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.ResultsFolders;
import de.dagere.peass.dependency.analysis.data.ChangeTestMapping;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.Version;
import de.dagere.peass.dependency.traces.DiffFileGenerator;
import de.dagere.peass.dependency.traces.TraceFileMapping;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.VersionIterator;

/**
 * Shared functions for dependency reading, which are both used if dependencies are read fully or if one continues a dependency reading process.
 * 
 * @author reichelt
 *
 */
public class DependencyReader {

   private static final boolean DETAIL_DEBUG = true;

   private static final Logger LOG = LogManager.getLogger(DependencyReader.class);

   private final DependencyConfig dependencyConfig;
   protected final Dependencies dependencyResult = new Dependencies();
   private final ExecutionData executionResult = new ExecutionData();
   protected final ResultsFolders resultsFolders;
   protected DependencyManager dependencyManager;
   protected final PeASSFolders folders;
   protected VersionIterator iterator;
   protected String lastRunningVersion;
   private final VersionKeeper skippedNoChange;
   private final ExecutionConfig executionConfig;
   private final EnvironmentVariables env;

   private final ChangeManager changeManager;
   private final DependencySizeRecorder sizeRecorder = new DependencySizeRecorder();
   private final TraceFileMapping mapping = new TraceFileMapping();

   public DependencyReader(final DependencyConfig dependencyConfig, final PeASSFolders folders,
         final ResultsFolders resultsFolders, final String url, final VersionIterator iterator,
         final ChangeManager changeManager, final ExecutionConfig executionConfig, final EnvironmentVariables env) {
      this.dependencyConfig = dependencyConfig;
      this.resultsFolders = resultsFolders;
      this.iterator = iterator;
      this.folders = folders;
      this.skippedNoChange = new VersionKeeper(new File("/dev/null"));
      this.executionConfig = executionConfig;
      this.env = env;

      dependencyResult.setUrl(url);
      executionResult.setUrl(url);

      this.changeManager = changeManager;
   }

   /**
    * Starts reading dependencies
    * 
    * @param projectFolder
    * @param dependencyFile
    * @param url
    * @param iterator
    */
   public DependencyReader(final DependencyConfig dependencyConfig, final PeASSFolders folders, final ResultsFolders resultsFolders, final String url,
         final VersionIterator iterator,
         final VersionKeeper skippedNoChange, final ExecutionConfig executionConfig, final EnvironmentVariables env) {
      this.dependencyConfig = dependencyConfig;
      this.resultsFolders = resultsFolders;
      this.iterator = iterator;
      this.folders = folders;
      this.skippedNoChange = skippedNoChange;
      this.executionConfig = executionConfig;
      this.env = env;

      dependencyResult.setUrl(url);

      changeManager = new ChangeManager(folders, iterator);
   }

   /**
    * Reads the dependencies of the tests
    */
   public boolean readDependencies() {
      try {
         LOG.debug("Analysing {} entries", iterator.getRemainingSize());

         sizeRecorder.setPrunedSize(dependencyManager.getDependencyMap().size());

         changeManager.saveOldClasses();
         lastRunningVersion = iterator.getTag();
         while (iterator.hasNextCommit()) {
            iterator.goToNextCommit();
            readVersion();
         }

         LOG.debug("Finished dependency-reading");
         return true;
      } catch (IOException | XmlPullParserException | InterruptedException | ParseException | ViewNotFoundException e) {
         e.printStackTrace();
         return false;
      }
   }

   public void readVersion() throws IOException, FileNotFoundException, XmlPullParserException, InterruptedException, ParseException, ViewNotFoundException {
      final int tests = analyseVersion(changeManager);
      DependencyReaderUtil.write(dependencyResult, resultsFolders.getDependencyFile());
      if (dependencyConfig.isGenerateViews()) {
         Constants.OBJECTMAPPER.writeValue(resultsFolders.getExecutionFile(), executionResult);
      }

      sizeRecorder.addVersionSize(dependencyManager.getDependencyMap().size(), tests);

      dependencyManager.getExecutor().deleteTemporaryFiles();
      TooBigLogCleaner.cleanXMLFolder(folders);
      TooBigLogCleaner.cleanTooBigLogs(folders, iterator.getTag());
   }

   /**
    * Determines the tests that may have got new dependencies, writes that changes (i.e. the tests that need to be run in that version) and re-runs the tests in order to get the
    * updated test dependencies.
    * 
    * @param dependencyFile
    * @param dependencyManager
    * @param dependencies
    * @param dependencyResult
    * @param version
    * @return
    * @throws IOException
    * @throws XmlPullParserException
    * @throws InterruptedException
    * @throws ViewNotFoundException 
    * @throws ParseException 
    */
   public int analyseVersion(final ChangeManager changeManager) throws IOException, XmlPullParserException, InterruptedException, ParseException, ViewNotFoundException {
      final String version = iterator.getTag();
      if (!dependencyManager.getExecutor().isVersionRunning(iterator.getTag())) {
         documentFailure(version);
         return 0;
      }

      dependencyManager.getExecutor().loadClasses();

      final DependencyReadingInput input;
      if (iterator.isPredecessor(lastRunningVersion)) {
         input = new DependencyReadingInput(changeManager.getChanges(null), lastRunningVersion);
      } else {
         input = new DependencyReadingInput(changeManager.getChanges(lastRunningVersion), lastRunningVersion);
      }
      changeManager.saveOldClasses();
      lastRunningVersion = iterator.getTag();

      if (DETAIL_DEBUG) {
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "initialdependencies_" + version + ".json"), dependencyManager.getDependencyMap());
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "changes_" + version + ".json"), input.getChanges());
      }

      if (input.getChanges().size() > 0) {
         return analyseChanges(version, input);
      } else {
         skippedNoChange.addVersion(version, "No Change at all");
         return 0;
      }
   }

   private int analyseChanges(final String version, final DependencyReadingInput input)
         throws IOException, JsonGenerationException, JsonMappingException, XmlPullParserException, InterruptedException, ParseException, ViewNotFoundException {
      final Version newVersionInfo = handleStaticAnalysisChanges(version, input);

      if (!dependencyConfig.isDoNotUpdateDependencies()) {
         TraceChangeHandler traceChangeHandler = new TraceChangeHandler(dependencyManager, folders, executionConfig, version);
         traceChangeHandler.handleTraceAnalysisChanges(newVersionInfo);

         if (dependencyConfig.isGenerateViews()) {
            TraceViewGenerator traceViewGenerator = new TraceViewGenerator(dependencyManager, folders, version, mapping);
            traceViewGenerator.generateViews(resultsFolders, newVersionInfo.getTests());

            DiffFileGenerator diffGenerator = new DiffFileGenerator(resultsFolders.getVersionDiffFolder(version));
            for (TestCase testcase : newVersionInfo.getTests().getTests()) {
               boolean somethingChanged = diffGenerator.generateDiffFiles(testcase, mapping);
               if (somethingChanged) {
                  executionResult.addCall(version, newVersionInfo.getPredecessor(), testcase);
               }
            }
         }
      } else {
         LOG.debug("Not updating dependencies since doNotUpdateDependencies was set - only returning dependencies based on changed classes");
      }
      dependencyResult.getVersions().put(version, newVersionInfo);

      final int changedClazzCount = calculateChangedClassCount(newVersionInfo);
      return changedClazzCount;
   }

   private int calculateChangedClassCount(final Version newVersionInfo) {
      final int changedClazzCount = newVersionInfo.getChangedClazzes().values().stream().mapToInt(value -> {
         return value.getTestcases().values().stream().mapToInt(list -> list.size()).sum();
      }).sum();
      return changedClazzCount;
   }

   private Version handleStaticAnalysisChanges(final String version, final DependencyReadingInput input) throws IOException, JsonGenerationException, JsonMappingException {
      final ChangeTestMapping changeTestMap = dependencyManager.getDependencyMap().getChangeTestMap(input.getChanges()); // tells which tests need to be run, and
      // because of
      LOG.debug("Change test mapping (without added tests): " + changeTestMap);
      // which change they need to be run

      if (DETAIL_DEBUG)
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "changetest_" + version + ".json"), changeTestMap);

      final Version newVersionInfo = DependencyReaderUtil.createVersionFromChangeMap(input.getChanges(), changeTestMap);
      newVersionInfo.setJdk(dependencyManager.getExecutor().getJDKVersion());
      newVersionInfo.setPredecessor(input.getPredecessor());

      if (DETAIL_DEBUG) {
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "versioninfo_" + version + ".json"), newVersionInfo);
      }
      return newVersionInfo;
   }

   public void documentFailure(final String version) {
      if (dependencyManager.getExecutor().isAndroid()) {
         dependencyResult.setAndroid(true);
         executionResult.setAndroid(true);
      }
      LOG.error("Version not running");
      final Version newVersionInfo = new Version();
      newVersionInfo.setRunning(false);
      dependencyResult.getVersions().put(version, newVersionInfo);
   }

   public boolean readInitialVersion() throws IOException, InterruptedException, XmlPullParserException, ParseException, ViewNotFoundException {
      dependencyManager = new DependencyManager(folders, executionConfig, env);
      InitialVersionReader initialVersionReader = new InitialVersionReader(dependencyResult, dependencyManager, iterator);
      if (initialVersionReader.readInitialVersion()) {
         DependencyReaderUtil.write(dependencyResult, resultsFolders.getDependencyFile());
         lastRunningVersion = iterator.getTag();
         
         if (dependencyConfig.isGenerateViews()) {
            generateInitialViews();
         }
         dependencyManager.cleanResultFolder();
         return true;
      } else {
         return false;
      }
   }

   private void generateInitialViews() throws IOException, XmlPullParserException, ParseException, ViewNotFoundException, InterruptedException {
      TestSet initialTests = dependencyResult.getInitialversion().getInitialTests();
      TraceViewGenerator traceViewGenerator = new TraceViewGenerator(dependencyManager, folders, iterator.getTag(), mapping);
      traceViewGenerator.generateViews(resultsFolders, initialTests);
      
      executionResult.getVersions().put(iterator.getTag(), new TestSet());
   }

   public void readCompletedVersions(final Dependencies initialdependencies) {
      dependencyManager = new DependencyManager(folders, executionConfig, env);

      dependencyResult.setVersions(initialdependencies.getVersions());
      dependencyResult.setInitialversion(initialdependencies.getInitialversion());

      InitialVersionReader initialVersionReader = new InitialVersionReader(initialdependencies, dependencyManager, iterator);
      initialVersionReader.readCompletedVersions();
      DependencyReaderUtil.write(dependencyResult, resultsFolders.getDependencyFile());
      lastRunningVersion = iterator.getTag();
   }

   public Dependencies getDependencies() {
      return dependencyResult;
   }

   public void setIterator(final VersionIterator reserveIterator) {
      this.iterator = reserveIterator;
   }

}
