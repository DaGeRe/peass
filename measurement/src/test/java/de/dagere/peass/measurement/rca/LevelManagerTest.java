package de.dagere.peass.measurement.rca;

import java.util.LinkedList;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.PersistedTestDataBuilder;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.helper.TreeBuilder;
import de.dagere.peass.measurement.rca.kieker.BothTreeReader;
import de.dagere.peass.measurement.rca.serialization.MeasuredNode;

public class LevelManagerTest {
   private TreeBuilder builder1 = new TreeBuilder();
   private TreeBuilder builder2 = new TreeBuilder();

   private PersistedTestDataBuilder persistedDataBuilder = new PersistedTestDataBuilder();
   
   @Test
   public void testLevelGoing() throws Exception {
      final CauseSearchData data = persistedDataBuilder.getData();

      final BothTreeReader mock = buildTree();

      final LinkedList<CallTreeNode> currentVersionNodeList = new LinkedList<>();
      final LinkedList<CallTreeNode> currentVersionPredecessorNodeList = new LinkedList<>();

      new LevelManager(currentVersionPredecessorNodeList, currentVersionNodeList, mock).goToLastMeasuredLevel(data.getNodes());

      System.out.println(currentVersionPredecessorNodeList);
      System.out.println(currentVersionNodeList);
      testMeasureListCorrectness(currentVersionPredecessorNodeList, builder1);
      testMeasureListCorrectness(currentVersionNodeList, builder2);
   }

   private void testMeasureListCorrectness(final LinkedList<CallTreeNode> currentVersionNodeList, final TreeBuilder builder) {
      Assert.assertEquals(3, currentVersionNodeList.size());
      MatcherAssert.assertThat(currentVersionNodeList, Matchers.hasItem(builder.getA()));
      MatcherAssert.assertThat(currentVersionNodeList, Matchers.hasItem(builder.getC()));
      MatcherAssert.assertThat(currentVersionNodeList, Matchers.hasItem(builder.getConstructor()));
   }

   @Test
   public void testTwoLevelGoing() throws Exception {
      persistedDataBuilder.addSecondLevel();
      final CauseSearchData data = persistedDataBuilder.getData();

      final BothTreeReader mock = buildTree();

      final LinkedList<CallTreeNode> currentVersionNodeList = new LinkedList<>();
      final LinkedList<CallTreeNode> currentVersionPredecessorNodeList = new LinkedList<>();

      new LevelManager(currentVersionPredecessorNodeList, currentVersionNodeList, mock).goToLastMeasuredLevel(data.getNodes());

      System.out.println(currentVersionPredecessorNodeList);
      Assert.assertEquals(1, currentVersionPredecessorNodeList.size());
      Assert.assertEquals(1, currentVersionNodeList.size());
   }

   @Test
   public void testLongTree() {
      final CauseSearchData data = new CauseSearchData();
      final MeasuredNode rootMeasured = new MeasuredNode("Test#test", "public void Test.test()", null);
      data.setNodes(rootMeasured);

      final CallTreeNode root = new CallTreeNode("Test#test", "public void Test.test()", "public void Test.test()", (MeasurementConfig) null);
      CallTreeNode current = root;
      MeasuredNode measuredCurrent = rootMeasured;
      for (int i = 0; i < 20; i++) {
         final String call = "C" + i + ".method" + i;
         final String kiekerPattern = "public void " + call + "()";
         current = current.appendChild(call, kiekerPattern, null);
         final MeasuredNode childMeasured = new MeasuredNode(call, kiekerPattern, null);
         measuredCurrent.getChilds().add(childMeasured);
         measuredCurrent = childMeasured;
      }
      current.appendChild("FinalClass.finalMethod", "public void FinalClass.finalMethod()", null);

      final BothTreeReader mock = Mockito.mock(BothTreeReader.class);
      Mockito.when(mock.getRootPredecessor()).thenReturn(root);
      Mockito.when(mock.getRootVersion()).thenReturn(root);

      final LinkedList<CallTreeNode> currentVersionNodeList = new LinkedList<>();
      final LinkedList<CallTreeNode> currentVersionPredecessorNodeList = new LinkedList<>();

      new LevelManager(currentVersionPredecessorNodeList, currentVersionNodeList, mock).goToLastMeasuredLevel(rootMeasured);

      System.out.println(currentVersionPredecessorNodeList);
      Assert.assertEquals(1, currentVersionPredecessorNodeList.size());
      Assert.assertEquals(1, currentVersionNodeList.size());
      Assert.assertEquals("FinalClass.finalMethod", currentVersionNodeList.get(0).getCall());
   }

   

   private BothTreeReader buildTree() {
      final BothTreeReader mock = Mockito.mock(BothTreeReader.class);
      Mockito.when(mock.getRootPredecessor()).thenReturn(builder1.getRoot());
      Mockito.when(mock.getRootVersion()).thenReturn(builder2.getRoot());
      return mock;
   }

   @Test
   public void testCauseDataReusing() throws Exception {
      final CauseSearchData data = persistedDataBuilder.getData();

      final BothTreeReader mock = buildTree();

      final LinkedList<CallTreeNode> currentVersionNodeList = new LinkedList<>();
      final LinkedList<CallTreeNode> currentVersionPredecessorNodeList = new LinkedList<>();

      new LevelManager(currentVersionPredecessorNodeList, currentVersionNodeList, mock).goToLastMeasuredLevel(data.getNodes());

      builder1.buildMeasurements(builder1.getRoot(), builder1.getA(),builder1.getC());
      data.addDiff(builder1.getA());
      data.addDiff(builder1.getC());
      
      MatcherAssert.assertThat(data.getNodes().getChildren(), Matchers.hasSize(2));
   }
}
