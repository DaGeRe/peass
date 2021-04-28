package de.dagere.peass.traceminimization.pure;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import de.dagere.peass.dependency.traces.requitur.Rule;
import de.dagere.peass.dependency.traces.requitur.Sequitur;
import de.dagere.peass.dependency.traces.requitur.TraceStateTester;
import de.dagere.peass.dependency.traces.requitur.content.Content;
import de.dagere.peass.dependency.traces.requitur.content.RuleContent;
import de.dagere.peass.dependency.traces.requitur.content.StringContent;

/**
 * Tests only sequitur on artificial examples given by manually constructed traces.
 * 
 * @author reichelt
 *
 */
public class TestSequitur {

   private final Sequitur seq = new Sequitur();
   
   public static List<String> contentToStringTrace(final List<Content> expandedTrace) {
      return expandedTrace.stream().map(value -> ((StringContent) value).getValue()).collect(Collectors.toList());
   }

   @Test
   public void testBasic() {
      final List<String> mytrace = new LinkedList<>();
      for (int i = 0; i < 2; i++) {
         mytrace.add("A");
         mytrace.add("B");
      }
      seq.addElements(mytrace);

      final List<Content> trace = seq.getUncompressedTrace();
      System.out.println(trace);
      Assert.assertEquals(2, trace.size());
      Assert.assertEquals("#0", ((RuleContent) trace.get(0)).getValue());
      Assert.assertEquals("#0", ((RuleContent) trace.get(1)).getValue());
      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seq.getRules());
      Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
   }
   
   @Test
   public void testSequence() {
      String content[] = new String[] { "B", "B", "B", "C", "C", "B", "B", "C", "C" };
      System.out.println(Arrays.toString(content));
      List<String> mytrace = Arrays.asList(content);
      
      seq.addElements(mytrace);

      final List<Content> trace = seq.getUncompressedTrace();
      System.out.println(trace + " " + seq.getRules());
      Assert.assertEquals(5, trace.size());
      Assert.assertEquals("#0", ((RuleContent) trace.get(0)).getValue());
      Assert.assertEquals("B", ((StringContent) trace.get(1)).getValue());
      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seq.getRules());
      Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
   }

   @Test
   public void testOverlappingPredecessor() {
      final List<String> mytrace = new LinkedList<>();
      for (final String c : new String[] { "f", "e", "f", "e", "f", "f", "f", "e", "f", "g", "h", "c", "d", "f", "e", "f", "f", "x" }) {
         mytrace.add(c);
      }
      seq.addElements(mytrace);

      final List<Content> trace = seq.getUncompressedTrace();
      System.out.println(trace);
      Assert.assertEquals(9, trace.size());
      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seq.getRules());
      Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
   }

   @Test
   public void testCorrectNamesExtended() {
      final List<String> mytrace = new LinkedList<>();
      for (final String c : new String[] { "setUp", "test", "a", "b", "c", "a", "b", "c" }) {
         mytrace.add(c);
      }
      seq.addElements(mytrace);

      final List<Content> trace = seq.getUncompressedTrace();
      System.out.println(trace);
      Assert.assertEquals(4, trace.size());
      // Assert.assertEquals("#0 (2)", trace.get(2)); // Could be tested sometimes..
      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seq.getRules());
      Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
   }

   @Test
   public void testCorrectNamesSimple() {
      final List<String> mytrace = new LinkedList<>();
      for (final String c : new String[] { "setUp", "test", "a", "b", "a", "b" }) {
         mytrace.add(c);
      }
      seq.addElements(mytrace);

      final List<Content> trace = seq.getUncompressedTrace();
      System.out.println(trace);
      Assert.assertEquals(4, trace.size());
      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seq.getRules());
      Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
   }

   @Test
   public void testOverlappingSuccessor() {
      final Sequitur seq = new Sequitur();
      final List<String> mytrace = new LinkedList<>();
      for (final String c : new String[] { "D", "E", "G", "K", "I", "J", "I", "J", "I", "J", "X", "M", "L", "N", "O", "P", "T", "Q", "R", "S", "R", "S", "R", "S", "U", "V", "W",
            "V", "X", "M", "L", "N", "O", "P", "T", "Q", "R", "S" }) {
         mytrace.add(c);
      }
      seq.addElements(mytrace);

      final List<Content> trace = seq.getUncompressedTrace();
      System.out.println(trace);
      Assert.assertEquals(15, trace.size());
      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seq.getRules());
      Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
   }

   @Test
   public void testViewExample() {
      final List<String> mytrace = new LinkedList<>();
      mytrace.add("A");
      mytrace.add("B");
      for (int j = 0; j < 5; j++) {
         for (int i = 0; i < 5; i++) {
            mytrace.add("C");
            mytrace.add("D");
         }
         mytrace.add("E");
      }
      mytrace.add("E");
      seq.addElements(mytrace);

      final List<Content> trace = seq.getUncompressedTrace();
      System.out.println(trace);
      Assert.assertEquals(8, trace.size());
      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seq.getRules());
      Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
   }

   @Test
   public void testTriple() {
      final List<String> mytrace = new LinkedList<>();
      for (int i = 0; i < 2; i++) {
         mytrace.add("A");
         mytrace.add("B");
         mytrace.add("C");
      }
      seq.addElements(mytrace);

      final List<Content> trace = seq.getUncompressedTrace();
      System.out.println(trace);
      Assert.assertEquals(2, trace.size());
      Assert.assertEquals("#1", ((RuleContent) trace.get(0)).getValue());
      Assert.assertEquals("#1", ((RuleContent) trace.get(1)).getValue());
      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seq.getRules());
      Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
   }

   @Test
   public void testQuad() {
      final List<String> mytrace = new LinkedList<>();
      for (int i = 0; i < 2; i++) {
         mytrace.add("A");
         mytrace.add("B");
         mytrace.add("C");
         mytrace.add("D");
      }
      seq.addElements(mytrace);

      final List<Content> trace = seq.getUncompressedTrace();
      System.out.println(trace);
      System.out.println(seq.getRules());
      Assert.assertEquals(2, trace.size());
      Assert.assertEquals(1, seq.getRules().size());
      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seq.getRules());
      Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
   }

   @Test
   public void test6() {
      final List<String> mytrace = new LinkedList<>();
      for (int i = 0; i < 2; i++) {
         mytrace.add("A");
         mytrace.add("B");
         mytrace.add("C");
         mytrace.add("D");
         mytrace.add("E");
         mytrace.add("F");
      }
      seq.addElements(mytrace);

      final List<Content> trace = seq.getUncompressedTrace();
      System.out.println(trace);
      System.out.println(seq.getRules());
      Assert.assertEquals(2, trace.size());
      Assert.assertEquals(1, seq.getRules().size());
      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seq.getRules());
      Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
   }

   @Test
   public void testNested() {
      final List<String> mytrace = new LinkedList<>();
      for (int j = 0; j < 3; j++) {
         for (int i = 0; i < 2; i++) {
            mytrace.add("A");
            mytrace.add("B");
            mytrace.add("C");
         }
         mytrace.add("D");
         mytrace.add("E");
         mytrace.add("F");
      }
      mytrace.add("A");
      mytrace.add("B");
      mytrace.add("H");
      seq.addElements(mytrace);

      final List<Content> trace = seq.getUncompressedTrace();
      System.out.println(trace);
      System.out.println(seq.getRules());
      Assert.assertEquals(5, trace.size());
      Assert.assertEquals(3, seq.getRules().size());
      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seq.getRules());
      Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
   }

   @Test
   public void testUnitRules() {
      // AABBCCBDDDEDEEDEEBCCBCCBCCBCC
      String content[] = new String[] { "A", "A", "B", "B", "C", "C", "B", "D", "D", "D", "E", "D", "E", "E", "D", "E", "E", "B", "C", "C", "B", "C", "C", "B", "C", "C", "B", "C",
            "C" };
      System.out.println(Arrays.toString(content));
      List<String> trace = Arrays.asList(content);
      final Sequitur seq = new Sequitur();
      seq.addElements(trace);

      for (Entry<String, Rule> rule : seq.getRules().entrySet()) {
         System.out.println(rule.getValue());
      }

      for (Content traceElement : seq.getUncompressedTrace()) {
         if (traceElement instanceof RuleContent) {
            System.out.print(((RuleContent) traceElement).getValue() + " ");
         } else {
            System.out.print(traceElement + " ");
         }
      }
      System.out.println();

      System.out.println(seq.getUncompressedTrace());

      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(seq.getUncompressedTrace(), seq.getRules());
      Assert.assertEquals(trace, contentToStringTrace(expandedTrace));

   }

   @Test
   public void testRuleOnce() {
      final List<String> mytrace = new LinkedList<>();
      for (int j = 0; j < 2; j++) {
         for (int i = 0; i < 2; i++) {
            mytrace.add("A");
            mytrace.add("B");
            mytrace.add("C");
         }
         mytrace.add("D");
         mytrace.add("E");
         mytrace.add("F");
      }
      mytrace.add("A");
      mytrace.add("B");
      mytrace.add("C");
      seq.addElements(mytrace);

      final List<Content> trace = seq.getUncompressedTrace();
      System.out.println(trace);
      System.out.println(seq.getRules());
      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seq.getRules());
      Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
   }

   @Test
   public void testMultipleNest() {

      final List<String> mytrace = new LinkedList<>();
      for (int k = 0; k < 2; k++) {
         for (int j = 0; j < 2; j++) {
            for (int i = 0; i < 2; i++) {
               mytrace.add("A");
               mytrace.add("B");
               mytrace.add("C");
            }
            mytrace.add("D");
            mytrace.add("E");
            mytrace.add("F");
         }
         mytrace.add("A");
         // mytrace.add("B");
         // mytrace.add("H");
      }
      seq.addElements(mytrace);

      final List<Content> trace = seq.getUncompressedTrace();
      System.out.println(trace);
      System.out.println(seq.getRules());
      // Assert.assertEquals(10, trace.size());
      Assert.assertEquals(5, seq.getRules().size());
      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seq.getRules());
      Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
   }

   @Test
   public void testMoreRuleUsage() {
      final List<String> mytrace = new LinkedList<>();
      for (int i = 0; i < 3; i++) {
         mytrace.add("A");
         mytrace.add("B");
      }
      seq.addElements(mytrace);

      final List<Content> trace = seq.getUncompressedTrace();
      System.out.println(trace);
      System.out.println(seq.getRules());
      Assert.assertEquals(3, trace.size());
      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seq.getRules());
      Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
   }
}
