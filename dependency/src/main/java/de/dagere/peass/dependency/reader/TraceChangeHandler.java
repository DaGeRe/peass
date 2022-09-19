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

import de.dagere.peass.ci.NonIncludedByRule;
import de.dagere.peass.ci.NonIncludedTestRemover;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.DependencyManager;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestExistenceChanges;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.testData.TestClazzCall;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.TestTransformer;
import de.dagere.peass.utils.Constants;

public class TraceChangeHandler {

   private static final boolean DETAIL_DEBUG = true;

   private static final Logger LOG = LogManager.getLogger(TraceChangeHandler.class);

   private final DependencyManager dependencyManager;
   private final PeassFolders folders;
   private final ExecutionConfig executionConfig;
   private final String commit;

   public TraceChangeHandler(final DependencyManager dependencyManager, final PeassFolders folders, final ExecutionConfig executionConfig,
         final String commit) {
      this.dependencyManager = dependencyManager;
      this.folders = folders;
      this.executionConfig = executionConfig;
      this.commit = commit;
   }

   public void handleTraceAnalysisChanges(final CommitStaticSelection newCommitInfo)
         throws IOException, JsonGenerationException, JsonMappingException, XmlPullParserException, InterruptedException {
      LOG.debug("Updating dependencies.. {}", commit);

      final ModuleClassMapping mapping = new ModuleClassMapping(dependencyManager.getExecutor());
      final TestSet testsToRun = getTestsToRun(newCommitInfo, mapping);

      if (testsToRun.classCount() > 0) {
         analyzeTests(newCommitInfo, testsToRun, mapping);
      }
   }

   private TestSet getTestsToRun(final CommitStaticSelection newCommitStaticSelection, ModuleClassMapping mapping) throws IOException, JsonGenerationException, JsonMappingException {
      final TestSet testsToRun = newCommitStaticSelection.getTests() ; // contains only the tests that need to be run -> could be changeTestMap.values() und dann
                                                                                      // umwandeln
      addAddedTests(newCommitStaticSelection, testsToRun);
      
      Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "toRun_" + commit + ".json"), testsToRun.entrySet());

      NonIncludedTestRemover.removeNotIncluded(testsToRun, executionConfig);
      
      TestTransformer testTransformer = dependencyManager.getTestTransformer();
      NonIncludedByRule.removeNotIncluded(testsToRun, testTransformer, mapping);
      
      return testsToRun;
   }

   public void addAddedTests(final CommitStaticSelection newVersionInfo, final TestSet testsToRun) {
      for (final ChangedEntity testName : newVersionInfo.getChangedClazzes().keySet()) {
         ChangedEntity simplyClazz = testName.getSourceContainingClazz();
         TestClazzCall potentialTest = new TestClazzCall(simplyClazz.getClazz(), testName.getModule());
         if (NonIncludedTestRemover.isTestClassIncluded(potentialTest, executionConfig)) {
            testsToRun.addTest(potentialTest, null);
         }
      }
   }

   private void analyzeTests(final CommitStaticSelection newCommitInfo, final TestSet testsToRun, ModuleClassMapping mapping)
         throws IOException, XmlPullParserException {
      
      dependencyManager.runTraceTests(testsToRun, commit);

      handleDependencyChanges(newCommitInfo, testsToRun, mapping);
      
      if (dependencyManager.getIgnoredTests().getTestMethods().size() > 0) {
         newCommitInfo.setIgnoredAffectedTests(dependencyManager.getIgnoredTests());
      }
      
   }

   private void handleDependencyChanges(final CommitStaticSelection newVersionStaticSelection, final TestSet testsToRun, final ModuleClassMapping mapping)
         throws IOException, XmlPullParserException {
      final TestExistenceChanges testExistenceChanges = dependencyManager.updateDependencies(testsToRun, mapping);
      final Map<ChangedEntity, Set<TestMethodCall>> addedTestcases = testExistenceChanges.getAddedTests();

      if (DETAIL_DEBUG) {
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "add_" + commit + ".json"), addedTestcases);
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "remove_" + commit + ".json"), testExistenceChanges.getRemovedTests());
      }

      DependencyReaderUtil.removeDeletedTestcases(newVersionStaticSelection, testExistenceChanges);
      DependencyReaderUtil.addNewTestcases(newVersionStaticSelection, addedTestcases);

      if (DETAIL_DEBUG)
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "finalStaticSelection_" + commit + ".json"), newVersionStaticSelection);
   }
}
