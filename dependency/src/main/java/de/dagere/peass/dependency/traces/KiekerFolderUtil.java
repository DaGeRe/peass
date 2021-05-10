package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.KiekerResultManager;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class KiekerFolderUtil {

   private static final Logger LOG = LogManager.getLogger(KiekerFolderUtil.class);

   public static File[] getClazzMethodFolder(final TestCase testcase, final File resultsFolder) {
      final File projectResultFolder = new File(resultsFolder, testcase.getClazz());
      final File[] listFiles = projectResultFolder.listFiles(new FileFilter() {
         @Override
         public boolean accept(final File pathname) {
            return pathname.getName().matches("[0-9]*");
         }
      });
      if (listFiles == null) {
         new ErrorLogWriter(testcase, resultsFolder).tryToWriteLastLog();
         throw new RuntimeException("Probably project not running - Result folder: " + Arrays.toString(listFiles) + " ("
               + (listFiles != null ? listFiles.length : "null") + ") in " + projectResultFolder.getAbsolutePath() + " should exist!");
      }

      File methodResult = getMethodFolder(testcase, listFiles);

      LOG.debug("Searching for: {}", methodResult);

      if (methodResult.exists() && methodResult.isDirectory()) {
         if (methodResult.listFiles().length > 0) {
            return methodResult.listFiles();
         } else {
            throw new RuntimeException("Folder " + methodResult + " is no Kieker result folder!");
         }
      } else {
         throw new RuntimeException("Folder " + methodResult + " is no Kieker result folder!");
      }
   }

   public static File getKiekerTraceFolder(final File kiekerResultFolder, final TestCase testcase) {
      File methodResult = new File(kiekerResultFolder, testcase.getMethod());
      if (methodResult.exists() && methodResult.isDirectory()) {
         if (methodResult.listFiles().length > 0) {
            return methodResult.listFiles()[0];
         } else {
            throw new RuntimeException("Folder " + methodResult + " is no Kieker result folder!");
         }
      } else {
         throw new RuntimeException("Folder " + methodResult + " is no Kieker result folder!");
      }
   }

   private static File getMethodFolder(final TestCase testcase, final File[] listFiles) {
      File methodResult = new File(listFiles[0], testcase.getMethod());
      for (final File test : listFiles) {
         final File candidate = new File(test, testcase.getMethod());
         if (candidate.exists()) {
            methodResult = candidate;
         }
      }
      return methodResult;
   }

   public static File getModuleResultFolder(final PeASSFolders folders, final TestCase testcase) {
      File moduleFolder;
      if (testcase.getModule() != null) {
         File rawModuleFolder = new File(folders.getProjectFolder(), testcase.getModule());
         moduleFolder = KiekerResultManager.getXMLFileFolder(folders, rawModuleFolder);
      } else {
         moduleFolder = KiekerResultManager.getXMLFileFolder(folders, folders.getProjectFolder());
      }
      return moduleFolder;
   }
}
