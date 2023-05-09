package de.dagere.peass.measurement.rca.searcher;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.CauseTester;
import de.dagere.peass.measurement.rca.RCAStrategy;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.kieker.BothTreeReader;
import de.dagere.peass.utils.Constants;

public class TestLevelCauseSearcher {

   private static final TestMethodCall TEST_CASE = new TestMethodCall("org.gradoop.model.impl.pojo.EdgePojoTest", "createWithEmptyLabelTest");
   private static final CauseSearcherConfig POJO_TEST_CAUSE_CONFIG = new CauseSearcherConfig(TEST_CASE, false, 0.1,
         false, true, RCAStrategy.COMPLETE, 1);
   private static final String COMMIT_PREDECESSOR = "104139feeab5aadbad8da05a0e6083e467e09d56";
   private static final String COMMIT_CURRENT = "cf1a996ffc69fd1fdd94a3eb16ef40eb9843e2de";

   private static final File folder = new File("src/test/resources/gradoop_rca_example/createWithEmptyLabelTest/");
   
   private final File temp = new File("target/temp_test");

   @BeforeEach
   public void init() throws IOException {
      FileUtils.deleteDirectory(temp);
      temp.mkdirs();
   }

   @Test
   public void testCauseSearching() throws StreamReadException, DatabindException, IOException {
      CallTreeNode predecessor = Constants.OBJECTMAPPER.readValue(new File(folder, COMMIT_PREDECESSOR), CallTreeNode.class);
      CallTreeNode current = Constants.OBJECTMAPPER.readValue(new File(folder, COMMIT_CURRENT), CallTreeNode.class);

      MeasurementConfig config = new MeasurementConfig(30);
      config.getFixedCommitConfig().setCommit(COMMIT_CURRENT);
      config.getFixedCommitConfig().setCommitOld(COMMIT_PREDECESSOR);

      setConfigs(current, config);
      setConfigs(predecessor, config);

      CauseTester measurer = Mockito.mock(CauseTester.class);
      CauseSearchFolders foldersMock = Mockito.mock(CauseSearchFolders.class);
      Mockito.when(foldersMock.getRcaTreeFile("cf1a996ffc69fd1fdd94a3eb16ef40eb9843e2de", TEST_CASE)).thenReturn(new File(temp, "createWithEmptyLabelTest.json"));
      Mockito.when(foldersMock.getRcaTreeFolder("cf1a996ffc69fd1fdd94a3eb16ef40eb9843e2de", TEST_CASE)).thenReturn(new File(temp, "createWithEmptyLabelTest"));
      Mockito.when(foldersMock.getRcaTreeFileDetails("cf1a996ffc69fd1fdd94a3eb16ef40eb9843e2de", TEST_CASE)).thenReturn(new File(temp, "createWithEmptyLabelTest/details"));
      Mockito.when(foldersMock.getResultFile(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any())).thenReturn(new File(temp, "resultFile.json"));

      BothTreeReader readMock = Mockito.mock(BothTreeReader.class);
      Mockito.when(readMock.getRootCurrent()).thenReturn(current);
      Mockito.when(readMock.getRootPredecessor()).thenReturn(predecessor);

      LevelCauseSearcher searcher = new LevelCauseSearcher(readMock,
            POJO_TEST_CAUSE_CONFIG,
            measurer,
            config,
            foldersMock,
            new EnvironmentVariables());

      mockMeasurementResults(config, measurer);

      searcher.search();

      CallTreeNode createEdgeLevel2 = predecessor.getChildByKiekerPattern(
            "public org.gradoop.model.impl.pojo.EdgePojo org.gradoop.model.impl.pojo.EdgePojoFactory.createEdge(java.lang.Long,java.lang.String,java.lang.Long,java.lang.Long)");
      CallTreeNode secondChild = createEdgeLevel2.getChildren().get(0);
      MatcherAssert.assertThat(secondChild.getKiekerPattern(), StringContains.containsString("createEdge"));
      MatcherAssert.assertThat(secondChild.getOtherKiekerPattern(), StringContains.containsString("createEdge"));
   }

