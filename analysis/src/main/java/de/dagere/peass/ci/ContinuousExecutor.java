package de.dagere.peass.ci;

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

import de.dagere.peass.config.DependencyConfig;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.ResultsFolders;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.measurement.analysis.AnalyseFullData;
import de.dagere.peass.measurement.analysis.ProjectStatistics;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.GitCommit;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionControlSystem;
import de.dagere.peass.vcs.VersionIteratorGit;
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
   private final ResultsFolders resultsFolders;
   private DependencyConfig dependencyConfig = new DependencyConfig(2, false, true);

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
      getGitRepo(projectFolder, measurementConfig, projectFolderLocal);
      resultsFolders = new ResultsFolders(projectFolderLocal, projectFolder.getName());

      folders = new PeASSFolders(projectFolderLocal);

      version = measurementConfig.getVersion();
      versionOld = measurementConfig.getVersionOld();
   }

   private void getGitRepo(final File projectFolder, final MeasurementConfiguration measurementConfig, File projectFolderLocal) throws InterruptedException, IOException {
      if (!localFolder.exists() || !projectFolderLocal.exists()) {
         ContinuousFolderUtil.cloneProject(projectFolder, localFolder);
         if (!projectFolderLocal.exists()) {
            throw new RuntimeException("Was not able to clone project to " + projectFolderLocal.getAbsolutePath() + " (folder not existing)");
         }
      } else {
         GitUtils.reset(projectFolderLocal);
         GitUtils.clean(projectFolderLocal);
         GitUtils.pull(projectFolderLocal);
         GitUtils.goToTag(measurementConfig.getVersion(), projectFolderLocal);
      }
   }

   public void execute() throws Exception {
      final VersionIteratorGit iterator = buildIterator();
      final String url = GitUtils.getURL(projectFolder);

      ContinuousDependencyReader dependencyReader = new ContinuousDependencyReader(dependencyConfig, measurementConfig.getExecutionConfig(), folders, resultsFolders, env);
      final Dependencies dependencies = dependencyReader.getDependencies(iterator, url);

      if (dependencies.getVersions().size() > 0) {
         ExecutionData executionData = Constants.OBJECTMAPPER.readValue(resultsFolders.getExecutionFile(), ExecutionData.class);
         Set<TestCase> tests = executionData.getVersions().get(version).getTests();
//         final Set<TestCase> tests = selectIncludedTests(dependencies);
         NonIncludedTestRemover.removeNotIncluded(tests, measurementConfig.getExecutionConfig());
         
         final File measurementFolder = measure(tests);

         analyzeMeasurements(measurementFolder);
      } else {
         LOG.info("No test executed - version did not contain changed tests.");
      }
   }

//   private Set<TestCase> selectIncludedTests(final Dependencies dependencies) throws Exception {
//      final TestChooser chooser = new TestChooser(useViews, localFolder, folders, version,
//            resultsFolders, threads, measurementConfig.getExecutionConfig(), env);
//      final Set<TestCase> tests = chooser.getTestSet(dependencies);
//      LOG.debug("Executing measurement on {}", tests);
//      return tests;
//   }

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
      versionOld = GitUtils.getName(measurementConfig.getVersionOld() != null ? measurementConfig.getVersionOld() : "HEAD~1", folders.getProjectFolder());
      version = GitUtils.getName(measurementConfig.getVersion() != null ? measurementConfig.getVersion() : "HEAD", folders.getProjectFolder());

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

   public File getProjectFolder() {
      return folders.getProjectFolder();
   }

   public File getFullResultsVersion() {
      final File fullResultsVersion = new File(localFolder, version + "_" + versionOld);
      return fullResultsVersion;
   }
}
