package de.dagere.peass.measurement.analysis;

import java.io.File;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Test;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.dagere.peass.measurement.dataloading.MeasurementFileFinder;

public class TestMearementFileFinder {
   
   @Test
   public void testFileFinding() throws JAXBException {
      File temp = new File("target/tmp");
      temp.mkdirs();
      
      String shortPackageName = "de.peass.Test";
      String longerPackageName = "de.peass.otherPackage.Test";
      
      MeasurementFileFinder finderBasic = new MeasurementFileFinder(temp, shortPackageName, "testMe");
      finderBasic.getDataCollector().getChunk().add(new Chunk());
      XMLDataStorer.storeData(finderBasic.getMeasurementFile(), finderBasic.getOneResultData());
      testClazzName(shortPackageName, finderBasic);
      
      MeasurementFileFinder finderSame = new MeasurementFileFinder(temp, shortPackageName, "testMe");
      XMLDataStorer.storeData(finderSame.getMeasurementFile(), finderSame.getOneResultData());
      testClazzName(shortPackageName, finderSame);
      
      MeasurementFileFinder finderOtherPackage = new MeasurementFileFinder(temp, longerPackageName, "testMe");
      finderOtherPackage.getDataCollector().getChunk().add(new Chunk());
      XMLDataStorer.storeData(finderOtherPackage.getMeasurementFile(), finderOtherPackage.getOneResultData());
      testClazzName(longerPackageName, finderOtherPackage);
      
      MeasurementFileFinder finderSamePackage = new MeasurementFileFinder(temp, shortPackageName, "testMe");
      XMLDataStorer.storeData(finderSamePackage.getMeasurementFile(), finderSamePackage.getOneResultData());
      testClazzName(shortPackageName, finderSamePackage);
      
      MeasurementFileFinder finderOtherPackage2 = new MeasurementFileFinder(temp, longerPackageName, "testMe");
      XMLDataStorer.storeData(finderOtherPackage2.getMeasurementFile(), finderOtherPackage2.getOneResultData());
      testClazzName(longerPackageName, finderOtherPackage2);
   }

   public void testClazzName(String expectedName, MeasurementFileFinder finderOtherPackage) throws JAXBException {
      XMLDataLoader loader = new XMLDataLoader(finderOtherPackage.getMeasurementFile());
      Assert.assertEquals(loader.getFullData().getTestcases().getClazz(), expectedName);
   }
}
