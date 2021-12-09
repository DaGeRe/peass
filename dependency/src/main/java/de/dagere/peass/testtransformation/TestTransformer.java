package de.dagere.peass.testtransformation;

import java.io.File;
import java.util.List;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.execution.utils.ProjectModules;

/**
 * Defines what should be done with tests; based on this class, extensions might define how to handle their tests.
 * This might include:
 * - Searching for test classes (*Test* or *Benchmark*)
 * - Definition of changes (e.g. adding specific source code to the test calls)
 * @author reichelt
 *
 */
public interface TestTransformer {
   
   /**
    * Returns a test set containing all modules of the test
    * @param mapping
    * @param includedModules
    * @param modules
    * @return
    */
   public TestSet findModuleTests(final ModuleClassMapping mapping, final List<String> includedModules, final ProjectModules modules);

   public TestSet buildTestMethodSet(final TestSet testsToUpdate, List<File> modules);
   
   public void determineVersions(final List<File> modules);
   
   public MeasurementConfig getConfig();

   public boolean isAggregatedWriter();

   public void setAggregatedWriter(boolean useAggregation);
   
   public boolean isJUnit3();

   public void setIgnoreEOIs(boolean ignoreEOIs);
   
   public boolean isIgnoreEOIs();

   public List<TestCase> getTestMethodNames(File module, ChangedEntity entity);
   
}
