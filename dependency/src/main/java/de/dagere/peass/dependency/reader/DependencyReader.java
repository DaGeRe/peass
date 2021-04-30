package de.dagere.peass.dependency.reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.ci.NonIncludedTestRemover;
import de.dagere.peass.config.DependencyConfig;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.DependencyManager;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.ChangeTestMapping;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestExistenceChanges;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.Version;
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
   protected final File dependencyFile;
   protected DependencyManager dependencyManager;
   protected final PeASSFolders folders;
   protected VersionIterator iterator;
   protected String lastRunningVersion;
   private final VersionKeeper skippedNoChange;
   private final ExecutionConfig executionConfig;
   private final EnvironmentVariables env;

   private final ChangeManager changeManager;
   private final DependencySizeRecorder sizeRecorder = new DependencySizeRecorder();

   public DependencyReader(final DependencyConfig dependencyConfig, final PeASSFolders folders,
         final File dependencyFile, final String url, final VersionIterator iterator,
         final ChangeManager changeManager, final ExecutionConfig executionConfig, final EnvironmentVariables env) {
      this.dependencyConfig = dependencyConfig;
      this.dependencyFile = dependencyFile;
      this.iterator = iterator;
      this.folders = folders;
      this.skippedNoChange = new VersionKeeper(new File("/dev/null"));
      this.executionConfig = executionConfig;
      this.env = env;

      dependencyResult.setUrl(url);

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
   public DependencyReader(final DependencyConfig dependencyConfig, final PeASSFolders folders, final File dependencyFile, final String url, final VersionIterator iterator,
         final VersionKeeper skippedNoChange, final ExecutionConfig executionConfig, final EnvironmentVariables env) {
      this.dependencyConfig = dependencyConfig;
      this.dependencyFile = dependencyFile;
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
      } catch (IOException | XmlPullParserException | InterruptedException e) {
         e.printStackTrace();
         return false;
      }
   }

   public void readVersion() throws IOException, FileNotFoundException, XmlPullParserException, InterruptedException {
      final int tests = analyseVersion(changeManager);
      DependencyReaderUtil.write(dependencyResult, dependencyFile);
      
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
    */
   public int analyseVersion(final ChangeManager changeManager) throws IOException, XmlPullParserException, InterruptedException {
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
         throws IOException, JsonGenerationException, JsonMappingException, XmlPullParserException, InterruptedException {
      final Version newVersionInfo = handleStaticAnalysisChanges(version, input);

      if (!dependencyConfig.isDoNotUpdateDependencies()) {
         handleTraceAnalysisChanges(version, input, newVersionInfo);
      } else {
         LOG.debug("Not updating dependencies since doNotUpdateDependencies was set - only returning dependencies based on changed classes");
         dependencyResult.getVersions().put(version, newVersionInfo);
      }

      final int changedClazzCount = newVersionInfo.getChangedClazzes().values().stream().mapToInt(value -> {
         return value.getTestcases().values().stream().mapToInt(list -> list.size()).sum();
      }).sum();
      return changedClazzCount;

   }

   private void handleTraceAnalysisChanges(final String version, final DependencyReadingInput input, final Version newVersionInfo)
         throws IOException, JsonGenerationException, JsonMappingException, XmlPullParserException, InterruptedException {
      LOG.debug("Updating dependencies.. {}", version);

      final TestSet testsToRun = dependencyManager.getTestsToRun(input.getChanges()); // contains only the tests that need to be run -> could be changeTestMap.values() und dann umwandeln
      Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "toRun_" + version + ".json"), testsToRun.entrySet());

      NonIncludedTestRemover.removeNotIncluded(testsToRun, executionConfig);

      if (testsToRun.classCount() > 0) {
         analyzeTests(version, newVersionInfo, testsToRun);
      }
      dependencyResult.getVersions().put(version, newVersionInfo);
   }

   private Version handleStaticAnalysisChanges(final String version, final DependencyReadingInput input) throws IOException, JsonGenerationException, JsonMappingException {
      final ChangeTestMapping changeTestMap = dependencyManager.getDependencyMap().getChangeTestMap(input.getChanges()); // tells which tests need to be run, and
                                                                                                              // because of
      LOG.debug("Change test mapping (without added tests): " + changeTestMap);
      // which change they need to be run

      if (DETAIL_DEBUG)
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "changetest_" + version + ".json"), changeTestMap);

      final Version newVersionInfo = DependencyReaderUtil.createVersionFromChangeMap(version, input.getChanges(), changeTestMap);
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
      }
      LOG.error("Version not running");
      final Version newVersionInfo = new Version();
      newVersionInfo.setRunning(false);
      dependencyResult.getVersions().put(version, newVersionInfo);
   }

   void analyzeTests(final String version, final Version newVersionInfo, final TestSet testsToRun)
         throws IOException, XmlPullParserException, InterruptedException, JsonGenerationException, JsonMappingException {
      final ModuleClassMapping mapping = new ModuleClassMapping(dependencyManager.getExecutor());
      dependencyManager.runTraceTests(testsToRun, version);
      final TestExistenceChanges testExistenceChanges = dependencyManager.updateDependencies(testsToRun, version, mapping);
      final Map<ChangedEntity, Set<ChangedEntity>> newTestcases = testExistenceChanges.getAddedTests();

      if (DETAIL_DEBUG) {
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "add_" + version + ".json"), newTestcases);
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "remove_" + version + ".json"), testExistenceChanges.getRemovedTests());
      }

      DependencyReaderUtil.removeDeletedTestcases(newVersionInfo, testExistenceChanges);
      DependencyReaderUtil.addNewTestcases(newVersionInfo, newTestcases);

      if (DETAIL_DEBUG)
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "final_" + version + ".json"), newVersionInfo);
   }

   public boolean readInitialVersion() throws IOException, InterruptedException, XmlPullParserException {
      dependencyManager = new DependencyManager(folders, executionConfig, env);
      InitialVersionReader initialVersionReader = new InitialVersionReader(dependencyResult, dependencyManager, iterator);
      if (initialVersionReader.readInitialVersion()) {
         DependencyReaderUtil.write(dependencyResult, dependencyFile);
         lastRunningVersion = iterator.getTag();
         return true;
      } else {
         return false;
      }
   }

   public void readCompletedVersions(final Dependencies initialdependencies) {
      dependencyManager = new DependencyManager(folders, executionConfig, env);

      dependencyResult.setVersions(initialdependencies.getVersions());
      dependencyResult.setInitialversion(initialdependencies.getInitialversion());

      InitialVersionReader initialVersionReader = new InitialVersionReader(initialdependencies, dependencyManager, iterator);
      initialVersionReader.readCompletedVersions();
      DependencyReaderUtil.write(dependencyResult, dependencyFile);
      lastRunningVersion = iterator.getTag();
   }

   public Dependencies getDependencies() {
      return dependencyResult;
   }

   public void setIterator(final VersionIterator reserveIterator) {
      this.iterator = reserveIterator;
   }

}
