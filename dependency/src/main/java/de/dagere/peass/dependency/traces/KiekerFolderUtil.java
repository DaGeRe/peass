package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.KiekerResultManager;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.folders.PeassFolders;

public class KiekerFolderUtil {

   private static final Logger LOG = LogManager.getLogger(KiekerFolderUtil.class);

   public static File[] getClazzMethodFolder(final TestCase testcase, final File resultsFolder) {
      final File projectResultFolder = new File(resultsFolder, testcase.getClazz());
      final File[] kiekerTimestampFolders = projectResultFolder.listFiles(new FileFilter() {
         @Override
         public boolean accept(final File pathname) {
            return pathname.getName().matches("[0-9]*");
         }
      });
      if (kiekerTimestampFolders == null) {
         new ErrorLogWriter(testcase, resultsFolder).tryToWriteLastLog();
         LOG.debug("Probably project not running - Result folder: " + Arrays.toString(kiekerTimestampFolders) + " ("
               + (kiekerTimestampFolders != null ? kiekerTimestampFolders.length : "null") + ") in " + projectResultFolder.getAbsolutePath() + " should exist!");
         return null;
      }

      File methodResult = getMethodFolder(testcase, kiekerTimestampFolders);

      LOG.debug("Searching in: {}", methodResult);

      if (methodResult.exists() && methodResult.isDirectory()) {
         if (methodResult.listFiles().length > 0) {
            return methodResult.listFiles();
         } else {
            throw new RuntimeException("Folder " + methodResult + " is no Kieker result folder; folder is empty!");
         }
      } else {
         throw new RuntimeException("Folder " + methodResult + " is no Kieker result folder; does not exist or is no directory!");
      }
   }

   public static File getKiekerTraceFolder(final File kiekerResultFolder, final TestCase testcase) {
      File methodResult = new File(kiekerResultFolder, testcase.getMethodWithParams());
      LOG.debug("Searching in: {}", methodResult);
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

   private static File getMethodFolder(final TestCase testcase, final File[] kiekerTimestampFolders) {
      String methodName = testcase.getMethodWithParams();
      File methodResult = new File(kiekerTimestampFolders[0], methodName);
      for (final File test : kiekerTimestampFolders) {
         final File candidate = new File(test, methodName);
         if (candidate.exists()) {
            methodResult = candidate;
         }
      }
      return methodResult;
   }

   public static File getModuleResultFolder(final PeassFolders folders, final TestCase testcase) {
      if (testcase.getModule() != null) {
         File rawModuleFolder = new File(folders.getProjectFolder(), testcase.getModule());
         File moduleResultsFolder = KiekerResultManager.getXMLFileFolder(folders, rawModuleFolder);
         if (moduleResultsFolder.exists()) {
            return moduleResultsFolder;
         }
      } 
      File projectResultsFolder = KiekerResultManager.getXMLFileFolder(folders, folders.getProjectFolder());
      return projectResultsFolder;
   }
}
