package de.dagere.peass.visualization;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.utils.Constants;

public class TestRCAGenerator {

   private static final File VISUALIZATION_FOLDER = new File("src/test/resources/visualization/");
   private static final File RESULT_FOLDER = new File("target/example");

   @BeforeEach
   public void init() throws IOException {
      if (RESULT_FOLDER.exists()) {
         FileUtils.cleanDirectory(RESULT_FOLDER);
      }
   }

   @Test
   public void testRCAGeneration() throws IOException {
      File folder = new File(VISUALIZATION_FOLDER, "rcaSingleTreeTest_peass");
      String commit = "0e8c00cb58fa9873c89ba04e8d447376ca4b90f5";

      File expectedResultFile = generate(folder, commit);
      Assert.assertTrue(expectedResultFile.exists());

      File clazzResultFolder = new File(RESULT_FOLDER, "0e8c00cb58fa9873c89ba04e8d447376ca4b90f5/de.peass.MainTest/");
      File expectedFirstSingleFile = new File(clazzResultFolder, "testMe_0e8c00cb58fa9873c89ba04e8d447376ca4b90f5.html");
      Assert.assertTrue(expectedFirstSingleFile.exists());
      
      File expectedJSSingleFileCurrent = new File(clazzResultFolder, "testMe_0e8c00cb58fa9873c89ba04e8d447376ca4b90f5.js");
      Assert.assertTrue(expectedJSSingleFileCurrent.exists());
      
      File expectedJSSingleFileOld = new File(clazzResultFolder, "testMe_32759dad8f3be04835d1e833ede95662f4a412e1.js");
      Assert.assertTrue(expectedJSSingleFileOld.exists());
      
      String textCurrent = FileUtils.readFileToString(expectedJSSingleFileCurrent, Charset.defaultCharset());
      MatcherAssert.assertThat(textCurrent, Matchers.not( StringContains.containsString("de.dagere.using.AnotherChangeable#method0")));
      MatcherAssert.assertThat(textCurrent, Matchers.not(StringContains.containsString("XYZ123-Test")));
      
      String textOld = FileUtils.readFileToString(expectedJSSingleFileOld, Charset.defaultCharset());
      MatcherAssert.assertThat(textOld, StringContains.containsString("de.dagere.using.AnotherChangeable#method0"));
      MatcherAssert.assertThat(textOld, StringContains.containsString("XYZ123-Test"));
   }

   @Test
   public void testSingleTreeGeneration() throws IOException {
      File folder = new File(VISUALIZATION_FOLDER, "project_3_peass");
      String commit = "9177678d505bfacb64a95c2271fb03b1e18475a8";

      File expectedResultFile = generate(folder, commit);
      Assert.assertTrue(expectedResultFile.exists());

   }

   @Test
   public void testFailingGeneration() throws IOException {
      File folder = new File(VISUALIZATION_FOLDER, "project_wrong_peass");
      String commit = "9177678d505bfacb64a95c2271fb03b1e18475a8";

      Assert.assertThrows(FileNotFoundException.class, () -> {
         File expectedResultFile = generate(folder, commit);
      });
   }

   @Test
   public void testNumberTooSmallGeneration() throws IOException {
      File folder = new File(VISUALIZATION_FOLDER, "numberTooSmall_peass");
      String commit = "0e89a15c9e2ebf9078ef03c2dcd556fcfe228970";

      File expectedResultFile = generate(folder, commit);
      Assert.assertFalse(expectedResultFile.exists());
   }

   private File generate(File folder, String commit) throws IOException {
      CauseSearchFolders folders = new CauseSearchFolders(folder);
      File sourceFile = new File(folder, "rca/treeMeasurementResults/" + commit + "/MainTest/details/testMe.json");

      RCAGenerator generator = new RCAGenerator(sourceFile, RESULT_FOLDER, folders);
      generator.createVisualization();

      generator.setPropertyFolder(new File(VISUALIZATION_FOLDER, "properties_rcaSingleTreeTest"));

      createTreeStructureView(commit, folders, generator);

      File expectedResultFile = new File(RESULT_FOLDER, commit + "/de.peass.MainTest/testMe.html");
      return expectedResultFile;
   }

   private void createTreeStructureView(String commit, CauseSearchFolders folders, RCAGenerator generator) throws IOException, StreamReadException, DatabindException {
      File treeFolder = folders.getExistingTreeCacheFolder(commit, new TestMethodCall("de.peass.MainTest", "testMe"));
      if (treeFolder.exists() && commit.equals("0e8c00cb58fa9873c89ba04e8d447376ca4b90f5")) {
         final File potentialCacheFileOld = new File(treeFolder, "32759dad8f3be04835d1e833ede95662f4a412e1");
         final File potentialCacheFile = new File(treeFolder, "0e8c00cb58fa9873c89ba04e8d447376ca4b90f5");

         final CallTreeNode rootPredecessor = Constants.OBJECTMAPPER.readValue(potentialCacheFileOld, CallTreeNode.class);
         final CallTreeNode rootCurrent = Constants.OBJECTMAPPER.readValue(potentialCacheFile, CallTreeNode.class);

         generator.createSingleVisualization(commit, rootCurrent);
         generator.createSingleVisualization("32759dad8f3be04835d1e833ede95662f4a412e1", rootPredecessor);
      }
   }
}
