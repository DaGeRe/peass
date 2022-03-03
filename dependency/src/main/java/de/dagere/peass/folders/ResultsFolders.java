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

   private final File resultFolder;
   private final String projectName;

   public ResultsFolders(final File resultFolder, final String projectName) {
      this.resultFolder = resultFolder;
      resultFolder.mkdirs();
      this.projectName = projectName;
   }

   public File getDependencyFile() {
      return new File(resultFolder, "deps_" + projectName + ".json");
   }

   public File getExecutionFile() {
      return new File(resultFolder, "execute_" + projectName + ".json");
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

   public File getDependencyLogFile(final String version, final String versionOld) {
      File folder = getRtsLogFolder();
      if (!folder.exists()) {
         folder.mkdirs();
      }
      File logFile = new File(folder, version + "_" + versionOld + ".txt");
      return logFile;
   }

   public File getSourceReadLogFile(final String version, final String versionOld) {
      File folder = getSourceReadLogFolder();
      if (!folder.exists()) {
         folder.mkdirs();
      }
      File logFile = new File(folder, version + "_" + versionOld + ".txt");
      return logFile;
   }

   public File getMeasurementLogFolder() {
      File folder = new File(resultFolder, "measurementLogs");
      return folder;
   }

   public File getMeasurementLogFile(final String version, final String versionOld) {
      File folder = getMeasurementLogFolder();
      if (!folder.exists()) {
         folder.mkdirs();
      }
      File logFile = new File(folder, version + "_" + versionOld + ".txt");
      return logFile;
   }

   public File getRCALogFolder() {
      File folder = new File(resultFolder, "rcaLogs");
      return folder;
   }

   public File getRCALogFile(final String version, final String versionOld) {
      File folder = getRCALogFolder();
      if (!folder.exists()) {
         folder.mkdirs();
      }
      File logFile = new File(folder, version + "_" + versionOld + ".txt");
      return logFile;
   }

   public File getVersionFullResultsFolder(final String version, final String versionOld) {
      if (version == null) {
         throw new RuntimeException("Version must not be null!");
      }
      if (versionOld == null) {
         throw new RuntimeException("VersionOld must not be null!");
      }
      final File fullResultsVersion = new File(resultFolder, MEASUREMENT_PREFIX + version + "_" + versionOld);
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

   public File getVersionDiffFolder(final String version) {
      File diffsFolder = new File(getVersionViewFolder(version), "diffs");
      diffsFolder.mkdirs();
      return diffsFolder;
   }

   public File getVersionViewFolder(final String version) {
      File versionViewFolder = new File(getViewFolder(), "view_" + version);
      versionViewFolder.mkdirs();
      return versionViewFolder;
   }

   public File getViewMethodDir(final String version, final TestCase testcase) {
      final File methodDir = new File(getClazzDir(version, testcase), testcase.getMethodWithParams());
      if (!methodDir.exists()) {
         boolean create = methodDir.mkdirs();
         LOG.debug("Created directory {} Success: {}", methodDir.getAbsolutePath(), create);
      } else {
         LOG.debug("Directory {} already existing", methodDir.getAbsolutePath());
      }
      return methodDir;
   }

   public File getClazzDir(final String version, final TestCase testcase) {
      final File viewResultsFolder = new File(getViewFolder(), "view_" + version);
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
      return getVersionFullResultsFolder(measurementConfig.getExecutionConfig().getVersion(), measurementConfig.getExecutionConfig().getVersionOld());
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
}
