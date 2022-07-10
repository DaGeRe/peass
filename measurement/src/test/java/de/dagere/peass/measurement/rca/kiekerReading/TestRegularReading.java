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

      MeasurementConfig config = new MeasurementConfig(1, "1", "0");
      CallTreeNode root =  new CallTreeNode("de.dagere.peass.MainTest.testMe()", "public void de.dagere.peass.MainTest.testMe()", "public void de.dagere.peass.MainTest.testMe()", config);
      callTreeNodes.add(root);
      root.initCommitData();
      
      buildFirstLevel(callTreeNodes, root);
      CallTreeNode c0_method0Node = root.getChildren().get(1);
      CallTreeNode child3 = buildSecondLevel(callTreeNodes, c0_method0Node);
      buildLastLevel(callTreeNodes, child3);
      
      return callTreeNodes;
   }

   private void buildLastLevel(Set<CallTreeNode> callTreeNodes, CallTreeNode child3) {
      for (String kiekerPattern : new String[] {
            "public new de.dagere.peass.AddRandomNumbers.<init>()",
            "public int de.dagere.peass.AddRandomNumbers.doSomething(int)"
            }) {
         int startIndex = kiekerPattern.lastIndexOf(" ") != -1 ? kiekerPattern.lastIndexOf(" ") : 0;
         String call = kiekerPattern.substring(startIndex, kiekerPattern.lastIndexOf("("));
         CallTreeNode addedNode = child3.appendChild(call, kiekerPattern, kiekerPattern);
         callTreeNodes.add(addedNode);
         addedNode.initCommitData();
      }
   }

   private CallTreeNode buildSecondLevel(Set<CallTreeNode> callTreeNodes, CallTreeNode c0_method0Node) {
      CallTreeNode child = c0_method0Node.appendChild("de.dagere.peass.C1_0.<init>()", "new de.dagere.peass.C1_0.<init>()","new de.dagere.peass.C1_0.<init>()");
      callTreeNodes.add(child);
      child.initCommitData();
      
      CallTreeNode child2 = c0_method0Node.appendChild("de.dagere.peass.C0_0.method0(int)", "private int de.dagere.peass.C0_0.method0(int)","private int de.dagere.peass.C0_0.method0(int)");
      callTreeNodes.add(child2);
      child2.initCommitData();
      
      CallTreeNode child3 = c0_method0Node.appendChild("de.dagere.peass.C1_0.method0()", "public int de.dagere.peass.C1_0.method0()","public int de.dagere.peass.C1_0.method0()");
      callTreeNodes.add(child3);
      child3.initCommitData();
      return child3;
   }

   private void buildFirstLevel(Set<CallTreeNode> callTreeNodes, CallTreeNode root) {
      for (String kiekerPattern : new String[] { "new de.dagere.peass.C0_0.<init>()",
            "public int de.dagere.peass.C0_0.method0()",
            }) {
         int startIndex = kiekerPattern.lastIndexOf(" ") != -1 ? kiekerPattern.lastIndexOf(" ") : 0;
         String call = kiekerPattern.substring(startIndex, kiekerPattern.lastIndexOf("("));
         CallTreeNode addedNode = root.appendChild(call, kiekerPattern, kiekerPattern);
         callTreeNodes.add(addedNode);
         addedNode.initCommitData();
      }
   }
}
