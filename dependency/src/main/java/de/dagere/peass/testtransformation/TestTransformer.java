package de.dagere.peass.testtransformation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

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
   
   public TestSet findModuleTests(final ModuleClassMapping mapping, final List<String> includedModules, final ProjectModules modules) throws FileNotFoundException, IOException, XmlPullParserException;

   public TestSet buildTestMethodSet(final TestSet testsToUpdate, List<File> modules) throws IOException, XmlPullParserException;
   
   public MeasurementConfiguration getConfig();

   public void determineVersions(List<File> modules);
}
