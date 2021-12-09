package de.dagere.peass.measurement.dataloading;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBException;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;

public class MeasurementFileFinder {

   private final File measurementFile;
   private final Kopemedata oneResultData;
   private final Datacollector datacollector;

   public MeasurementFileFinder(final File folder, final String clazz, final String method) throws JAXBException {
      final String shortClazz = clazz.substring(clazz.lastIndexOf('.') + 1);
      final File candidateFull = new File(folder, clazz + "_" + method + ".xml");
      if (!candidateFull.exists()) {
         final File candidateShort = new File(folder, shortClazz + "_" + method + ".xml");
         final Kopemedata oneResultData2 = loadData(candidateShort);
         if (candidateShort.exists()) {
            final String otherFullClazz = oneResultData2.getTestcases().getClazz();
            if (!otherFullClazz.equals(clazz)) {
               measurementFile = candidateFull;
               oneResultData = loadData(measurementFile);
            } else {
               measurementFile = candidateShort;
               oneResultData = oneResultData2;
            }
         } else {
            measurementFile = candidateShort;
            oneResultData = oneResultData2;
         }
      } else {
         measurementFile = candidateFull;
         oneResultData = loadData(measurementFile);
      }
      oneResultData.getTestcases().setClazz(clazz);
      
      final List<TestcaseType> testcaseList = oneResultData.getTestcases().getTestcase();
      datacollector = getDataCollector(method, testcaseList);
   }

   public Kopemedata loadData(final File file) throws JAXBException {
      final XMLDataLoader xdl2 = new XMLDataLoader(file);
      final Kopemedata oneResultData2 = xdl2.getFullData();
      return oneResultData2;
   }
   
   public File getMeasurementFile() {
      return measurementFile;
   }

   public Kopemedata getOneResultData() {
      return oneResultData;
   }
   
   public Datacollector getDataCollector() {
      return datacollector;
   }
   
   private static Datacollector getDataCollector(final String method, final List<TestcaseType> testcaseList) {
      Datacollector datacollector = null;
      for (final TestcaseType testcase : testcaseList) {
         if (testcase.getName().equals(method)) {
            datacollector = testcase.getDatacollector().get(0);
         }
      }
      if (datacollector == null) {
         final TestcaseType testcase = new TestcaseType();
         testcaseList.add(testcase);
         testcase.setName(method);
         datacollector = new Datacollector();
         testcase.getDatacollector().add(datacollector);
      }
      return datacollector;
   }
}