   private void mockMeasurementResults(MeasurementConfig config, CauseTester measurer) {
      Map<String, long[]> averages = new HashMap<>();
      averages.put("public void org.gradoop.model.impl.pojo.EdgePojoTest.createWithEmptyLabelTest()", new long[] { 100, 150 });
      averages.put("public new org.gradoop.model.impl.pojo.ElementPojoFactory.<init>()", new long[] { 100, 100 });
      averages.put("public new org.gradoop.model.impl.pojo.EdgePojoFactory.<init>()", new long[] { 100, 100 });
      averages.put(
            "public org.gradoop.model.impl.pojo.EdgePojo org.gradoop.model.impl.pojo.EdgePojoFactory.createEdge(java.lang.Long,java.lang.String,java.lang.Long,java.lang.Long)",
            new long[] { 100, 150 });
      averages.put(
            "public org.gradoop.model.impl.pojo.EdgePojo org.gradoop.model.impl.pojo.EdgePojoFactory.createEdge(org.gradoop.model.impl.id.GradoopId,java.lang.String,org.gradoop.model.impl.id.GradoopId,org.gradoop.model.impl.id.GradoopId)",
            new long[] { 100, 150 });
      averages.put("public new org.gradoop.model.impl.id.SequenceIdGenerator.<init>(java.lang.Long)", new long[] { 100, 150 });
      averages.put("public new org.gradoop.model.impl.id.SequenceIdGenerator.<init>()", new long[] { 100, 100 });
      averages.put("public org.gradoop.model.impl.id.GradoopId org.gradoop.model.impl.id.SequenceIdGenerator.createId()", new long[] { 100, 100 });
      averages.put(
            "public org.gradoop.model.impl.pojo.EdgePojo org.gradoop.model.impl.pojo.EdgePojoFactory.createEdge(java.lang.Long,java.lang.String,java.lang.Long,java.lang.Long,java.util.Map,java.util.Set)",
            new long[] { 100, 100 });
      averages.put("new org.gradoop.model.impl.id.GradoopId.<init>(java.lang.Long)", new long[] { 100, 100 });

      Mockito.doAnswer(new Answer<Void>() {
         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            Object[] args = invocation.getArguments();
            System.out.println("Mocking measurement of: " + args[0]);
            List<CallTreeNode> nodes = (List<CallTreeNode>) args[0];

            int vms = 30;
            int iterations = 10;

            for (CallTreeNode node : nodes) {
               node.initCommitData();
               if (!node.getKiekerPattern().equals(CauseSearchData.ADDED)) {
                  long[] average = averages.get(node.getKiekerPattern());
                  System.out.println("Loading: " + node.getKiekerPattern());

                  buildChunks(average[0], config.getFixedCommitConfig().getCommitOld(), node, vms, iterations);
               }

               if (!node.getOtherKiekerPattern().equals(CauseSearchData.ADDED)) {
                  long[] average = averages.get(node.getOtherKiekerPattern());
                  System.out.println("Loading: " + node.getOtherKiekerPattern());

                  buildChunks(average[1], config.getFixedCommitConfig().getCommit(), node, vms, iterations);
               }
            }

            return null;
         }

         private void buildChunks(long average, String commitName, CallTreeNode callTreeNode, int vms, int iterations) {
            final List<StatisticalSummary> statistics = new LinkedList<>();
            for (int vm = 0; vm < vms; vm++) {
               final long deltaVM = (vms / 2) + vm * 2;
               for (int iteration = 0; iteration < iterations; iteration++) {
                  final long deltaIteration = (iterations / 2) + iteration * 2;
                  final long value = average - deltaIteration - deltaVM;
                  final SummaryStatistics statistic = new SummaryStatistics();
                  statistic.addValue(value);
                  statistics.add(statistic);
               }
               callTreeNode.addAggregatedMeasurement(commitName, statistics);
            }
            callTreeNode.createStatistics(commitName);
         }
      }).when(measurer).measureCommit(Mockito.any());
   }

   private void setConfigs(CallTreeNode current, MeasurementConfig config) {
      current.setConfig(config);
      for (CallTreeNode child : current.getChildren()) {
         setConfigs(child, config);
      }
   }
}
