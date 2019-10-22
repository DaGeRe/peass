package de.peass.measurement;

import java.util.LinkedList;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import de.peass.measurement.rca.LevelManager;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.measurement.rca.helper.TreeBuilder;
import de.peass.measurement.rca.kieker.BothTreeReader;
import de.peass.measurement.rca.serialization.MeasuredNode;

public class LevelManagerTest {
   TreeBuilder builder1 = new TreeBuilder();
   TreeBuilder builder2 = new TreeBuilder();

   @Test
   public void testLevelGoing() throws Exception {
      final CauseSearchData data = buildPersistedData();

      final BothTreeReader mock = buildTree();

      final LinkedList<CallTreeNode> currentVersionNodeList = new LinkedList<>();
      final LinkedList<CallTreeNode> currentVersionPredecessorNodeList = new LinkedList<>();

      new LevelManager(currentVersionPredecessorNodeList, currentVersionNodeList, mock).goToLastLevel(data.getNodes());

      System.out.println(currentVersionPredecessorNodeList);
      Assert.assertEquals(2, currentVersionPredecessorNodeList.size());
      Assert.assertEquals(2, currentVersionNodeList.size());
   }

   @Test
   public void testTwoLevelGoing() throws Exception {
      final CauseSearchData data = buildPersistedData();

      final MeasuredNode rootMeasured = addSecondLevel(data);

      final BothTreeReader mock = buildTree();

      final LinkedList<CallTreeNode> currentVersionNodeList = new LinkedList<>();
      final LinkedList<CallTreeNode> currentVersionPredecessorNodeList = new LinkedList<>();

      new LevelManager(currentVersionPredecessorNodeList, currentVersionNodeList, mock).goToLastLevel(rootMeasured);

      System.out.println(currentVersionPredecessorNodeList);
      Assert.assertEquals(1, currentVersionPredecessorNodeList.size());
      Assert.assertEquals(1, currentVersionNodeList.size());
   }

   @Test
   public void testLongTree() {
      final CauseSearchData data = new CauseSearchData();
      final MeasuredNode rootMeasured = new MeasuredNode();
      rootMeasured.setCall("Test#test");
      rootMeasured.setKiekerPattern("public void Test.test");
      data.setNodes(rootMeasured);

      final CallTreeNode root = new CallTreeNode("Test#test", "public void Test.test");
      CallTreeNode current = root;
      MeasuredNode measuredCurrent = rootMeasured;
      for (int i = 0; i < 20; i++) {
         final String call = "C" + i + ".method" + i;
         final String kiekerPattern = "public void " + call;
         current = current.appendChild(call, kiekerPattern);
         final MeasuredNode childMeasured = new MeasuredNode();
         measuredCurrent.getChilds().add(childMeasured);
         childMeasured.setCall(call);
         childMeasured.setKiekerPattern(kiekerPattern);
         measuredCurrent = childMeasured;
      }
      current.appendChild("FinalClass.finalMethod", "public void FinalClass.finalMethod");

      final BothTreeReader mock = Mockito.mock(BothTreeReader.class);
      Mockito.when(mock.getRootPredecessor()).thenReturn(root);
      Mockito.when(mock.getRootVersion()).thenReturn(root);

      final LinkedList<CallTreeNode> currentVersionNodeList = new LinkedList<>();
      final LinkedList<CallTreeNode> currentVersionPredecessorNodeList = new LinkedList<>();

      new LevelManager(currentVersionPredecessorNodeList, currentVersionNodeList, mock).goToLastLevel(rootMeasured);

      System.out.println(currentVersionPredecessorNodeList);
      Assert.assertEquals(1, currentVersionPredecessorNodeList.size());
      Assert.assertEquals(1, currentVersionNodeList.size());
      Assert.assertEquals("FinalClass.finalMethod", currentVersionNodeList.get(0).getCall());
   }

   private MeasuredNode addSecondLevel(final CauseSearchData data) {
      final MeasuredNode child1 = new MeasuredNode();
      child1.setCall("ClassA.methodA");
      child1.setKiekerPattern("public void ClassA.methodA");

      final MeasuredNode child2 = new MeasuredNode();
      child2.setCall("ClassC.methodC");
      child2.setKiekerPattern("public void ClassC.methodC");

      final MeasuredNode rootMeasured = data.getNodes();
      data.setNodes(rootMeasured);
      rootMeasured.getChilds().add(child1);
      rootMeasured.getChilds().add(child2);
      return rootMeasured;
   }

   private BothTreeReader buildTree() {
      final BothTreeReader mock = Mockito.mock(BothTreeReader.class);
      Mockito.when(mock.getRootPredecessor()).thenReturn(builder1.getRoot());
      Mockito.when(mock.getRootVersion()).thenReturn(builder2.getRoot());
      return mock;
   }

   @Test
   public void testCauseDataReusing() throws Exception {
      final CauseSearchData data = buildPersistedData();

      final BothTreeReader mock = buildTree();

      final LinkedList<CallTreeNode> currentVersionNodeList = new LinkedList<>();
      final LinkedList<CallTreeNode> currentVersionPredecessorNodeList = new LinkedList<>();

      new LevelManager(currentVersionPredecessorNodeList, currentVersionNodeList, mock).goToLastLevel(data.getNodes());

      data.addDiff(builder1.getA());
   }

   private CauseSearchData buildPersistedData() {
      final CauseSearchData data = new CauseSearchData();
      final MeasuredNode rootMeasured = new MeasuredNode();
      rootMeasured.setCall("Test#test");
      rootMeasured.setKiekerPattern("public void Test.test");
      data.setNodes(rootMeasured);
      return data;
   }
}
