package de.dagere.peass.measurement.dependencyprocessors;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

import de.dagere.kopeme.datacollection.TimeDataCollectorNoGC;
import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.datastorage.JSONDataStorer;
import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.kopeme.kopemedata.Fulldata;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.MeasuredValue;
import de.dagere.kopeme.kopemedata.TestMethod;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.OneVMResult;
import de.dagere.peass.measurement.rca.kiekerReading.KiekerDurationReader;

public class DirectKiekerMeasurementTransformer {

   private final PeassFolders folders;

   public DirectKiekerMeasurementTransformer(PeassFolders folders) {
      this.folders = folders;
   }

   public void transform(TestCase test) {
      List<File> tempClazzFolder = folders.findTempClazzFolder(test);
      File tempFile = new File(tempClazzFolder.get(0), test.getMethod() + ".json");
      Kopemedata data = JSONDataLoader.loadData(tempFile);

      OneVMResult oneVMResult = readKiekerData(test);
      
      buildMergedData(data, oneVMResult);
      
      JSONDataStorer.storeData(tempFile, data);
      
   }

   private void buildMergedData(Kopemedata data, OneVMResult oneVMResult) {
      TestMethod testMethod = data.getMethods().get(0);
      DatacollectorResult datacollector = new DatacollectorResult(TimeDataCollectorNoGC.class.getName());
      VMResult vmResult = new VMResult();
      datacollector.getResults().add(vmResult);
      
      vmResult.setIterations(oneVMResult.getCalls());
      vmResult.setRepetitions(1);
      vmResult.setValue(oneVMResult.getAverage());
      Fulldata fulldata = new Fulldata();
      for (StatisticalSummary value : oneVMResult.getValues()) {
         MeasuredValue kopemeValue = new MeasuredValue();
         kopemeValue.setValue((long) value.getMean());
         fulldata.getValues().add(kopemeValue);
      }
      
      vmResult.setFulldata(fulldata);
      
      testMethod.getDatacollectorResults().add(datacollector);
   }

   private OneVMResult readKiekerData(TestCase test) {
      File[] kiekerData = folders.getTempDir().listFiles((FileFilter) new WildcardFileFilter("kieker-*-KoPeMe"));
      if (kiekerData.length != 1) {
         throw new RuntimeException("Expected exactly one Kieker results folder to exist - error occured!");
      }
      
      File kiekerDataFolder = kiekerData[0];
      HashSet<CallTreeNode> fakeNodes = new HashSet<>();
      MeasurementConfig config = new MeasurementConfig(0);
      config.getExecutionConfig().setCommit("dummy");
      config.getExecutionConfig().setCommitOld("neverfound");
      config.getKiekerConfig().setUseAggregation(false);
      
      if (test.getParams() != null) {
         throw new RuntimeException("Params and directlyMeasureKieker are currently not combinable!");
      }
      
      CallTreeNode fakeNode = new CallTreeNode(test.toString(), "public void " + test.getClazz() + "." + test.getMethod() + "()", null, config);
      fakeNode.initVersions();
      fakeNode.setOtherKiekerPattern("public void " + test.getClazz() + "." + test.getMethod() + "()");
      fakeNodes.add(fakeNode);
      
//      fakeNode.get
      
      KiekerDurationReader.executeReducedDurationStage(kiekerDataFolder, fakeNodes, "dummy");
      
      OneVMResult oneVMResult = fakeNode.getResults("dummy").get(0);
      return oneVMResult;
   }
}
