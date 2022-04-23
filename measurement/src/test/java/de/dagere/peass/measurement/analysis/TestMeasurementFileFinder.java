package de.dagere.peass.measurement.analysis;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.datastorage.JSONDataStorer;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.VMResultChunk;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.dataloading.MeasurementFileFinder;

public class TestMeasurementFileFinder {
   
   @Test
   public void testFileFinding() {
      File temp = new File("target/tmp");
      temp.mkdirs();
      
      String shortPackageName = "de.peass.Test";
      String longerPackageName = "de.peass.otherPackage.Test";
      
      MeasurementFileFinder finderBasic = new MeasurementFileFinder(temp, new TestCase(shortPackageName, "testMe"));
      finderBasic.getDataCollector().getChunks().add(new VMResultChunk());
      JSONDataStorer.storeData(finderBasic.getMeasurementFile(), finderBasic.getOneResultData());
      testClazzName(shortPackageName, finderBasic);
      
      MeasurementFileFinder finderSame = new MeasurementFileFinder(temp, new TestCase(shortPackageName, "testMe"));
      JSONDataStorer.storeData(finderSame.getMeasurementFile(), finderSame.getOneResultData());
      testClazzName(shortPackageName, finderSame);
      
      MeasurementFileFinder finderOtherPackage = new MeasurementFileFinder(temp, new TestCase(longerPackageName, "testMe"));
      finderOtherPackage.getDataCollector().getChunks().add(new VMResultChunk());
      JSONDataStorer.storeData(finderOtherPackage.getMeasurementFile(), finderOtherPackage.getOneResultData());
      testClazzName(longerPackageName, finderOtherPackage);
      
      MeasurementFileFinder finderSamePackage = new MeasurementFileFinder(temp, new TestCase(shortPackageName, "testMe"));
      JSONDataStorer.storeData(finderSamePackage.getMeasurementFile(), finderSamePackage.getOneResultData());
      testClazzName(shortPackageName, finderSamePackage);
      
      MeasurementFileFinder finderOtherPackage2 = new MeasurementFileFinder(temp, new TestCase(longerPackageName, "testMe"));
      JSONDataStorer.storeData(finderOtherPackage2.getMeasurementFile(), finderOtherPackage2.getOneResultData());
      testClazzName(longerPackageName, finderOtherPackage2);
   }

   public void testClazzName(String expectedName, MeasurementFileFinder finderOtherPackage) {
      Kopemedata data = JSONDataLoader.loadData(finderOtherPackage.getMeasurementFile()            );
      Assert.assertEquals(data.getClazz(), expectedName);
   }
}
