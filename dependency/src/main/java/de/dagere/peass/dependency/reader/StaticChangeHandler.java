package de.dagere.peass.dependency.reader;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.ci.NonIncludedTestRemover;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.DependencyManager;
import de.dagere.peass.dependency.analysis.data.ChangeTestMapping;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.changesreading.ClazzChangeData;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.utils.Constants;

public class StaticChangeHandler {
   
   private static final Logger LOG = LogManager.getLogger(StaticChangeHandler.class);
   
   private final PeassFolders folders;
   private final ExecutionConfig executionConfig;
   private final DependencyManager dependencyManager;
   
   public StaticChangeHandler(PeassFolders folders, ExecutionConfig executionConfig, DependencyManager dependencyManager) {
      this.folders = folders;
      this.executionConfig = executionConfig;
      this.dependencyManager = dependencyManager;
   }

   public VersionStaticSelection handleStaticAnalysisChanges(final String version, final DependencyReadingInput input) throws IOException, JsonGenerationException, JsonMappingException {
      final ChangeTestMapping changeTestMap = dependencyManager.getDependencyMap().getChangeTestMap(input.getChanges()); // tells which tests need to be run, and
      // because of which change they need to be run
      LOG.debug("Change test mapping (without added tests): " + changeTestMap);

      handleAddedTests(input, changeTestMap);

      if (executionConfig.isCreateDetailDebugFiles())
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "changeTestMap_" + version + ".json"), changeTestMap);

      final VersionStaticSelection newVersionStaticSelection = DependencyReaderUtil.createVersionFromChangeMap(input.getChanges(), changeTestMap);
      newVersionStaticSelection.setJdk(dependencyManager.getExecutor().getJDKVersion());
      newVersionStaticSelection.setPredecessor(input.getPredecessor());

      if (executionConfig.isCreateDetailDebugFiles()) {
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "versionStaticSelection_" + version + ".json"), newVersionStaticSelection);
      }
      return newVersionStaticSelection;
   }

   private void handleAddedTests(final DependencyReadingInput input, final ChangeTestMapping changeTestMap) {
      dependencyManager.getTestTransformer().determineVersions(dependencyManager.getExecutor().getModules().getModules());
      for (ClazzChangeData changedEntry : input.getChanges().values()) {
         if (!changedEntry.isOnlyMethodChange()) {
            for (ChangedEntity change : changedEntry.getChanges()) {
               File moduleFolder = new File(folders.getProjectFolder(), change.getModule());
               TestCase potentialTest = new TestCase(change.getClazz(), change.getMethod(), change.getModule());
               List<TestCase> addedTests = dependencyManager.getTestTransformer().getTestMethodNames(moduleFolder, potentialTest);
               for (TestCase added : addedTests) {
                  if (NonIncludedTestRemover.isTestIncluded(added, executionConfig)) {
                     changeTestMap.addChangeEntry(change, added);
                  }
               }
            }
         }
      }
   }
}
