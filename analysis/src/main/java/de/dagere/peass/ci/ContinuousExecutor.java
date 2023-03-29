package de.dagere.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.analysis.changes.ChangeReader;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.analysis.measurement.ProjectStatistics;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.reader.CommitKeeper;
import de.dagere.peass.dependency.reader.RunningCommitFinder;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.CommitIteratorGit;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionControlSystem;

public class ContinuousExecutor {

   private static final Logger LOG = LogManager.getLogger(ContinuousExecutor.class);

   private final MeasurementConfig measurementConfig;
   private final TestSelectionConfig dependencyConfig;

   private final String commit;
   private final String commitOld;
   private final CommitIteratorGit iterator;
   private final CommitComparatorInstance comparator;

   private final File originalProjectFolder;
   private final File localFolder;
   private final PeassFolders folders;
   private final ResultsFolders resultsFolders;

   private final EnvironmentVariables env;

   public ContinuousExecutor(final File projectFolder, final MeasurementConfig measurementConfig, final TestSelectionConfig dependencyConfig, final EnvironmentVariables env)
         throws IOException {
      this.originalProjectFolder = projectFolder;
      this.measurementConfig = measurementConfig;
      this.dependencyConfig = dependencyConfig;
      this.env = env;
      LOG.info("Properties: " + env.getProperties());

      File vcsFolder = VersionControlSystem.findVCSFolder(projectFolder);
      localFolder = ContinuousFolderUtil.getLocalFolder(vcsFolder);
      String projectName = ContinuousFolderUtil.getSubFolderPath(projectFolder);
      File projectFolderLocal = new File(localFolder, projectName);
      getGitRepo(projectFolder, projectFolderLocal);
      resultsFolders = new ResultsFolders(localFolder, projectName);

      folders = new PeassFolders(projectFolderLocal);

      StaticTestSelection dependencies = null;
      if (resultsFolders.getStaticTestSelectionFile().exists()) {
         dependencies = Constants.OBJECTMAPPER.readValue(resultsFolders.getStaticTestSelectionFile(), StaticTestSelection.class);
      }

      CommitIteratorBuilder iteratorBuiler = new CommitIteratorBuilder(measurementConfig.getFixedCommitConfig(), dependencies, folders);
      commit = iteratorBuiler.getCommit();
      String userDefinedCommitOld = iteratorBuiler.getCommitOld();
      measurementConfig.getFixedCommitConfig().setCommit(commit);

      LOG.debug("Commit: {} Predecessor Commit: {}", commit, userDefinedCommitOld);

      List<String> commits = GitUtils.getCommits(projectFolderLocal, false, measurementConfig.getExecutionConfig().isLinearizeHistory());
      comparator = new CommitComparatorInstance(commits);

      commitOld = getLatestRunnableCommit(measurementConfig, env, userDefinedCommitOld);
      measurementConfig.getFixedCommitConfig().setCommitOld(commitOld);

      if (!iteratorBuiler.isLatestCommitWasAnalyzed()) {
         iterator = new CommitIteratorGit(projectFolderLocal, Arrays.asList(new String[] { commitOld, commit }), commitOld);
      } else {
         iterator = null;
      }

      LOG.debug("Commit: {} Predecessor Commit: {}", commit, commitOld);
   }

   private String getLatestRunnableCommit(final MeasurementConfig measurementConfig, final EnvironmentVariables env, String userDefinedCommitOld) {
      if (iterator != null) {
         File nonRunningCommitFile = new File(resultsFolders.getStaticTestSelectionFile().getParentFile(), "notRunnable.json");
         CommitKeeper commitKeeper = new CommitKeeper(nonRunningCommitFile);
         RunningCommitFinder latestRunningCommitFinder = new RunningCommitFinder(folders, commitKeeper, iterator, measurementConfig.getExecutionConfig(), env);
         iterator.goToNamedCommit(userDefinedCommitOld);
         boolean foundRunningCommit = latestRunningCommitFinder.searchLatestRunningCommit();
         if (!foundRunningCommit) {
            throw new RuntimeException("No running predecessor commit before " + userDefinedCommitOld + " was found; measurement not possible");
         }
         String latestRunnableCommit = iterator.getCommitName();
         return latestRunnableCommit;
      } else {
         return userDefinedCommitOld;
      }
   }

