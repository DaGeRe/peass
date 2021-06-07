package de.dagere.peass.dependency;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.execution.GradleParseUtil;
import de.dagere.peass.dependency.traces.TemporaryProjectFolderUtil;
import de.dagere.peass.vcs.VersionControlSystem;

/**
 * Helps managing the PeASS-folders and their existance
 * 
 * @author reichelt
 *
 */
public class PeassFolders {
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
   
   public PeassFolders(final File folder, final String name) {
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

   public PeassFolders(final File folder) {
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
      if (!logFolder.exists()) {
         logFolder.mkdirs();
      }
      return logFolder;
   }
   
   public File getLogFolder(final String version, final TestCase testcase) {
      File testLogFolder = new File(logFolder, version + File.separator + testcase.getMethod());
      if (testLogFolder.exists()) {
         testLogFolder = new File(logFolder, version + File.separator + testcase.getMethod() + "_new");
      }
      testLogFolder.mkdirs();
      return testLogFolder;
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
   
   /**
    * Searches in subfolders for a clazz folder (necessary, since submodules may have arbitraty depth)
    * @param baseFolder
    * @param folderFilter
    * @return
    */
   public List<File> findTempClazzFolder(final TestCase testcase) {
      final String expectedFolderName = "*" + testcase.getClazz();
      FileFilter folderFilter = new WildcardFileFilter(expectedFolderName);
      return findTempClazzFolder(tempResultFolder, folderFilter);
   }
   
   private List<File> findTempClazzFolder(final File baseFolder, final FileFilter folderFilter) {
      final List<File> files = new LinkedList<>();
      for (final File subfolder : baseFolder.listFiles()) {
         if (subfolder.isDirectory()) {
            if (folderFilter.accept(subfolder)) {
               files.add(subfolder);
            } else {
               files.addAll(findTempClazzFolder(subfolder, folderFilter));
            }
         }
      }
      return files;
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
   
   public PeassFolders getTempFolder(final String name) throws IOException, InterruptedException {
      final File nowFolder = new File(getTempProjectFolder(), name);
      PeassFolders folders = TemporaryProjectFolderUtil.cloneForcefully(this, nowFolder);
      return folders;
   }

   public VersionControlSystem getVCS() {
      return vcs;
   }

   public String getProjectName() {
      return projectName;
   }

}
