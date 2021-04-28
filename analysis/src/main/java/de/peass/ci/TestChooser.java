package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.analysis.properties.PropertyReader;
import de.peass.config.ExecutionConfig;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.execution.EnvironmentVariables;
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
   private final String version;
   private final File viewFolder;
   private final File propertyFolder;
   private final int threads;
   private final ExecutionConfig config;
   private final EnvironmentVariables env;

   public TestChooser(final boolean useViews, final File localFolder, final PeASSFolders folders, final String version, final File viewFolder, final File propertyFolder,
         final int threads, final ExecutionConfig config, final EnvironmentVariables env) {
      this.useViews = useViews;
      this.localFolder = localFolder;
      this.folders = folders;
      this.version = version;
      this.viewFolder = viewFolder;
      this.propertyFolder = propertyFolder;
      this.threads = threads;
      this.config = config;
      this.env = env;
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

      NonIncludedTestRemover.removeNotIncluded(tests, config);
      return tests;
   }

   private TestSet getViewTests(final Dependencies dependencies) throws Exception {
      final File executeFile = new File(localFolder, "execute.json");

      FileUtils.deleteDirectory(folders.getTempMeasurementFolder());

      boolean alreadyTried = false;
      if (!executeFile.exists()) {
         LOG.debug("Expected file {} does not exist, executing view creation", executeFile);
         generateViews(dependencies, executeFile);
         alreadyTried = true;
      }
      ExecutionData traceTests = Constants.OBJECTMAPPER.readValue(executeFile, ExecutionData.class);
      if (!traceTests.getVersions().containsKey(version) && !alreadyTried) {
         LOG.debug("Version {} was not contained in tests, executing view creation", version);
         generateViews(dependencies, executeFile);
         traceTests = Constants.OBJECTMAPPER.readValue(executeFile, ExecutionData.class);
      }

      LOG.debug("Version: {} Path: {}", version, executeFile.getAbsolutePath());
      final TestSet traceTestSet = traceTests.getVersions().get(version);

      return traceTestSet;
   }

   public File getExecutionreadingFolder() {
      File folder = new File(localFolder, "executionreading");
      if (!folder.exists()) {
         folder.mkdirs();
      }
      return folder;
   }

   private void generateViews(final Dependencies dependencies, final File executeFile) throws Exception {
      if (config.isRedirectSubprocessOutputToFile()) {
         File logFile = new File(getExecutionreadingFolder(), version + "_" + dependencies.getVersions().get(version).getPredecessor() + ".txt");
         LOG.info("Executig regression test selection (step 2) - Log goes to {}", logFile.getAbsolutePath());
         try (LogRedirector director = new LogRedirector(logFile)) {
            doGenerateViews(dependencies, executeFile);
         }
      } else {
         doGenerateViews(dependencies, executeFile);
      }
   }

   private void doGenerateViews(final Dependencies dependencies, final File executeFile) throws JAXBException, IOException {
      final ViewGenerator viewgenerator = new ViewGenerator(folders.getProjectFolder(), dependencies, executeFile, viewFolder, threads, config, env);
      viewgenerator.processCommandline();
      final PropertyReader propertyReader = new PropertyReader(propertyFolder, folders.getProjectFolder(), viewFolder);
      propertyReader.readAllTestsProperties(viewgenerator.getChangedTraceMethods());
   }
}
