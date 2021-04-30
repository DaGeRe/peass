package de.dagere.peass.dependency.reader;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.ci.NonIncludedTestRemover;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.DependencyManager;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestExistenceChanges;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.Version;
import de.dagere.peass.utils.Constants;

public class TraceChangeHandler {

   private static final boolean DETAIL_DEBUG = true;

   private static final Logger LOG = LogManager.getLogger(TraceChangeHandler.class);

   private final DependencyManager dependencyManager;
   private final PeASSFolders folders;
   private final ExecutionConfig executionConfig;
   private final String version;

   public TraceChangeHandler(final DependencyManager dependencyManager, final PeASSFolders folders, final ExecutionConfig executionConfig,
         final String version) {
      this.dependencyManager = dependencyManager;
      this.folders = folders;
      this.executionConfig = executionConfig;
      this.version = version;
   }

   public void handleTraceAnalysisChanges(final DependencyReadingInput input, final Version newVersionInfo)
         throws IOException, JsonGenerationException, JsonMappingException, XmlPullParserException, InterruptedException {
      LOG.debug("Updating dependencies.. {}", version);

      final TestSet testsToRun = getTestsToRun(input);

      if (testsToRun.classCount() > 0) {
         analyzeTests(newVersionInfo, testsToRun);
      }
   }

   private TestSet getTestsToRun(final DependencyReadingInput input) throws IOException, JsonGenerationException, JsonMappingException {
      final TestSet testsToRun = dependencyManager.getTestsToRun(input.getChanges()); // contains only the tests that need to be run -> could be changeTestMap.values() und dann
                                                                                      // umwandeln
      Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "toRun_" + version + ".json"), testsToRun.entrySet());

      NonIncludedTestRemover.removeNotIncluded(testsToRun, executionConfig);
      return testsToRun;
   }

   private void analyzeTests(final Version newVersionInfo, final TestSet testsToRun)
         throws IOException, XmlPullParserException, InterruptedException, JsonGenerationException, JsonMappingException {
      final ModuleClassMapping mapping = new ModuleClassMapping(dependencyManager.getExecutor());
      dependencyManager.runTraceTests(testsToRun, version);

      handleDependencyChanges(newVersionInfo, testsToRun, mapping);
      
      //TODO Generate views if flag is set
//      Map<String, List<File>> traceFileMap = new HashMap<>();
//      for (TestCase testcase : testsToRun.getTests()) {
//         dependencyManager.getExecutor().getModules();
//         final File moduleFolder = KiekerFolderUtil.getModuleResultFolder(folders, testcase);
//         final OneTraceGenerator oneViewGenerator = new OneTraceGenerator(viewFolder, folders, testcase, traceFileMap, version, moduleFolder,
//               dependencyManager.getExecutor().getModules());
//         final boolean workedLocal = oneViewGenerator.generateTrace(version);
//      }
      
   }

   private void handleDependencyChanges(final Version newVersionInfo, final TestSet testsToRun, final ModuleClassMapping mapping)
         throws IOException, XmlPullParserException, JsonGenerationException, JsonMappingException {
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
}
