package de.peass.dependency.traces;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.KiekerResultManager;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependencyprocessors.ViewNotFoundException;

public class KiekerFolderUtil {

   private static final Logger LOG = LogManager.getLogger(KiekerFolderUtil.class);

   public static File getClazzMethodFolder(final TestCase testcase, final File resultsFolder) throws ViewNotFoundException {
      final File projectResultFolder = new File(resultsFolder, testcase.getClazz());
      final File[] listFiles = projectResultFolder.listFiles(new FileFilter() {
         @Override
         public boolean accept(final File pathname) {
            return pathname.getName().matches("[0-9]*");
         }
      });
      if (listFiles == null) {
         tryToWriteLastLog(testcase, resultsFolder);
         throw new ViewNotFoundException("Probably project not running - Result folder: " + Arrays.toString(listFiles) + " ("
               + (listFiles != null ? listFiles.length : "null") + ") in " + projectResultFolder.getAbsolutePath() + " should exist!");
      }

      File methodResult = getMethodFolder(testcase, listFiles);

      LOG.debug("Searching for: {}", methodResult);

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

   private static void tryToWriteLastLog(final TestCase testcase, final File resultsFolder) {
      File logFolder = new File(resultsFolder, "../../logs/");
      System.out.println("Searching in" + logFolder.getAbsolutePath());
      if (logFolder.exists()) {
         File logFolderChild = logFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(final File pathname) {
               return pathname.isDirectory();
            }
         })[0];
         System.out.println("First folder: " + logFolderChild.getAbsolutePath());
         File txtFile = new File(logFolderChild, "log_" + testcase.getClazz() + "/" + testcase.getMethod());
         System.out.println("Trying " + txtFile.getAbsolutePath());
         if (txtFile.exists()) {
            try {
               System.out.println(FileUtils.readFileToString(txtFile, StandardCharsets.UTF_8));
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      } else {
         System.out.println("Folder did not exist");
         File potentialPeassFolder = new File(resultsFolder, "../../");
         System.out.println("Files in " + potentialPeassFolder.getAbsolutePath());
         for (File file : potentialPeassFolder.listFiles()) {
            System.out.println(file.getAbsolutePath());
         }
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

   public static File findKiekerFolder(final String testMethodName, final File parent) {
      final File[] listFiles = parent.listFiles(new FileFilter() {
         @Override
         public boolean accept(final File pathname) {
            return pathname.getName().matches("[0-9]*");
         }
      });
      LOG.debug("Kieker-Files: {}", listFiles.length);
      if (listFiles.length == 0) {
         LOG.info("No result folder existing - probably a package name change?");
         LOG.info("Files: {}", Arrays.toString(parent.list()));
         return null;
      }
      for (final File kiekerFolder : listFiles) {
         LOG.debug("Analysing Folder: {} {}", kiekerFolder.getAbsolutePath(), testMethodName);
         final File kiekerNextFolder = new File(kiekerFolder, testMethodName);
         if (kiekerNextFolder.exists() && kiekerNextFolder.listFiles().length > 0) {
            final File kiekerResultFolder = kiekerNextFolder.listFiles()[0];
            LOG.debug("Test: " + testMethodName);
            return kiekerResultFolder;
         }
      }
      return null;
   }

   public static File getModuleResultFolder(final PeASSFolders folders, final TestCase testcase)
         throws FileNotFoundException, IOException, XmlPullParserException {
      File moduleFolder;
      if (testcase.getModule() != null) {
         moduleFolder = KiekerResultManager.getXMLFileFolder(folders, new File(folders.getProjectFolder(), testcase.getModule()));
      } else {
         moduleFolder = KiekerResultManager.getXMLFileFolder(folders, folders.getProjectFolder());
      }
      return moduleFolder;
   }
}
