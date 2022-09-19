package de.dagere.peass.measurement.dependencyprocessors;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datacollection.TimeDataCollectorNoGC;
import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.datastorage.JSONDataStorer;
import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.kopeme.kopemedata.Fulldata;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.MeasuredValue;
import de.dagere.kopeme.kopemedata.TestMethod;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.rca.kiekerReading.KiekerDurationReader;

public class DirectKiekerMeasurementTransformer {

   private static final Logger LOG = LogManager.getLogger(DirectKiekerMeasurementTransformer.class);

   private final PeassFolders folders;

   public DirectKiekerMeasurementTransformer(PeassFolders folders) {
      this.folders = folders;
   }

   public void transform(TestMethodCall test) {
      List<File> tempClazzFolder = folders.findTempClazzFolder(test);
      if (tempClazzFolder.size() > 0) {
         File tempFile = new File(tempClazzFolder.get(0), test.getMethod() + ".json");
         Kopemedata data = JSONDataLoader.loadData(tempFile);

         List<MeasuredValue> oneVMResult = readKiekerData(test);

         buildMergedData(data, oneVMResult);

         JSONDataStorer.storeData(tempFile, data);
      } else {
         LOG.error("Result transformation impossible - no data written");
      }

   }

   private void buildMergedData(Kopemedata data, List<MeasuredValue> oneVMResult) {
      TestMethod testMethod = data.getMethods().get(0);
      DatacollectorResult datacollector = new DatacollectorResult(TimeDataCollectorNoGC.class.getName());
      VMResult vmResult = new VMResult();
      datacollector.getResults().add(vmResult);

      vmResult.setIterations(oneVMResult.size());
      vmResult.setRepetitions(1);
      Fulldata fulldata = new Fulldata();
      SummaryStatistics statistics = new SummaryStatistics();
      for (MeasuredValue value : oneVMResult) {
         fulldata.getValues().add(value);

         statistics.addValue(value.getValue());
      }

      vmResult.setValue(statistics.getMean());
      vmResult.setDeviation(statistics.getStandardDeviation());
      vmResult.setDate(oneVMResult.get(0).getStartTime());

      vmResult.setFulldata(fulldata);

      testMethod.getDatacollectorResults().add(datacollector);
   }

   private List<MeasuredValue> readKiekerData(TestMethodCall test) {
      File[] kiekerData = folders.getTempDir().listFiles((FileFilter) new WildcardFileFilter("kieker-*-KoPeMe"));
      if (kiekerData.length != 1) {
         throw new RuntimeException("Expected exactly one Kieker results folder to exist - error occured!");
      }

      File kiekerDataFolder = kiekerData[0];

      if (test.getParams() != null) {
         throw new RuntimeException("Params and directlyMeasureKieker are currently not combinable!");
      }

      List<MeasuredValue> values = KiekerDurationReader.executeReducedDurationStage(kiekerDataFolder);
      return values;
   }
}
