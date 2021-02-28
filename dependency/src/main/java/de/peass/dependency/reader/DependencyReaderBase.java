package de.peass.dependency.reader;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.peass.config.ExecutionConfig;
import de.peass.dependency.ChangeManager;
import de.peass.dependency.DependencyManager;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.ModuleClassMapping;
import de.peass.dependency.analysis.data.ChangeTestMapping;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestExistenceChanges;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.changesreading.ClazzChangeData;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.Version;
import de.peass.utils.Constants;
import de.peass.vcs.VersionIterator;

/**
 * Shared functions for dependency reading, which are both used if dependencies are read fully or if one continues a dependency reading process.
 * 
 * @author reichelt
 *
 */
public abstract class DependencyReaderBase {

   private static final boolean DETAIL_DEBUG = true;
   static {
      Constants.OBJECTMAPPER.enable(SerializationFeature.INDENT_OUTPUT);
   }

   private static final Logger LOG = LogManager.getLogger(DependencyReaderBase.class);

   protected final Dependencies dependencyResult;
   protected final File dependencyFile;
   protected DependencyManager dependencyManager;
   protected final PeASSFolders folders;
   protected VersionIterator iterator;
   // protected TestDependencies dependencyMap;
   protected String lastRunningVersion;
   protected final int timeout;
   private final VersionKeeper skippedNoChange;
   private final ExecutionConfig executionConfig;

   /**
    * Initializes the reader with the given result-object, the folder to examine an the file to write to
    * 
    * @param dependencyResult Object to write results to
    * @param projectFolder Folder to examine
    * @param dependencyFile File to write results to
    */
   public DependencyReaderBase(final Dependencies dependencyResult, final File projectFolder, final File dependencyFile, final int timeout, 
         final VersionKeeper skippedNoChange, final ExecutionConfig executionConfig) {
      this.dependencyResult = dependencyResult;
      this.dependencyFile = dependencyFile;
      this.folders = new PeASSFolders(projectFolder);
      this.timeout = timeout;
      this.skippedNoChange = skippedNoChange;
      this.executionConfig = executionConfig;
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

      final Map<ChangedEntity, ClazzChangeData> changes;
      String predecessor;
      if (iterator.isPredecessor(lastRunningVersion)) {
         changes = changeManager.getChanges(null);
         predecessor = lastRunningVersion;
      } else {
         changes = changeManager.getChanges(lastRunningVersion);
         predecessor = lastRunningVersion;
      }
      changeManager.saveOldClasses();
      lastRunningVersion = iterator.getTag();

      if (DETAIL_DEBUG) {
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "initialdependencies_" + version + ".json"), dependencyManager.getDependencyMap());
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "changes_" + version + ".json"), changes);
      }

      if (changes.size() > 0) {
         return analyseChanges(version, changes, predecessor);
      } else {
         skippedNoChange.addVersion(version, "No Change at all");
         return 0;
      }
   }

   private int analyseChanges(final String version, final Map<ChangedEntity, ClazzChangeData> changes, final String predecessor)
         throws IOException, JsonGenerationException, JsonMappingException, XmlPullParserException, InterruptedException {
      final ChangeTestMapping changeTestMap = dependencyManager.getDependencyMap().getChangeTestMap(changes); // tells which tests need to be run, and
                                                                                                              // because of
      LOG.debug("Change test mapping (without added tests): " + changeTestMap);
      // which change they need to be run

      if (DETAIL_DEBUG)
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "changetest_" + version + ".json"), changeTestMap);

      final Version newVersionInfo = DependencyReaderUtil.createVersionFromChangeMap(version, changes, changeTestMap);
      newVersionInfo.setJdk(dependencyManager.getExecutor().getJDKVersion());
      newVersionInfo.setPredecessor(predecessor);

      if (DETAIL_DEBUG) {
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "versioninfo_" + version + ".json"), newVersionInfo);
      }

      LOG.debug("Updating dependencies.. {}", version);

      final TestSet testsToRun = dependencyManager.getTestsToRun(changes); // contains only the tests that need to be run -> could be changeTestMap.values() und dann umwandeln
      Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "toRun_" + version + ".json"), testsToRun.entrySet());

      final int changedTests;
      if (testsToRun.classCount() > 0) {
         changedTests = analyzeTests(version, newVersionInfo, testsToRun);
      } else {
         changedTests = 0;
      }
      dependencyResult.getVersions().put(version, newVersionInfo);
      return changedTests;
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

   int analyzeTests(final String version, final Version newVersionInfo, final TestSet testsToRun)
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

      final int changedClazzCount = newVersionInfo.getChangedClazzes().values().stream().mapToInt(value -> {
         return value.getTestcases().values().stream().mapToInt(list -> list.size()).sum();
      }).sum();
      return changedClazzCount;
   }

   public boolean readInitialVersion() throws IOException, InterruptedException, XmlPullParserException {
      dependencyManager = new DependencyManager(folders, executionConfig);
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
      dependencyResult.setVersions(initialdependencies.getVersions());
      dependencyResult.setInitialversion(initialdependencies.getInitialversion());
      
      dependencyManager = new DependencyManager(folders, executionConfig);

      InitialVersionReader initialVersionReader = new InitialVersionReader(initialdependencies, dependencyManager, iterator);
      initialVersionReader.readCompletedVersions();
      DependencyReaderUtil.write(dependencyResult, dependencyFile);
      lastRunningVersion = iterator.getTag();
   }

}
