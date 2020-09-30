package de.peass.traceminimization.pure;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;

import de.peass.dependency.traces.TraceWithMethods;
import de.peass.dependency.traces.requitur.ReducedTraceElement;
import de.peass.dependency.traces.requitur.Rule;
import de.peass.dependency.traces.requitur.RunLengthEncodingSequitur;
import de.peass.dependency.traces.requitur.Sequitur;
import de.peass.dependency.traces.requitur.TraceStateTester;
import de.peass.dependency.traces.requitur.content.Content;
import de.peass.dependency.traces.requitur.content.RuleContent;

public class TestRLESequiturEfficiency {

   @Test
   public void testUnitRuleExample() {
      String content[] = new String[] { "B", "B", "C", "C", "B", "D", "D", "D", "E", "D", "E", "E", "D", "E", "E", "B", "C", "C", "B", "C", "C", "B", "C", "C", "B", "C",
             "C" };
      System.out.println(Arrays.toString(content));
      List<String> mytrace = Arrays.asList(content);

      final Sequitur seq = new Sequitur();
      seq.addElements(mytrace);
      
      final List<Content> trace = seq.getUncompressedTrace();
      System.out.println(trace + " " + seq.getRules());

      final RunLengthEncodingSequitur runLengthEncodingSequitur = new RunLengthEncodingSequitur(seq);
      runLengthEncodingSequitur.reduce();

      final List<Content> unexpandedTrace = seq.getUncompressedTrace();
      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(unexpandedTrace, seq.getRules());
      Assert.assertEquals(mytrace, TestSequitur.contentToStringTrace(expandedTrace));
      
      Assert.assertEquals(13, runLengthEncodingSequitur.getReadableRLETrace().size());
   }
   
   @Test
   public void testUnitRuleExampleManyOccurences() {
      String content[] = new String[] { "A", "B", "C", "D", "C", "D", "C", "D", "B", "C", "D", "A", "B", "C", "D", "C", "D", 
            "C", "D", "C", "D", "C", "D" };
      System.out.println(Arrays.toString(content));
      List<String> mytrace = Arrays.asList(content);

      final Sequitur seq = new Sequitur();
      seq.addElements(mytrace);
      
      final List<Content> trace = seq.getUncompressedTrace();
      System.out.println(trace + " " + seq.getRules());

      final RunLengthEncodingSequitur runLengthEncodingSequitur = new RunLengthEncodingSequitur(seq);
      runLengthEncodingSequitur.reduce();

      final List<Content> unexpandedTrace = seq.getUncompressedTrace();
      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(unexpandedTrace, seq.getRules());
      Assert.assertEquals(mytrace, TestSequitur.contentToStringTrace(expandedTrace));
   }
   
   @Test
   public void testUnitRuleNewSequences() {
      String content[] = new String[] { "A","A","A","A","B","A","C","A","D","D","D","A","C","A","D","D","D","D","A","C","A","D","D","A","C","A","D","D","D","D","D","B","C","B","E","E","B","C","C" };
   
      System.out.println(Arrays.toString(content));
      List<String> mytrace = Arrays.asList(content);

      final Sequitur seq = new Sequitur();
      seq.addElements(mytrace);
      
      final List<Content> trace = seq.getUncompressedTrace();
      System.out.println(trace + " " + seq.getRules());

      final RunLengthEncodingSequitur runLengthEncodingSequitur = new RunLengthEncodingSequitur(seq);
      runLengthEncodingSequitur.reduce();
      
      System.out.println(runLengthEncodingSequitur.getReadableRLETrace());

      final List<Content> unexpandedTrace = seq.getUncompressedTrace();
      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(unexpandedTrace, seq.getRules());
      Assert.assertEquals(mytrace, TestSequitur.contentToStringTrace(expandedTrace));
   }
   
   
}
