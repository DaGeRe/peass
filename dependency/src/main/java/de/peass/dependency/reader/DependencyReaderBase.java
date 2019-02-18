package de.peass.dependency.reader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.peass.dependency.ChangeManager;
import de.peass.dependency.DependencyManager;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.ModuleClassMapping;
import de.peass.dependency.analysis.data.CalledMethods;
import de.peass.dependency.analysis.data.ChangeTestMapping;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.ClazzChangeData;
import de.peass.dependency.analysis.data.TestDependencies;
import de.peass.dependency.analysis.data.TestExistenceChanges;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.InitialDependency;
import de.peass.dependency.persistence.InitialVersion;
import de.peass.dependency.persistence.Version;
import de.peass.vcs.VersionIterator;

/**
 * Shared functions for dependency reading, which are both used if dependencies are read fully or if one continues a dependency reading process.
 * 
 * @author reichelt
 *
 */
public abstract class DependencyReaderBase {

   private static final boolean DETAIL_DEBUG = true;
   private static final File DEBUG_FOLDER = new File("debug");
   public final static ObjectMapper OBJECTMAPPER = new ObjectMapper();

   static {
      if (DETAIL_DEBUG) {
         if (!DEBUG_FOLDER.exists()) {
            DEBUG_FOLDER.mkdir();
         }
      }
      OBJECTMAPPER.enable(SerializationFeature.INDENT_OUTPUT);
   }

   private static final Logger LOG = LogManager.getLogger(DependencyReaderBase.class);

   protected final Dependencies dependencyResult;
   protected final File dependencyFile;
   protected DependencyManager dependencyManager;
   protected final PeASSFolders folders;
   protected VersionIterator iterator;
   protected TestDependencies dependencyMap;
   protected String lastRunningVersion;
   protected final int timeout;
   private final VersionKeeper skippedNoChange;

   /**
    * Initializes the reader with the given result-object, the folder to examine an the file to write to
    * 
    * @param dependencyResult Object to write results to
    * @param projectFolder Folder to examine
    * @param dependencyFile File to write results to
    */
   public DependencyReaderBase(final Dependencies dependencyResult, final File projectFolder, final File dependencyFile, final int timeout, final VersionKeeper skippedNoChange) {
      this.dependencyResult = dependencyResult;
      this.dependencyFile = dependencyFile;
      this.folders = new PeASSFolders(projectFolder);
      this.timeout = timeout;
      this.skippedNoChange = skippedNoChange;
   }

   protected static InitialVersion createInitialVersion(final String startVersion, final TestDependencies dependencyMap, final int jdkversion) {
      final InitialVersion initialversion = new InitialVersion();
      initialversion.setVersion(startVersion);
      initialversion.setJdk(jdkversion);
      LOG.debug("Starting writing: {}", dependencyMap.getDependencyMap().size());
      for (final Entry<ChangedEntity, CalledMethods> dependencyEntry : dependencyMap.getDependencyMap().entrySet()) {
         final ChangedEntity testcase = dependencyEntry.getKey();
         for (final Map.Entry<ChangedEntity, Set<String>> calledClassEntry : dependencyEntry.getValue().getCalledMethods().entrySet()) {
            final ChangedEntity dependentclass = calledClassEntry.getKey();
            if (!dependentclass.getJavaClazzName().contains("junit") && !dependentclass.getJavaClazzName().contains("log4j")) {
               for (final String dependentmethod : calledClassEntry.getValue()) {
                  final ChangedEntity callee = new ChangedEntity(dependentclass.getClazz(), dependentclass.getModule(), dependentmethod);
                  initialversion.addDependency(testcase, callee);
               }
            }
         }
         initialversion.sort(testcase);
      }
      return initialversion;
   }

   protected DependencyManager readCompletedVersions() {
      // final DependencyManager handler = ;
      final TestDependencies testDependencies = dependencyManager.getDependencyMap();
      for (final Entry<ChangedEntity, InitialDependency> dependency : dependencyResult.getInitialversion().getInitialDependencies().entrySet()) {
         for (final ChangedEntity dependentClass : dependency.getValue().getEntities()) {
            final Map<ChangedEntity, Set<String>> dependents = testDependencies.getDependenciesForTest(dependency.getKey());
            final ChangedEntity dependencyEntity = new ChangedEntity(dependentClass.getClazz(), dependentClass.getModule());
            Set<String> methods = dependents.get(dependencyEntity);
            if (methods == null) {
               methods = new HashSet<>();
               dependents.put(dependencyEntity, methods);
            }
            methods.add(dependentClass.getMethod());
         }
      }
      dependencyMap = testDependencies;

      checkCorrectness();

      final InitialVersion initialversion = createInitialVersion(iterator.getTag(), dependencyMap, dependencyResult.getInitialversion().getJdk());
      dependencyResult.setInitialversion(initialversion);
      DependencyReaderUtil.write(dependencyResult, dependencyFile);

      if (dependencyResult.getVersions().size() > 0) {
         for (final Version version : dependencyResult.getVersions().values()) {
            for (final Entry<ChangedEntity, TestSet> dependency : version.getChangedClazzes().entrySet()) {
               final ChangedEntity callee = dependency.getKey();
               for (final Entry<ChangedEntity, List<String>> testcase : dependency.getValue().getTestcases().entrySet()) {
                  for (final String testMethod : testcase.getValue()) {
                     final Map<ChangedEntity, Set<String>> calledClasses = new HashMap<>();
                     final Set<String> methods = new HashSet<>();
                     methods.add(callee.getMethod());
                     calledClasses.put(new ChangedEntity(callee.getClazz(), callee.getModule()), methods);
                     final ChangedEntity testClazz = testcase.getKey();
                     dependencyManager.addDependencies(new ChangedEntity(testClazz.getClazz(), testClazz.getModule(), testMethod), calledClasses);

                  }
               }
            }
         }
         DependencyReaderUtil.write(dependencyResult, dependencyFile);
      }
      checkCorrectness();
      lastRunningVersion = iterator.getTag();

      LOG.debug("Analysiere {} Einträge", iterator.getSize());
      return dependencyManager;
   }

