package de.peass.dependency;

import java.io.File;

import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.GradleParseUtil;

/**
 * Helps managing the PeASS-folders and their existance
 * 
 * @author reichelt
 *
 */
public class PeASSFolders {
   protected final File projectFolder;
   // private final File resultFolder;
   protected final File fullResultFolder;
   private final File tempResultFolder, tempProjectFolder, tempFolder, kiekerTemp;
   private final File logFolder;
   private final File oldSourceFolder;
   private final File measurementsFolder;
   private final File cleanFolder;
   private File gradleHome;

   protected final File peassFolder;

   public PeASSFolders(final File folder) {
      if (!folder.getName().endsWith("_peass")) {
         projectFolder = folder;
         peassFolder = new File(projectFolder.getParentFile(), projectFolder.getName() + "_peass");
         if (!peassFolder.exists()) {
            peassFolder.mkdir();
         }
      } else {
         projectFolder = null;
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
      // cleanFolder.mkdir();
      measurementsFolder = new File(fullResultFolder, "measurements");
      measurementsFolder.mkdir();
      tempResultFolder = new File(peassFolder, "measurementsTemp");
      tempResultFolder.mkdir();
      kiekerTemp = new File(peassFolder, "kiekerTemp");
      tempProjectFolder = new File(peassFolder, "projectTemp");
      // tempProjectFolder.mkdir();
   }

   public File getGradleHome() {
      if (gradleHome == null) {
         final File peassFolder = new File(projectFolder.getParentFile(), projectFolder.getName() + "_peass");
         gradleHome = new File(peassFolder, "gradleHome");
         gradleHome.mkdir();
         final File init = new File(gradleHome, "init.gradle");
         GradleParseUtil.writeInitGradle(init);
      }
      return gradleHome;
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

   public File getFullResultFolder(final TestCase testcase, final String mainVersion, final String version) {
      final File destFolder = new File(getDetailResultFolder(), testcase.getClazz());
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

}
