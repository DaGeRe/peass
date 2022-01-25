package de.dagere.peass.dependency.traces.coverage;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.traces.TraceElementContent;
import de.dagere.requitur.content.Content;

public class TestTraceSummaryTransformer {
   
   @Test
   public void testRegularTrace() {
      List<Content> traceElements = buildTestTrace();
      TraceCallSummary summary = TraceSummaryTransformer.transform(new TestCase("Clazz#test"), traceElements);
      
      System.out.println(summary.getCallCounts());
      
      Assert.assertEquals(summary.getCallCounts().get("de.dagere.peass.ExampleClazz#method0").intValue(), 5);
      Assert.assertEquals(summary.getCallCounts().get("de.dagere.peass.ExampleClazz#method1").intValue(), 3);
      Assert.assertEquals(summary.getCallCounts().get("de.dagere.peass.ExampleClazz#method2(int)").intValue(), 2);
   }

   public static List<Content> buildTestTrace() {
      List<Content> traceElements = new LinkedList<>();
      for (int i = 0; i < 5; i++) {
         traceElements.add(new TraceElementContent("de.dagere.peass.ExampleClazz", "method0", "", new String[0], 0));
      }
      for (int i = 0; i < 3; i++) {
         traceElements.add(new TraceElementContent("de.dagere.peass.ExampleClazz", "method1", "", new String[0], 0));
      }
      for (int i = 0; i < 2; i++) {
         traceElements.add(new TraceElementContent("de.dagere.peass.ExampleClazz", "method2", "", new String[] {"int"}, 0));
      }
      return traceElements;
   }
   
   public static List<Content> buildOtherTrace() {
      List<Content> traceElements = new LinkedList<>();
      for (int i = 0; i < 5; i++) {
         traceElements.add(new TraceElementContent("de.dagere.peass.ExampleClazzB", "method0", "", new String[0], 0));
      }
      for (int i = 0; i < 3; i++) {
         traceElements.add(new TraceElementContent("de.dagere.peass.ExampleClazzB", "method1", "", new String[0], 0));
      }
      for (int i = 0; i < 2; i++) {
         traceElements.add(new TraceElementContent("de.dagere.peass.ExampleClazzB", "method2", "", new String[] {"int"}, 0));
      }
      traceElements.add(new TraceElementContent("de.dagere.peass.ExampleClazz", "method0", "", new String[0], 0));
      return traceElements;
   }
   
   public static List<Content> buildBetterMatchingOtherTrace() {
      List<Content> traceElements = new LinkedList<>();
      for (int i = 0; i < 10; i++) {
         traceElements.add(new TraceElementContent("de.dagere.peass.ExampleClazzB", "method0", "", new String[0], 0));
      }
      for (int i = 0; i < 3; i++) {
         traceElements.add(new TraceElementContent("de.dagere.peass.ExampleClazzB", "method1", "", new String[0], 0));
      }
      for (int i = 0; i < 2; i++) {
         traceElements.add(new TraceElementContent("de.dagere.peass.ExampleClazzB", "method2", "", new String[] {"int"}, 0));
      }
      return traceElements;
   }
}
