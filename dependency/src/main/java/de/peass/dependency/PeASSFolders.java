package de.peass.dependency;

import java.io.File;
import java.io.IOException;

import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.GradleParseUtil;
import de.peass.dependency.traces.TemporaryProjectFolderUtil;
import de.peass.vcs.VersionControlSystem;

/**
 * Helps managing the PeASS-folders and their existance
 * 
 * @author reichelt
 *
 */
public class PeASSFolders {
   public static final String PEASS_POSTFIX = "_peass";
   
   protected final File projectFolder;
   // private final File resultFolder;
   protected final File fullResultFolder;
   private final File tempResultFolder, tempProjectFolder, tempFolder, kiekerTemp;
   private final File logFolder;
   private final File oldSourceFolder;
   private final File measurementsFolder;
   private final File cleanFolder;
   private final File debugFolder;
   private File gradleHome;
   private final VersionControlSystem vcs;

   protected final File peassFolder;
   private final String projectName;
   
   public static File getPeassFolder(final File projectFolder) {
      File peassFolder = new File(projectFolder, ".." + File.separator + projectFolder.getName() + PEASS_POSTFIX);
      return peassFolder;
   }
   
   public PeASSFolders(final File folder, final String name) {
      this.projectName = name;
      if (!folder.getName().endsWith(PEASS_POSTFIX)) {
         projectFolder = folder;
         peassFolder = new File(projectFolder.getParentFile(), projectFolder.getName() + PEASS_POSTFIX);
         if (!peassFolder.exists()) {
            peassFolder.mkdir();
         }
         vcs = VersionControlSystem.getVersionControlSystem(projectFolder);
      } else {
         projectFolder = null;
         vcs = null;
         peassFolder = folder;
      }

      logFolder = new File(peassFolder, "logs");
      logFolder.mkdir();
      oldSourceFolder = new File(peassFolder, "lastSources");
      fullResultFolder = new File(peassFolder, "measurementsFull");
      fullResultFolder.mkdir();
      tempFolder = new File(peassFolder, "temp");
      tempFolder.mkdir();
      cleanFolder = new File(peassFolder, "clean");
      debugFolder = new File(peassFolder, "debug");
      // cleanFolder.mkdir();
      measurementsFolder = new File(fullResultFolder, "measurements");
      measurementsFolder.mkdir();
      tempResultFolder = new File(peassFolder, "measurementsTemp");
      tempResultFolder.mkdir();
      kiekerTemp = new File(peassFolder, "kiekerTemp");
      tempProjectFolder = new File(peassFolder, "projectTemp");
   }

   public PeASSFolders(final File folder) {
      this(folder, (folder != null ? folder.getName() : null));
   }

   public File getGradleHome() {
      if (gradleHome == null) {
         final File peassFolder = new File(projectFolder.getParentFile(), projectFolder.getName() + PEASS_POSTFIX);
         gradleHome = new File(peassFolder, "gradleHome");
         gradleHome.mkdir();
         final File init = new File(gradleHome, "init.gradle");
         GradleParseUtil.writeInitGradle(init);
      }
      return gradleHome;
   }
   
   public File getPeassFolder() {
      return peassFolder;
   }

   public File getProjectFolder() {
      return projectFolder;
   }

   public File getCleanFolder() {
      return cleanFolder;
   }

   public File getLogFolder() {
      return logFolder;
   }

   public File getOldSources() {
      if (!oldSourceFolder.exists()) {
         oldSourceFolder.mkdir();
      }
      return oldSourceFolder;
   }

   public File getFullMeasurementFolder() {
      return fullResultFolder;
   }

   public File getTempMeasurementFolder() {
      return tempResultFolder;
   }

   public File getDetailResultFolder() {
      return measurementsFolder;
   }

   public File getFullSummaryFile(final TestCase testcase) {
      final String shortClazzName = testcase.getShortClazz();
      final File fullResultFile = new File(fullResultFolder, shortClazzName + "_" + testcase.getMethod() + ".xml");
      return fullResultFile;
   }
   
   public File getFullResultFolder(final TestCase testcase, final String mainVersion, final String version) {
      final File destFolder = new File(getDetailResultFolder(), testcase.getClazz());
      System.out.println("Creating: " + destFolder + " " + mainVersion + " " + testcase.getClazz());
      final File currentVersionFolder = new File(destFolder, mainVersion);
      if (!currentVersionFolder.exists()) {
         currentVersionFolder.mkdir();
      }
      final File compareVersionFolder = new File(currentVersionFolder, version);
      if (!compareVersionFolder.exists()) {
         compareVersionFolder.mkdir();
      }
      return compareVersionFolder;
   }

   public File getTempProjectFolder() {
      if (!tempProjectFolder.exists()) {
         tempProjectFolder.mkdir();
      }
      return tempProjectFolder;
   }

   public File getKiekerTempFolder() {
      if (!kiekerTemp.exists()) {
         kiekerTemp.mkdir();
      }
      return kiekerTemp;
   }

   public File getTempDir() {
      return tempFolder;
   }

   public File getDebugFolder() {
      if (!debugFolder.exists()) {
         debugFolder.mkdir();
      }
      return debugFolder;
   }
   
   public PeASSFolders getTempFolder(final String name) throws IOException, InterruptedException {
      final File nowFolder = new File(getTempProjectFolder(), name);
      PeASSFolders folders = TemporaryProjectFolderUtil.cloneForcefully(this, nowFolder);
      return folders;
   }

   public VersionControlSystem getVCS() {
      return vcs;
   }

   public String getProjectName() {
      return projectName;
   }

}
