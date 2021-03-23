package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.ModuleClassMapping;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependency.persistence.Dependencies;
import de.peass.measurement.analysis.AnalyseFullData;
import de.peass.measurement.analysis.ProjectStatistics;
import de.peass.utils.Constants;
import de.peass.vcs.GitCommit;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionControlSystem;
import de.peass.vcs.VersionIteratorGit;
import de.peran.AnalyseOneTest;

public class ContinuousExecutor {

   private static final Logger LOG = LogManager.getLogger(ContinuousExecutor.class);

   private final File projectFolder;
   private final MeasurementConfiguration measurementConfig;
   private final int threads;
   private final boolean useViews;

   private String version;
   private String versionOld;

   private final File localFolder;
   private final PeASSFolders folders;
   private final File viewFolder;
   private final File propertyFolder;

   private final EnvironmentVariables env;

   public ContinuousExecutor(final File projectFolder, final MeasurementConfiguration measurementConfig, final int threads, final boolean useViews,
         final EnvironmentVariables env)
         throws InterruptedException, IOException {
      this.projectFolder = projectFolder;
      this.measurementConfig = measurementConfig;
      this.threads = threads;
      this.useViews = useViews;
      this.env = env;
      LOG.info("Properties: " + env.getProperties());

      File vcsFolder = VersionControlSystem.findVCSFolder(projectFolder);
      localFolder = ContinuousFolderUtil.getLocalFolder(vcsFolder);
      File projectFolderLocal = new File(localFolder, ContinuousFolderUtil.getSubFolderPath(projectFolder));
      if (!localFolder.exists() || !projectFolderLocal.exists()) {
         ContinuousFolderUtil.cloneProject(projectFolder, localFolder);
         if (!projectFolderLocal.exists()) {
            throw new RuntimeException("Was not able to clone project to " + projectFolderLocal.getAbsolutePath() + " (folder not existing)");
         }
      } else {
         GitUtils.pull(projectFolderLocal);
         GitUtils.goToTag(measurementConfig.getVersion(), projectFolderLocal);
      }

      folders = new PeASSFolders(projectFolderLocal);
      viewFolder = new File(localFolder, "views");
      viewFolder.mkdir();

      propertyFolder = new File(localFolder, "properties");

      version = measurementConfig.getVersion();
      versionOld = measurementConfig.getVersionOld();
   }

   public void execute() throws Exception {
      final File dependencyFile = new File(localFolder, "dependencies.json");
      final VersionIteratorGit iterator = buildIterator();
      final String url = GitUtils.getURL(projectFolder);

      ContinuousDependencyReader dependencyReader = new ContinuousDependencyReader(measurementConfig.getExecutionConfig(), folders, dependencyFile, env);
      final Dependencies dependencies = dependencyReader.getDependencies(iterator, url);

      if (dependencies.getVersions().size() > 0) {
         final Set<TestCase> tests = selectIncludedTests(dependencies);

         final File measurementFolder = measure(tests);

         analyzeMeasurements(measurementFolder);
      } else {
         LOG.info("No test executed - version did not contain changed tests.");
      }
   }

   private Set<TestCase> selectIncludedTests(final Dependencies dependencies) throws Exception {
      final TestChooser chooser = new TestChooser(useViews, localFolder, folders, version,
            viewFolder, propertyFolder, threads, measurementConfig.getExecutionConfig(), env);
      final Set<TestCase> tests = chooser.getTestSet(dependencies);
      LOG.debug("Executing measurement on {}", tests);
      return tests;
   }

   private File measure(final Set<TestCase> tests) throws IOException, InterruptedException, JAXBException, XmlPullParserException {
      final File fullResultsVersion = getFullResultsVersion();
      final ContinuousMeasurementExecutor measurementExecutor = new ContinuousMeasurementExecutor(version, versionOld, folders, measurementConfig, env);
      final File measurementFolder = measurementExecutor.executeMeasurements(tests, fullResultsVersion);
      return measurementFolder;
   }

   private void analyzeMeasurements(final File measurementFolder) throws InterruptedException, IOException, JsonGenerationException, JsonMappingException, XmlPullParserException {
      final File changefile = new File(localFolder, "changes.json");
      AnalyseOneTest.setResultFolder(new File(localFolder, version + "_graphs"));
      final ProjectStatistics statistics = new ProjectStatistics();
      ModuleClassMapping mapping = new ModuleClassMapping(projectFolder);
      final AnalyseFullData afd = new AnalyseFullData(changefile, statistics, mapping);
      afd.analyseFolder(measurementFolder);
      Constants.OBJECTMAPPER.writeValue(new File(localFolder, "statistics.json"), statistics);
   }

   private VersionIteratorGit buildIterator() {
      versionOld = GitUtils.getName(measurementConfig.getVersionOld(), folders.getProjectFolder());
      version = GitUtils.getName(measurementConfig.getVersion(), folders.getProjectFolder());

      final List<GitCommit> entries = new LinkedList<>();
      final GitCommit prevCommit = new GitCommit(versionOld, "", "", "");
      entries.add(prevCommit);
      entries.add(new GitCommit(version, "", "", ""));
      final VersionIteratorGit iterator = new VersionIteratorGit(folders.getProjectFolder(), entries, prevCommit);
      return iterator;
   }

   public String getLatestVersion() {
      return version;
   }

   public PeASSFolders getFolders() {
      return folders;
   }

   public String getVersionOld() {
      return versionOld;
   }

   public File getPropertyFolder() {
      return propertyFolder;
   }

   public File getProjectFolder() {
      return folders.getProjectFolder();
   }

   public File getFullResultsVersion() {
      final File fullResultsVersion = new File(localFolder, version + "_" + versionOld);
      return fullResultsVersion;
   }
}
