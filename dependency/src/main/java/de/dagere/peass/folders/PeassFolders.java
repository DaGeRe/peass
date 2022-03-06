package de.dagere.peass.folders;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.traces.TemporaryProjectFolderUtil;
import de.dagere.peass.vcs.VersionControlSystem;

/**
 * Helps managing the PeASS-folders and their existance
 * 
 * @author reichelt
 *
 */
public class PeassFolders {
   
   private static final Logger LOG = LogManager.getLogger(PeassFolders.class);
   
   public static final String MEASUREMENTS = "measurements";

   public static final String PEASS_POSTFIX = "_peass";

   protected final File projectFolder;
   // private final File resultFolder;
   protected final File fullResultFolder;
   
   private final File tempResultFolder, tempProjectFolder, tempFolder, kiekerTemp;
   private final VMExecutionLogFolders logFolders;
   private final File oldSourceFolder;
   private final File measurementsFolder;
   private final File cleanFolder;
   protected final File cleanNativeFolder;
   private final File debugFolder;
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

      logFolders = new VMExecutionLogFolders(peassFolder);

      oldSourceFolder = new File(peassFolder, "lastSources");
      fullResultFolder = new File(peassFolder, "measurementsFull");
      fullResultFolder.mkdir();
      tempFolder = new File(peassFolder, "temp");
      tempFolder.mkdir();
      cleanFolder = new File(peassFolder, "clean");
      cleanNativeFolder = new File(cleanFolder, "native");
      debugFolder = new File(peassFolder, "debug");
      // cleanFolder.mkdir();
      measurementsFolder = new File(fullResultFolder, MEASUREMENTS);
      measurementsFolder.mkdir();
      tempResultFolder = new File(peassFolder, "measurementsTemp");
      tempResultFolder.mkdir();
      kiekerTemp = new File(peassFolder, "kiekerTemp");
      tempProjectFolder = new File(peassFolder, "projectTemp");
   }

   public PeassFolders(final File folder) {
      this(folder, (folder != null ? folder.getName() : null));
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
   
   /**
    * If cleaning is done with an existing project, a new folder in the clean folder is created; if the cleaning is done directly by the measurement process, the native folder is used
    * @return
    */
   public File getNativeCleanFolder() {
      return cleanNativeFolder;
   }
   
   public File getDependencyLogFolder() {
      return logFolders.getDependencyLogFolder();
   }

   public File getDependencyLogSuccessRunFile(final String version) {
      final File versionFolder = new File(getDependencyLogFolder(), version);
      if (!versionFolder.exists()) {
         versionFolder.mkdir();
      }
      return new File(versionFolder, "testRunning.log");
   }

   public File getMeasureLogFolder() {
      return logFolders.getMeasureLogFolder();
   }

   public File getTreeLogFolder() {
      return logFolders.getTreeLogFolder();
   }

   public File getRCALogFolder() {
      return logFolders.getRCALogFolder();
   }

   public File getExistingMeasureLogFolder(final String version, final TestCase testcase) {
      File testLogFolder = new File(getMeasureLogFolder(), version + File.separator + testcase.getMethod());
      if (testLogFolder.exists()) {
         return testLogFolder;
      } else {
         testLogFolder = new File(getMeasureLogFolder(), version + File.separator + testcase.getMethod() + "_new");
         if (testLogFolder.exists()) {
            return testLogFolder;
         } else {
            return null;
         }
      }
   }

   public File getMeasureLogFolder(final String version, final TestCase testcase) {
      File testLogFolder = new File(getMeasureLogFolder(), version + File.separator + testcase.getMethod());
      if (testLogFolder.exists()) {
         testLogFolder = new File(getMeasureLogFolder(), version + File.separator + testcase.getMethod() + "_new");
      }
      testLogFolder.mkdirs();
      return testLogFolder;
   }

   public File getExistingRCALogFolder(final String version, final TestCase testcase, final int level) {
      File testLogFolder = new File(getRCALogFolder(), version + File.separator + testcase.getMethod() + File.separator + level);
      if (!testLogFolder.exists()) {
         testLogFolder = new File(getRCALogFolder(), version + File.separator + testcase.getMethod() + File.separator + level + "_new");
      }
      return testLogFolder;
   }

   public File getRCALogFolder(final String version, final TestCase testcase, final int level) {
      File testLogFolder = new File(getRCALogFolder(), version + File.separator + testcase.getMethod() + File.separator + level);
      if (testLogFolder.exists()) {
         testLogFolder = new File(getRCALogFolder(), version + File.separator + testcase.getMethod() + File.separator + level + "_new");
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

   public File getProgressFile() {
      return new File(fullResultFolder, "progress.txt");
   }

   public File getTempMeasurementFolder() {
      return tempResultFolder;
   }

   /**
    * Searches in subfolders for a clazz folder (necessary, since submodules may have arbitraty depth)
    * 
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

   public File getSummaryFile(final TestCase testcase) {
      final String shortClazzName = testcase.getShortClazz();
      final File fullResultFile;
      if (testcase.getParams() != null) {
         fullResultFile = new File(fullResultFolder, shortClazzName + "_" + testcase.getMethod() + "(" + testcase.getParams() + ").xml");
      } else {
         fullResultFile = new File(fullResultFolder, shortClazzName + "_" + testcase.getMethod() + ".xml");
      }
      return fullResultFile;
   }

   public File getFullResultFolder(final TestCase testcase, final String mainVersion, final String version) {
      final File destFolder = new File(getDetailResultFolder(), testcase.getClazz());
      LOG.debug("Creating: " + destFolder + " " + mainVersion + " " + testcase.getClazz());
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

   public File getResultFile(final TestCase testcase, final int vmid, final String version, final String mainVersion) {
      final File compareVersionFolder = getFullResultFolder(testcase, mainVersion, version);
      String xmlFileName = getXMLFileName(testcase, version, vmid);
      final File destFile = new File(compareVersionFolder, xmlFileName);
      return destFile;
   }

   public static String getRelativeFullResultPath(final TestCase testcase, final String mainVersion, final String version, final int vmid) {
      String filename = getXMLFileName(testcase, version, vmid);
      String start = testcase.getClazz() + File.separator + mainVersion + File.separator + version + File.separator + filename;
      return start;
   }

   private static String getXMLFileName(final TestCase testcase, final String version, final int vmid) {
      String filename;
      if (testcase.getParams() != null) {
         filename = testcase.getMethod() + "(" + testcase.getParams() + ")_" + vmid + "_" + version + ".xml";
      } else {
         filename = testcase.getMethod() + "_" + vmid + "_" + version + ".xml";
      }
      return filename;
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
      PeassFolders folders = TemporaryProjectFolderUtil.cloneForcefully(this, nowFolder, logFolders);
      return folders;
   }

   public VersionControlSystem getVCS() {
      return vcs;
   }

   public String getProjectName() {
      return projectName;
   }

}
