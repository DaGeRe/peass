package de.dagere.peass.dependency.traces.coverage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsIterableContaining;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.traces.requitur.content.Content;

public class TestCoverageBasedSelection {
   
   @Test
   public void selectNoTestSelection() {
      List<TraceCallSummary> traces = new LinkedList<>();
      Set<ChangedEntity> changes = new HashSet<>();
      changes.add(new ChangedEntity("de.dagere.peass.ExampleClazz", "", "method1"));
      Set<TestCase> selected = CoverageBasedSelector.selectBasedOnCoverage(traces, changes).getTestcases().keySet();
      
      MatcherAssert.assertThat(selected, IsEmptyCollection.empty());
   }
   
   @Test
   public void selectOneTestSelection() {
      List<TraceCallSummary> traces = getTraceSummaryList();
      Set<ChangedEntity> changes = new HashSet<>();
      changes.add(new ChangedEntity("de.dagere.peass.ExampleClazz", "", "method1"));
      Set<TestCase> selected = CoverageBasedSelector.selectBasedOnCoverage(traces, changes).getTestcases().keySet();
      
      MatcherAssert.assertThat(selected, IsIterableContaining.hasItem(new TestCase("ClazzA#testA")));
   }
   
   @Test
   public void testTwoTestsSelection() {
      List<TraceCallSummary> traces = getTraceSummaryList();
      Set<ChangedEntity> changes = new HashSet<>();
      changes.add(new ChangedEntity("de.dagere.peass.ExampleClazz", "", "method1"));
      changes.add(new ChangedEntity("de.dagere.peass.ExampleClazzB", "", "method0"));
      CoverageSelectionVersion selection = CoverageBasedSelector.selectBasedOnCoverage(traces, changes);
      Set<TestCase> selected = selection.getTestcases().keySet();
      
      MatcherAssert.assertThat(selected, IsIterableContaining.hasItem(new TestCase("ClazzA#testA")));
      MatcherAssert.assertThat(selected, IsIterableContaining.hasItem(new TestCase("ClazzC#testC")));
      
      TraceCallSummary summaryA = selection.getTestcases().get(new TestCase("ClazzA#testA"));
      TraceCallSummary summaryB = selection.getTestcases().get(new TestCase("ClazzB#testB"));
      TraceCallSummary summaryC = selection.getTestcases().get(new TestCase("ClazzC#testC"));
      Assert.assertEquals(summaryA.getOverallScore(), 3); 
      Assert.assertEquals(summaryB.getOverallScore(), 0); 
      Assert.assertEquals(summaryC.getOverallScore(), 10);
      
      Assert.assertEquals(summaryA.isSelected(), true); 
      Assert.assertEquals(summaryB.isSelected(), false); 
      Assert.assertEquals(summaryC.isSelected(), true);
      
      MatcherAssert.assertThat(summaryA.getSelectedChanges(), IsIterableContaining.hasItem("de.dagere.peass.ExampleClazz#method1"));
      MatcherAssert.assertThat(summaryB.getSelectedChanges(), IsEmptyCollection.empty());
      MatcherAssert.assertThat(summaryC.getSelectedChanges(), IsIterableContaining.hasItem("de.dagere.peass.ExampleClazzB#method0"));
   }
   
   @Test
   public void testSelectionBasedOnParameterizedChange() {
      List<TraceCallSummary> traces = getTraceSummaryList();
      Set<ChangedEntity> changes = new HashSet<>();
      ChangedEntity entityWithIntParameter = new ChangedEntity("de.dagere.peass.ExampleClazz", "", "method2");
      entityWithIntParameter.getParameters().add("int");
      changes.add(entityWithIntParameter);
      Set<TestCase> selected = CoverageBasedSelector.selectBasedOnCoverage(traces, changes).getTestcases().keySet();
      
      MatcherAssert.assertThat(selected, IsIterableContaining.hasItem(new TestCase("ClazzA#testA")));
   }
   
   @Test
   public void testClassChangeSelection() {
      List<TraceCallSummary> traces = getTraceSummaryList();
      Set<ChangedEntity> changes = new HashSet<>();
      changes.add(new ChangedEntity("de.dagere.peass.ExampleClazz", "", null));
      Set<TestCase> selected = CoverageBasedSelector.selectBasedOnCoverage(traces, changes).getTestcases().keySet();
      
      MatcherAssert.assertThat(selected, IsIterableContaining.hasItem(new TestCase("ClazzA#testA")));
   }
   
   private List<TraceCallSummary> getTraceSummaryList() {
      List<Content> firstTrace = TestTraceSummaryTransformer.buildTestTrace();
      TraceCallSummary summary = TraceSummaryTransformer.transform(new TestCase("ClazzA#testA"), firstTrace);
      
      List<Content> secondTrace = TestTraceSummaryTransformer.buildOtherTrace();
      TraceCallSummary summary2 = TraceSummaryTransformer.transform(new TestCase("ClazzB#testB"), secondTrace);
      
      List<Content> betterTrace = TestTraceSummaryTransformer.buildBetterMatchingOtherTrace();
      TraceCallSummary summary3 = TraceSummaryTransformer.transform(new TestCase("ClazzC#testC"), betterTrace);
      
      List<TraceCallSummary> traces = Arrays.asList(summary, summary2, summary3);
      return traces;
   }
}
