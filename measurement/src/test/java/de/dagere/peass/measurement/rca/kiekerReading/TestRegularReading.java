package de.dagere.peass.measurement.rca.kiekerReading;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;

public class TestRegularReading {

   private static final File RESOURCE_FOLDER = new File("src/test/resources/kiekerReading");

   private Set<CallTreeNode> callTreeNodes;
   
   @BeforeEach
   public void init() {
      callTreeNodes = buildCallTreeNodes();
   }
   
   @Test
   public void testOperationExecutionRecordReading() {
      File durationRecordFolder = new File(RESOURCE_FOLDER, "operationExecutionRecord");

      KiekerDurationReader.executeDurationStage(durationRecordFolder, callTreeNodes, "1");
      
      createAllStatistics();
      checkAllResults();
   }

   @Test
   public void testDurationRecordReading() {
      File durationRecordFolder = new File(RESOURCE_FOLDER, "durationRecord");

      KiekerDurationReader.executeReducedDurationStage(durationRecordFolder, callTreeNodes, "1");
      
      createAllStatistics();
      checkAllResults();
   }
   
   @Test
   public void testDurationRecordSourceInstrumentationReading() {
      File durationRecordFolder = new File(RESOURCE_FOLDER, "durationRecordSourceInstrumentation");

      KiekerDurationReader.executeReducedDurationStage(durationRecordFolder, callTreeNodes, "1");
      
      createAllStatistics();
      checkAllResults();
   }

   private void checkAllResults() {
      for (CallTreeNode node : callTreeNodes) {
         Assert.assertEquals("Expecting one VM run in " + node.getKiekerPattern(), 1, node.getStatistics("1").getN());
      }
   }

   private void createAllStatistics() {
      for (CallTreeNode node : callTreeNodes) {
         node.createStatistics("1");
      }
   }

   private Set<CallTreeNode> buildCallTreeNodes() {
      Set<CallTreeNode> callTreeNodes = new HashSet<>();

      for (String kiekerPattern : new String[] { "new de.dagere.peass.C0_0.<init>()",
            "new de.dagere.peass.C1_0.<init>()",
            "public new de.dagere.peass.AddRandomNumbers.<init>()",
            "public int de.dagere.peass.AddRandomNumbers.doSomething(int)",
            "public int de.dagere.peass.C1_0.method0()",
            "public int de.dagere.peass.C0_0.method0()",
            "private int de.dagere.peass.C0_0.method0(int)",
            "public void de.dagere.peass.MainTest.testMe()" }) {
         int startIndex = kiekerPattern.lastIndexOf(" ") != -1 ? kiekerPattern.lastIndexOf(" ") : 0;
         String call = kiekerPattern.substring(startIndex, kiekerPattern.lastIndexOf("("));
         CallTreeNode addedNode = new CallTreeNode(call, kiekerPattern, kiekerPattern, new MeasurementConfig(1, "1", "0"));
         callTreeNodes.add(addedNode);
         addedNode.initVersions();
      }
      return callTreeNodes;
   }
}
