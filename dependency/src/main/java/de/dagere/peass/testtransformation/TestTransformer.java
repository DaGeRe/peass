package de.dagere.peass.testtransformation;

import java.io.File;
import java.util.List;
import java.util.Set;

import de.dagere.nodeDiffDetector.data.TestClazzCall;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.RunnableTestInformation;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.execution.utils.ProjectModules;

/**
 * Defines what should be done with tests; based on this class, extensions might define how to handle their tests. This might include: - Searching for test classes (*Test* or
 * *Benchmark*) - Definition of changes (e.g. adding specific source code to the test calls)
 * 
 * @author reichelt
 *
 */
public interface TestTransformer {

   /**
    * Returns a test set containing all modules of the test
    * 
    * @param mapping
    * @param includedModules
    * @param modules
    * @return
    */
   public TestSet findModuleTests(final ModuleClassMapping mapping, final List<String> includedModules, final ProjectModules modules);

   public RunnableTestInformation buildTestMethodSet(final TestSet testsToUpdate, ModuleClassMapping mapping);

   public void determineVersions(final List<File> modules);

   public MeasurementConfig getConfig();

   public JUnitVersions getJUnitVersions();

   public void setIgnoreEOIs(boolean ignoreEOIs);

   public boolean isIgnoreEOIs();

   public Set<TestMethodCall> getTestMethodNames(File module, TestClazzCall entity);

}
