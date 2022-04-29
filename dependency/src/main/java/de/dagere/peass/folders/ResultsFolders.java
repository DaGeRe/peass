package de.dagere.peass.folders;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class ResultsFolders {

   private static final Logger LOG = LogManager.getLogger(ResultsFolders.class);

   public static final String MEASUREMENT_PREFIX = "measurement_";
   public static final String STATIC_SELECTION_PREFIX = "staticTestSelection_";
   public static final String TRACE_SELECTION_PREFIX = "traceTestSelection_";

   private final File resultFolder;
   private final String projectName;

   public ResultsFolders(final File resultFolder, final String projectName) {
      this.resultFolder = resultFolder;
      resultFolder.mkdirs();
      this.projectName = projectName;
   }

   public File getStaticTestSelectionFile() {
      //TODO Remove compatibility to old file names after next snapshot release
      File oldFileName = new File(resultFolder, "deps_" + projectName + ".json");
      if (oldFileName.exists()) {
         return oldFileName;
      } else {
         return new File(resultFolder, STATIC_SELECTION_PREFIX + projectName + ".json");
      }

   }

   public File getTraceTestSelectionFile() {
      File oldFileName = new File(resultFolder, "execute_" + projectName + ".json");
      if (oldFileName.exists()) {
         return oldFileName;
      } else {
         return new File(resultFolder, TRACE_SELECTION_PREFIX + projectName + ".json");
      }
   }

   public File getCoverageSelectionFile() {
      return new File(resultFolder, "coverageSelection_" + projectName + ".json");
   }

   public File getCoverageInfoFile() {
      return new File(resultFolder, "coverageInfo_" + projectName + ".json");
   }

   public File getStatisticsFile() {
      return new File(resultFolder, "statistics.json");
   }

   public File getChangeFile() {
      return new File(resultFolder, "changes.json");
   }

   public File getRtsLogFolder() {
      File folder = new File(resultFolder, "rtsLogs");
      return folder;
   }

   public File getSourceReadLogFolder() {
      File folder = new File(resultFolder, "sourceReadLogs");
      return folder;
   }

   public File getDependencyLogFile(final String commit, final String commitOld) {
      File folder = getRtsLogFolder();
      if (!folder.exists()) {
         folder.mkdirs();
      }
      File logFile = new File(folder, commit + "_" + commitOld + ".txt");
      return logFile;
   }

   public File getSourceReadLogFile(final String commit, final String commitOld) {
      File folder = getSourceReadLogFolder();
      if (!folder.exists()) {
         folder.mkdirs();
      }
      File logFile = new File(folder, commit + "_" + commitOld + ".txt");
      return logFile;
   }

   public File getMeasurementLogFolder() {
      File folder = new File(resultFolder, "measurementLogs");
      return folder;
   }

   public File getMeasurementLogFile(final String commit, final String commitOld) {
      File folder = getMeasurementLogFolder();
      if (!folder.exists()) {
         folder.mkdirs();
      }
      File logFile = new File(folder, commit + "_" + commitOld + ".txt");
      return logFile;
   }

   public File getRCALogFolder() {
      File folder = new File(resultFolder, "rcaLogs");
      return folder;
   }

   public File getRCALogFile(final String commit, final String commitOld) {
      File folder = getRCALogFolder();
      if (!folder.exists()) {
         folder.mkdirs();
      }
      File logFile = new File(folder, commit + "_" + commitOld + ".txt");
      return logFile;
   }

   public File getVersionFullResultsFolder(final String commit, final String commitOld) {
      if (commit == null) {
         throw new RuntimeException("Commit must not be null!");
      }
      if (commitOld == null) {
         throw new RuntimeException("CommitOld must not be null!");
      }
      final File fullResultsVersion = new File(resultFolder, MEASUREMENT_PREFIX + commit + "_" + commitOld);
      return fullResultsVersion;
   }

   public File getViewFolder() {
      File viewFolder = new File(resultFolder, "views_" + projectName);
      viewFolder.mkdirs();
      return viewFolder;
   }

   public File getPropertiesFile() {
      return new File(getPropertiesFolder(), "properties.json");
   }

   public File getVersionDiffFolder(final String commit) {
      File diffsFolder = new File(getCommitViewFolder(commit), "diffs");
      diffsFolder.mkdirs();
      return diffsFolder;
   }

   public File getCommitViewFolder(final String commit) {
      File commitViewFolder = new File(getViewFolder(), "view_" + commit);
      commitViewFolder.mkdirs();
      return commitViewFolder;
   }

   public File getViewMethodDir(final String commit, final TestCase testcase) {
      final File methodDir = new File(getClazzDir(commit, testcase), testcase.getMethodWithParams());
      if (!methodDir.exists()) {
         boolean create = methodDir.mkdirs();
         LOG.debug("Created directory {} Success: {}", methodDir.getAbsolutePath(), create);
      } else {
         LOG.debug("Directory {} already existing", methodDir.getAbsolutePath());
      }
      return methodDir;
   }

   public File getClazzDir(final String commit, final TestCase testcase) {
      final File viewResultsFolder = new File(getViewFolder(), "view_" + commit);
      if (!viewResultsFolder.exists()) {
         viewResultsFolder.mkdir();
      }
      String clazzDirName = (testcase.getModule() != null && !testcase.getModule().equals("")) ? testcase.getModule() + ChangedEntity.MODULE_SEPARATOR + testcase.getClazz()
            : testcase.getClazz();
      final File clazzDir = new File(viewResultsFolder, clazzDirName);
      if (!clazzDir.exists()) {
         clazzDir.mkdir();
      }
      return clazzDir;
   }

   public File getPropertiesFolder() {
      File propertyFolder = new File(resultFolder, "properties_" + projectName);
      propertyFolder.mkdirs();
      return propertyFolder;
   }

   public File getVersionFullResultsFolder(final MeasurementConfig measurementConfig) {
      return getVersionFullResultsFolder(measurementConfig.getExecutionConfig().getCommit(), measurementConfig.getExecutionConfig().getCommitOld());
   }

   /**
    * Returns the *regular* place of the project folder and its data folders. These is only where the folders typically reside - the returned folder may, based on the type of run,
    * not be in this place.
    * 
    * @return
    */
   public CauseSearchFolders getPeassFolders() {
      File folder = new File(resultFolder, projectName);
      if (folder.exists()) {
         return new CauseSearchFolders(folder);
      } else {
         return null;
      }
   }

   public String getProjectName() {
      return projectName;
   }
}
