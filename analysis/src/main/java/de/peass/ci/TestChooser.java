package de.peass.ci;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.analysis.properties.PropertyReader;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependency.persistence.Version;
import de.peass.dependency.traces.ViewGenerator;
import de.peass.utils.Constants;

public class TestChooser {

   private static final Logger LOG = LogManager.getLogger(TestChooser.class);

   private final boolean useViews;
   private File localFolder;
   private PeASSFolders folders;
   private String version;
   private final File viewFolder;
   private final File propertyFolder;
   private final int threads;
   private final List<String> includes;

   public TestChooser(final boolean useViews, final File localFolder, final PeASSFolders folders, final String version, final File viewFolder, final File propertyFolder,
         final int threads, final List<String> includes) {
      this.useViews = useViews;
      this.localFolder = localFolder;
      this.folders = folders;
      this.version = version;
      this.viewFolder = viewFolder;
      this.propertyFolder = propertyFolder;
      this.threads = threads;
      this.includes = includes;
   }

   public Set<TestCase> getTestSet(final Dependencies dependencies) throws Exception {
      final String versionName = dependencies.getVersionNames()[dependencies.getVersions().size() - 1];
      final Version currentVersion = dependencies.getVersions().get(versionName);

      final Set<TestCase> tests = new HashSet<>();

      if (useViews) {
         final TestSet traceTestSet = getViewTests(dependencies);
         if (traceTestSet == null) {
            LOG.error("No tests were included!");
         } else {
            for (final Map.Entry<ChangedEntity, Set<String>> test : traceTestSet.getTestcases().entrySet()) {
               for (final String method : test.getValue()) {
                  TestCase dynamicallySelectedTest = new TestCase(test.getKey().getClazz(), method, test.getKey().getModule());
                  tests.add(dynamicallySelectedTest);
               }
            }
         }
      } else {
         for (final TestSet dep : currentVersion.getChangedClazzes().values()) {
            tests.addAll(dep.getTests());
         }
      }

      if (includes.size() > 0) {
         removeNotIncluded(tests);
      }
      return tests;
   }

   private void removeNotIncluded(final Set<TestCase> tests) {
      for (Iterator<TestCase> it = tests.iterator(); it.hasNext();) {
         TestCase test = it.next();
         boolean isIncluded = isTestIncluded(test, includes);
         if (!isIncluded) {
            LOG.info("Excluding non-included test {}", test);
            it.remove();
         }
      }
   }

   public static boolean isTestIncluded(final TestCase test, final List<String> includes) {
      if (includes.size() == 0) {
         return true;
      }
      boolean isIncluded = false;
      for (String include : includes) {
         boolean match = FilenameUtils.wildcardMatch(test.getExecutable(), include);
         LOG.info("Testing {} {} {}", test.getExecutable(), include, match);
         if (match) {
            isIncluded = true;
            break;
         }
      }
      return isIncluded;
   }

   private TestSet getViewTests(final Dependencies dependencies)
         throws Exception {
      final File executeFile = new File(localFolder, "execute.json");

      FileUtils.deleteDirectory(folders.getTempMeasurementFolder());
      if (!executeFile.exists()) {
         LOG.debug("Expected file {} does not exist, executing view creation", executeFile);
         generateViews(dependencies, executeFile);
      }
      ExecutionData traceTests = Constants.OBJECTMAPPER.readValue(executeFile, ExecutionData.class);
      if (!traceTests.getVersions().containsKey(version)) {
         generateViews(dependencies, executeFile);
         traceTests = Constants.OBJECTMAPPER.readValue(executeFile, ExecutionData.class);
      }

      LOG.debug("Version: {} Path: {}", version, executeFile.getAbsolutePath());
      final TestSet traceTestSet = traceTests.getVersions().get(version);

      return traceTestSet;
   }

   private void generateViews(final Dependencies dependencies, final File executeFile) throws Exception {
      File logFile = new File(executeFile.getParentFile(), "executionreading_" + version + ".txt");
      LOG.info("Executig regression test selection (part 2) - Log goes to {}", logFile.getAbsolutePath());

      try (LogRedirector director = new LogRedirector(logFile)) {
         final ViewGenerator viewgenerator = new ViewGenerator(folders.getProjectFolder(), dependencies, executeFile, viewFolder, threads, 15);
         viewgenerator.processCommandline();
         final PropertyReader propertyReader = new PropertyReader(propertyFolder, folders.getProjectFolder(), viewFolder);
         propertyReader.readAllTestsProperties(viewgenerator.getChangedTraceMethods());
      }
   }
}
