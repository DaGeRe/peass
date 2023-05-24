package de.dagere.peass.measurement.kieker;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.TestConstants;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.dependencyprocessors.DirectKiekerMeasurementTransformer;

public class TestKiekerDataTransformation {

   @BeforeEach
   public void copyFolder() throws IOException {
      FileUtils.deleteDirectory(TestConstants.CURRENT_PEASS);
      File dataFolder = new File(TestConstants.TEST_RESOURCES, "directlyMeasureKieker/example-data/current_peass");
      FileUtils.copyDirectory(dataFolder, TestConstants.CURRENT_PEASS);
   }

   @Test
   public void testTransformation() {
      PeassFolders folders = new PeassFolders(TestConstants.CURRENT_FOLDER);
      DirectKiekerMeasurementTransformer transformer = new DirectKiekerMeasurementTransformer(folders);
      TestMethodCall test = new TestMethodCall("defaultpackage.TestMe", "testMe");
      transformer.transform(test);

      List<File> tempClazzFolder = folders.findTempClazzFolder(test);
      File tempFile = new File(tempClazzFolder.get(0), test.getMethod() + ".json");

      Kopemedata data = JSONDataLoader.loadData(tempFile);
      List<DatacollectorResult> collectors = data.getMethods().get(0).getDatacollectorResults();
      Assert.assertEquals(1, collectors.size());
      Assert.assertEquals(10, collectors.get(0).getResults().get(0).getFulldata().getValues().size());
      Assert.assertEquals(1656326191802823160L, collectors.get(0).getResults().get(0).getDate());
      
      TestMethodCall testMe2 = new TestMethodCall("defaultpackage.TestMe", "testMe2");

      // Failed tests (that can, in exceptional cases, produce JSONs without methods, should not yield an exception, therefore this is checked here
      transformer.transform(testMe2);
   }

   @Test
   public void testEmptyTransformation() {
      PeassFolders folders = new PeassFolders(TestConstants.CURRENT_FOLDER);
      try {
         FileUtils.cleanDirectory(folders.getTempMeasurementFolder());
      } catch (IOException e) {
         e.printStackTrace();
      }

      DirectKiekerMeasurementTransformer transformer = new DirectKiekerMeasurementTransformer(folders);
      TestMethodCall test = new TestMethodCall("defaultpackage.TestMe", "testMe");

      // Failed tests should not yield an exception, therefore this is checked here
      transformer.transform(test);
   }
   
   @Test
   public void testSemiEmptyEmptyTransformation() throws IOException {
      PeassFolders folders = new PeassFolders(TestConstants.CURRENT_FOLDER);

      for (File kiekerDataFolder : folders.getTempDir().listFiles((FileFilter) new WildcardFileFilter("kieker-*-KoPeMe"))) {
         for (File dataFile : kiekerDataFolder.listFiles()) {
            FileUtils.delete(dataFile);
         }
      }
      
      DirectKiekerMeasurementTransformer transformer = new DirectKiekerMeasurementTransformer(folders);
      TestMethodCall test = new TestMethodCall("defaultpackage.TestMe", "testMe");

      // Failed tests might not produce Kieker results, but produce KoPeMe results; this should also not yield exceptions (but just missing data)
      transformer.transform(test);
   }
}