   private void checkCorrectness() {
      for (final Entry<ChangedEntity, CalledMethods> entry : dependencyMap.getDependencyMap().entrySet()) {
         if (entry.getKey().getModule() == null) {
            throw new RuntimeException("Entry " + entry.getKey() + " has null module!");
         }
      }
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
         if (dependencyManager.getExecutor().isAndroid()) {
            dependencyResult.setAndroid(true);
         }
         LOG.error("Version not running");
         final Version newVersionInfo = new Version();
         newVersionInfo.setRunning(false);
         dependencyResult.getVersions().put(version, newVersionInfo);
         return 0;
      }

      final ModuleClassMapping mapping = new ModuleClassMapping(dependencyManager.getExecutor());
      dependencyManager.getExecutor().loadClasses();

      final Map<ChangedEntity, ClazzChangeData> changes;
      String predecessor;
      if (iterator.isPredecessor(lastRunningVersion)) {
         changes = changeManager.getChanges(null);
         predecessor = lastRunningVersion + "~1";
      } else {
         changes = changeManager.getChanges(lastRunningVersion);
         predecessor = lastRunningVersion;
      }
      changeManager.saveOldClasses();
      lastRunningVersion = iterator.getTag();

      if (DETAIL_DEBUG) {
         OBJECTMAPPER.writeValue(new File(DEBUG_FOLDER, "initialdependencies_" + version + ".json"), dependencyManager.getDependencyMap());
         OBJECTMAPPER.writeValue(new File(DEBUG_FOLDER, "changes_" + version + ".json"), changes);
      }

      if (changes.size() > 0) {
         final ChangeTestMapping changeTestMap = DependencyReaderUtil.getChangeTestMap(dependencyManager.getDependencyMap(), changes); // tells which tests need to be run, and
                                                                                                                                       // because of
         LOG.debug("Change test mapping (without added tests): " + changeTestMap);
         // which change they need to be run

         if (DETAIL_DEBUG)
            OBJECTMAPPER.writeValue(new File(DEBUG_FOLDER, "changetest_" + version + ".json"), changeTestMap);

         final Version newVersionInfo = DependencyReaderUtil.createVersionFromChangeMap(version, changes, changeTestMap);
         newVersionInfo.setJdk(dependencyManager.getExecutor().getJDKVersion());
         newVersionInfo.setPredecessor(predecessor);

         if (DETAIL_DEBUG) {
            OBJECTMAPPER.writeValue(new File(DEBUG_FOLDER, "versioninfo_" + version + ".json"), newVersionInfo);
            DependencyReaderUtil.write(newVersionInfo, new File(DEBUG_FOLDER, "versioninfo_" + version + ".xml"));
         }

         LOG.debug("Updating dependencies.. {}", version);

         final TestSet testsToRun = dependencyManager.getTestsToRun(changes); // contains only the tests that need to be run -> could be changeTestMap.values() und dann umwandeln
         OBJECTMAPPER.writeValue(new File(DEBUG_FOLDER, "toRun_" + version + ".json"), testsToRun.entrySet());
         if (testsToRun.classCount() > 0) {
            dependencyManager.runTraceTests(testsToRun, version);
            final TestExistenceChanges testExistenceChanges = dependencyManager.updateDependencies(testsToRun, version, mapping);
            final Map<ChangedEntity, Set<ChangedEntity>> newTestcases = testExistenceChanges.getAddedTests();

            if (DETAIL_DEBUG) {
               OBJECTMAPPER.writeValue(new File(DEBUG_FOLDER, "add_" + version + ".json"), newTestcases);
               OBJECTMAPPER.writeValue(new File(DEBUG_FOLDER, "remove_" + version + ".json"), testExistenceChanges.getRemovedTests());
            }

            DependencyReaderUtil.removeDeletedTestcases(newVersionInfo, testExistenceChanges);
            DependencyReaderUtil.addNewTestcases(newVersionInfo, newTestcases);

            if (DETAIL_DEBUG)
               OBJECTMAPPER.writeValue(new File(DEBUG_FOLDER, "final_" + version + ".json"), newVersionInfo);

            dependencyResult.getVersions().put(version, newVersionInfo);
            return newVersionInfo.getChangedClazzes().values().stream().mapToInt(value -> {
               return value.getTestcases().values().stream().mapToInt(list -> list.size()).sum();
            })
                  .sum();
         } else {
            return testsToRun.classCount();
         }
      } else {
         skippedNoChange.addVersion(version, "No Change at all");
         return 0;
      }
   }

   public boolean readInitialVersion() throws IOException, InterruptedException, XmlPullParserException {
      if (!dependencyManager.initialyGetTraces(iterator.getTag())) {
         return false;
      }
      dependencyMap = dependencyManager.getDependencyMap();
      final InitialVersion initialversion = createInitialVersion(iterator.getTag(), dependencyMap, dependencyManager.getExecutor().getJDKVersion());
      dependencyResult.setInitialversion(initialversion);
      DependencyReaderUtil.write(dependencyResult, dependencyFile);
      lastRunningVersion = iterator.getTag();
      return true;
   }
}
