package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.nodeDiffDetector.data.TestCase;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.dependency.KiekerResultManager;
import de.dagere.peass.folders.PeassFolders;

public class KiekerFolderUtil {

   private static final Logger LOG = LogManager.getLogger(KiekerFolderUtil.class);

   public static File[] getClazzMethodFolder(final TestMethodCall testcase, final File resultsFolder) {
      final File testclazzResultFolder = new File(resultsFolder, testcase.getClazz());
      final File[] kiekerTimestampFolders = testclazzResultFolder.listFiles(new FileFilter() {
         @Override
         public boolean accept(final File pathname) {
            return pathname.getName().matches("[0-9]*");
         }
      });
      if (kiekerTimestampFolders == null) {
         new ErrorLogWriter(testcase, resultsFolder).tryToWriteLastLog();
         LOG.debug("Probably project not running - Result folder: " + Arrays.toString(kiekerTimestampFolders) + " ("
               + (kiekerTimestampFolders != null ? kiekerTimestampFolders.length : "null") + ") in " + testclazzResultFolder.getAbsolutePath() + " should exist!");
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

   public static File getKiekerTraceFolder(final File kiekerResultFolder, final TestMethodCall testcase) {
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

   private static File getMethodFolder(final TestMethodCall testcase, final File[] kiekerTimestampFolders) {
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
         File moduleResultsFolder = KiekerResultManager.getJSONFileFolder(folders, rawModuleFolder);
         if (moduleResultsFolder.exists()) {
            return moduleResultsFolder;
         }
      } 
      File projectResultsFolder = KiekerResultManager.getJSONFileFolder(folders, folders.getProjectFolder());
      return projectResultsFolder;
   }
}
