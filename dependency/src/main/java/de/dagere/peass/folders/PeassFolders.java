package de.dagere.peass.folders;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.nodeDiffDetector.config.SourceCodeFolders;
import de.dagere.nodeDiffDetector.data.TestCase;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.dependency.traces.TemporaryProjectFolderUtil;
import de.dagere.peass.vcs.VersionControlSystem;

/**
 * Helps managing the PeASS-folders and their existance
 * 
 * @author reichelt
 *
 */
public class PeassFolders implements SourceCodeFolders {

   public static final String CLEAN_FOLDER_NAME = "clean";

   private static final Logger LOG = LogManager.getLogger(PeassFolders.class);

   public static final String MEASUREMENTS = "measurements";

   public static final String PEASS_POSTFIX = "_peass";
   public static final String PEASS_FULL_POSTFIX = "_fullPeass";

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

   public PeassFolders(final File folder, final String projectName) {
      this.projectName = projectName;
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
      cleanFolder = new File(peassFolder, CLEAN_FOLDER_NAME);
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
    * If cleaning is done with an existing project, a new folder in the clean folder is created; if the cleaning is done directly by the measurement process, the native folder is
    * used
    * 
    * @return
    */
   public File getNativeCleanFolder() {
      return cleanNativeFolder;
   }

   public File getDependencyLogFolder() {
      return logFolders.getDependencyLogFolder();
   }
   
   public File getTwiceRunningLogFolder() {
      return logFolders.getTwiceRunningLogFolder();
   }

   public File getDependencyLogSuccessRunFile(final String commit) {
      final File commitFolder = new File(getDependencyLogFolder(), commit);
      if (!commitFolder.exists()) {
         commitFolder.mkdir();
      }
      return new File(commitFolder, "testRunning.log");
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

   public File getExistingMeasureLogFolder(final String commit, final TestMethodCall testcase) {
      File testLogFolder = new File(getMeasureLogFolder(), commit + File.separator + testcase.getClazz() + File.separator + testcase.getMethod());
      if (testLogFolder.exists()) {
         return testLogFolder;
      } else {
         return null;
      }
   }

   public File getMeasureLogFolder(final String commit, final TestMethodCall testcase) {
      File testLogFolder = new File(getMeasureLogFolder(), commit + File.separator + testcase.getClazz() + File.separator + testcase.getMethod());
      testLogFolder.mkdirs();
      return testLogFolder;
   }

   public File getExistingRCALogFolder(final String commit, final TestMethodCall testcase, final int level) {
      File testLogFolder = new File(getRCALogFolder(), commit + File.separator + testcase.getClazz() + File.separator + testcase.getMethod() + File.separator + level);
      return testLogFolder;
   }

   public File getRCALogFolder(final String commit, final TestMethodCall testcase, final int level) {
      File testLogFolder = new File(getRCALogFolder(), commit + File.separator + testcase.getClazz() + File.separator + testcase.getMethod() + File.separator + level);
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
      System.out.println("Testcase: " + testcase + " " + expectedFolderName);
      FileFilter folderFilter = new WildcardFileFilter(expectedFolderName);
      return findTempClazzFolder(tempResultFolder, folderFilter);
   }

   private List<File> findTempClazzFolder(final File baseFolder, final FileFilter folderFilter) {
      final List<File> files = new LinkedList<>();
      LOG.trace("Searching in {} {}", baseFolder, baseFolder.exists());
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

   public File getSummaryFile(final TestMethodCall testcase) {
      final String shortClazzName = testcase.getShortClazz();
      final File fullResultFile;
      if (testcase.getParams() != null) {
         File fullResultFileXML = new File(fullResultFolder, shortClazzName + "_" + testcase.getMethod() + "(" + testcase.getParams() + ").xml");
         if (fullResultFileXML.exists()) {
            fullResultFile = fullResultFileXML;
         } else {
            fullResultFile = new File(fullResultFolder, shortClazzName + "_" + testcase.getMethod() + "(" + testcase.getParams() + ").json");
         }
      } else {
         File fullResultFileXML = new File(fullResultFolder, shortClazzName + "_" + testcase.getMethod() + ".xml");
         if (fullResultFileXML.exists()) {
            fullResultFile = fullResultFileXML;
         } else {
            fullResultFile = new File(fullResultFolder, shortClazzName + "_" + testcase.getMethod() + ".json");
         }
      }
      return fullResultFile;
   }

   public File getFullResultFolder(final TestCase testcase, final String mainCommit, final String commit) {
      final File destFolder = new File(getDetailResultFolder(), testcase.getClazz());
      LOG.trace("Creating: {} Commit: {} Class: {} ", destFolder, mainCommit, testcase.getClazz());
      final File currentCommitFolder = new File(destFolder, mainCommit);
      if (!currentCommitFolder.exists()) {
         currentCommitFolder.mkdir();
      }
      final File compareCommitFolder = new File(currentCommitFolder, commit);
      if (!compareCommitFolder.exists()) {
         compareCommitFolder.mkdir();
      }
      return compareCommitFolder;
   }

   public File getResultFile(final TestMethodCall testcase, final int vmid, final String commit, final String mainCommit) {
      final File compareCommitFolder = getFullResultFolder(testcase, mainCommit, commit);
      String xmlFileName = getXMLFileName(testcase, commit, vmid);
      final File destFile = new File(compareCommitFolder, xmlFileName);
      return destFile;
   }

   public static String getRelativeFullResultPath(final TestMethodCall testcase, final String mainCommit, final String commit, final int vmid) {
      String filename = getXMLFileName(testcase, commit, vmid);
      String start = testcase.getClazz() + File.separator + mainCommit + File.separator + commit + File.separator + filename;
      return start;
   }

   private static String getXMLFileName(final TestMethodCall testcase, final String commit, final int vmid) {
      String filename;
      if (testcase.getParams() != null) {
         filename = testcase.getMethod() + "(" + testcase.getParams() + ")_" + vmid + "_" + commit + ".json";
      } else {
         filename = testcase.getMethod() + "_" + vmid + "_" + commit + ".json";
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
      if (!tempFolder.exists()) {
         tempFolder.mkdir();
      }
      return tempFolder;
   }

   public File getDebugFolder() {
      if (!debugFolder.exists()) {
         debugFolder.mkdir();
      }
      return debugFolder;
   }

   public PeassFolders getTempFolder(final String name, final String gitCryptKey) {
      final File nowFolder = new File(getTempProjectFolder(), name);
      PeassFolders folders = TemporaryProjectFolderUtil.cloneForcefully(this, nowFolder, logFolders, gitCryptKey);
      return folders;
   }

   public VersionControlSystem getVCS() {
      return vcs;
   }

   public String getProjectName() {
      return projectName;
   }

   public File getReductionFile(TestMethodCall testcase) {
      File reductionFolder = new File(peassFolder, "reductions");
      if (!reductionFolder.exists()) {
         reductionFolder.mkdirs();
      }
      File clazzFolder = new File(reductionFolder, testcase.getClazz());
      if (!clazzFolder.exists()) {
         clazzFolder.mkdirs();
      }
      return new File(clazzFolder, testcase.getMethodWithParams() + ".json");
   }

   public VMExecutionLogFolders getLogFolders() {
      return logFolders;
   }

}
