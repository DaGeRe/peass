package de.dagere.peass.measurement.dataloading;

import java.io.File;
import java.util.List;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.TestMethod;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class MeasurementFileFinder {

   private final File measurementFile;
   private final Kopemedata oneResultData;
   private final DatacollectorResult datacollector;

   public MeasurementFileFinder(final File folder, TestCase testcase) {
      String clazz = testcase.getClazz();
      String methodWithParams = testcase.getMethodWithParams();
      final String shortClazz = clazz.substring(clazz.lastIndexOf('.') + 1);
      File xmlCandidateFull = new File(folder, clazz + "_" + methodWithParams + ".xml");
      final File xmlCandidateShort = new File(folder, shortClazz + "_" + methodWithParams + ".xml");
      if (xmlCandidateFull.exists()) {
         measurementFile = xmlCandidateFull;
         oneResultData = loadData(measurementFile);
      } else if (xmlCandidateShort.exists()) {
         final Kopemedata oneResultData2 = loadData(xmlCandidateShort);
         final String otherFullClazz = oneResultData2.getClazz();
         if (!otherFullClazz.equals(clazz)) {
            measurementFile = xmlCandidateFull;
            oneResultData = loadData(measurementFile);
         } else {
            measurementFile = xmlCandidateShort;
            oneResultData = oneResultData2;
         }
      } else {
         final File jsonCandidateFull = new File(folder, clazz + "_" + methodWithParams + ".json");
         if (!jsonCandidateFull.exists()) {
            final File jsonCandidateShort = new File(folder, shortClazz + "_" + methodWithParams + ".json");
            if (jsonCandidateShort.exists()) {
               final Kopemedata oneResultData2 = loadData(jsonCandidateShort);
               final String otherFullClazz = oneResultData2.getClazz();
               if (!otherFullClazz.equals(clazz)) {
                  measurementFile = jsonCandidateShort;
                  oneResultData = loadData(measurementFile);
               } else {
                  measurementFile = jsonCandidateShort;
                  oneResultData = oneResultData2;
               }
            } else {
               measurementFile = jsonCandidateShort;
               oneResultData = new Kopemedata(clazz);
            }
         } else {
            measurementFile = jsonCandidateFull;
            oneResultData = loadData(measurementFile);
         }
      }
      
      oneResultData.setClazz(clazz);

      final List<TestMethod> testcaseList = oneResultData.getMethods();
      datacollector = getDataCollector(testcase.getMethod(), testcaseList);
   }

   public Kopemedata loadData(final File file) {
      final Kopemedata oneResultData2 = JSONDataLoader.loadData(file);
      return oneResultData2;
   }

   public File getMeasurementFile() {
      return measurementFile;
   }

   public Kopemedata getOneResultData() {
      return oneResultData;
   }

   public DatacollectorResult getDataCollector() {
      return datacollector;
   }

   public static DatacollectorResult getDataCollector(final String method, final List<TestMethod> testcaseList) {
      DatacollectorResult datacollector = null;
      for (final TestMethod testcase : testcaseList) {
         if (testcase.getMethod().equals(method)) {
            datacollector = testcase.getDatacollectorResults().get(0);
         }
      }
      if (datacollector == null) {
         final TestMethod testcase = new TestMethod(method);
         testcaseList.add(testcase);
         datacollector = new DatacollectorResult("");
         testcase.getDatacollectorResults().add(datacollector);
      }
      return datacollector;
   }
}
