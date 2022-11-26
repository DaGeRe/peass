package de.dagere.peass.visualization;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.folders.CauseSearchFolders;

public class TestRCAGenerator {

   private static final File RESULT_FOLDER = new File("target/example");

   @BeforeEach
   public void init() throws IOException {
      FileUtils.cleanDirectory(RESULT_FOLDER);
   }

   @Test
   public void testRCAGeneration() throws IOException {
      File folder = new File("src/test/resources/visualization/project_3_peass");
      String commit = "9177678d505bfacb64a95c2271fb03b1e18475a8";

      File expectedResultFile = generate(folder, commit);
      Assert.assertTrue(expectedResultFile.exists());
   }

   @Test
   public void testFailingGeneration() throws IOException {
      File folder = new File("src/test/resources/visualization/project_wrong_peass");
      String commit = "9177678d505bfacb64a95c2271fb03b1e18475a8";

      Assert.assertThrows(FileNotFoundException.class, () -> {
         File expectedResultFile = generate(folder, commit);
      });
   }
   
   @Test
   public void testNumberTooSmallGeneration() throws IOException {
      File folder = new File("src/test/resources/visualization/numberTooSmall_peass");
      String commit = "0e89a15c9e2ebf9078ef03c2dcd556fcfe228970";

      File expectedResultFile = generate(folder, commit);
      Assert.assertFalse(expectedResultFile.exists());
   }

   private File generate(File folder, String commit) throws IOException {
      CauseSearchFolders folders = new CauseSearchFolders(folder);
      File sourceFile = new File(folder, "rca/treeMeasurementResults/" + commit + "/MainTest/details/testMe.json");

      RCAGenerator generator = new RCAGenerator(sourceFile, RESULT_FOLDER, folders);
      generator.createVisualization();

      File expectedResultFile = new File(RESULT_FOLDER, commit + "/de.peass.MainTest/testMe.html");
      return expectedResultFile;
   }
}