   public CommitIteratorGit getIterator() {
      return iterator;
   }

   private void getGitRepo(final File projectFolder, final File projectFolderLocal) throws IOException {
      if (projectFolderLocal.exists()) {
         FileUtils.deleteDirectory(projectFolderLocal);
      }
      ContinuousFolderUtil.copyProject(projectFolder, localFolder);
      if (!projectFolderLocal.exists()) {
         throw new RuntimeException("Was not able to clone project to " + projectFolderLocal.getAbsolutePath() + " (folder not existing)");
      }
   }

   public RTSResult executeRTS() {
      final String url = GitUtils.getURL(originalProjectFolder);

      RTSResult tests = executeRegressionTestSelection(url);
      return tests;
   }

   public void measure(final Set<TestMethodCall> tests) {
      try {
         File measurementFolder = executeMeasurement(tests);
         analyzeMeasurements(measurementFolder);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public void execute() {
      Set<TestMethodCall> tests = executeRTS().getTests();
      measure(tests);
   }

   protected RTSResult executeRegressionTestSelection(final String url) {
      ContinuousDependencyReader dependencyReader = new ContinuousDependencyReader(dependencyConfig, measurementConfig.getExecutionConfig(), measurementConfig.getKiekerConfig(),
            folders, resultsFolders, env);
      final RTSResult tests = dependencyReader.getTests(iterator, url, commit, measurementConfig);
      tests.setCommitOld(commitOld);

      if (iterator != null) {
         SourceReader sourceReader = new SourceReader(measurementConfig.getExecutionConfig(), commit, commitOld, resultsFolders, folders);
         sourceReader.readMethodSources(tests.getTests());
      }

      return tests;
   }

   protected File executeMeasurement(final Set<TestMethodCall> tests) throws IOException {
      final File fullResultsVersion = resultsFolders.getCommitFullResultsFolder(commit, commitOld);
      File logFile = resultsFolders.getMeasurementLogFile(commit, commitOld);
      final ContinuousMeasurementExecutor measurementExecutor = new ContinuousMeasurementExecutor(folders, measurementConfig, env, comparator);
      final File measurementFolder = measurementExecutor.executeMeasurements(tests, fullResultsVersion, logFile);
      return measurementFolder;
   }

   private void analyzeMeasurements(final File measurementFolder)
         throws IOException {
      StaticTestSelection selectedTests = Constants.OBJECTMAPPER.readValue(resultsFolders.getStaticTestSelectionFile(), StaticTestSelection.class);

      ProjectChanges changes = resultsFolders.getChangeFile().exists() ? Constants.OBJECTMAPPER.readValue(resultsFolders.getChangeFile(), ProjectChanges.class)
            : new ProjectChanges(comparator);
      ProjectStatistics statistics = resultsFolders.getStatisticsFile().exists() ? Constants.OBJECTMAPPER.readValue(resultsFolders.getStatisticsFile(), ProjectStatistics.class)
            : new ProjectStatistics(comparator);

      ChangeReader changeReader = new ChangeReader(resultsFolders, selectedTests, measurementConfig.getStatisticsConfig(), changes, statistics);
      changeReader.readFolder(measurementFolder.getParentFile());
   }

   public String getLatestCommit() {
      return commit;
   }

   public PeassFolders getFolders() {
      return folders;
   }

   public String getCommitOld() {
      LOG.debug("Commit old: {}", commitOld);
      return commitOld;
   }

   public File getProjectFolder() {
      return folders.getProjectFolder();
   }

   public File getLocalFolder() {
      return localFolder;
   }
}
