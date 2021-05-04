package de.dagere.peass.testtransformation;

import java.io.File;
import java.util.List;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.execution.ProjectModules;

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
   
   public MeasurementConfiguration getConfig();

   public boolean isAggregatedWriter();

   public boolean isJUnit3();
}
