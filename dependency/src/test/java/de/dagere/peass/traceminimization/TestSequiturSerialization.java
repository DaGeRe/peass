package de.dagere.peass.traceminimization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.github.javaparser.ParseException;

import de.dagere.peass.dependency.analysis.data.TraceElement;
import de.dagere.peass.dependency.traces.TraceMethodReader;
import de.dagere.peass.dependency.traces.TraceWithMethods;
import de.dagere.peass.dependency.traces.requitur.Sequitur;

public class TestSequiturSerialization {

   @Test
   public void testSimpleRepetition() throws ParseException, IOException {
      final List<TraceElement> calls = new LinkedList<>();
      calls.add(new TraceElement("CalleeSimpleFor", "methodA", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));

      final TraceMethodReader reader = new TraceMethodReader(calls, new File("src/test/java/"));
      final TraceWithMethods trace = reader.getTraceWithMethods();

      testTrace(calls, trace);
   }

   @Test
   public void testMultilineRepetition() throws ParseException, IOException {
      final List<TraceElement> calls = new LinkedList<>();
      calls.add(new TraceElement("CalleeSimpleFor", "methodA", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodA", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodD", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodD", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodE", 0));

      final TraceMethodReader reader = new TraceMethodReader(calls, new File("src/test/java/"));
      final TraceWithMethods trace = reader.getTraceWithMethods();

      testTrace(calls, trace);
   }

   @Test
   public void testDeepRepetition() throws ParseException, IOException {
      final List<TraceElement> calls = new LinkedList<>();
      for (int k = 0; k < 2; k++) {
         calls.add(new TraceElement("CalleeSimpleFor", "methodA", 0));
         for (int i = 0; i < 2; i++) {
            calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
            for (int j = 0; j < 2; j++) {
               calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
               calls.add(new TraceElement("CalleeSimpleFor", "methodD", 0));
            }
         }
      }

      final TraceMethodReader reader = new TraceMethodReader(calls, new File("src/test/java/"));
      final TraceWithMethods trace = reader.getTraceWithMethods();

      testTrace(calls, trace);
   }

   @Test
   public void testDeepRepetition2() throws ParseException, IOException {
      final List<TraceElement> calls = new LinkedList<>();
      calls.add(new TraceElement("CalleeSimpleFor", "methodA", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
      for (int i = 0; i < 5; i++) {
         calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
         calls.add(new TraceElement("CalleeSimpleFor", "methodD", 0));
      }

      calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodD", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodD", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodA", 0));
      calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
      for (int i = 0; i < 5; i++) {
         calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
         calls.add(new TraceElement("CalleeSimpleFor", "methodD", 0));
      }

      final TraceMethodReader reader = new TraceMethodReader(calls, new File("src/test/java/"));
      final TraceWithMethods trace = reader.getTraceWithMethods();

      testTrace(calls, trace);
   }

   @Test
   public void testDeepRepetition3() throws ParseException, IOException {
      final List<TraceElement> calls = new LinkedList<>();
      for (int j = 0; j < 3; j++) {
         for (int i = 0; i < 5; i++) {
            calls.add(new TraceElement("CalleeSimpleFor", "methodA", 0));
            calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
            for (int k = 0; k < 5; k++) {
               calls.add(new TraceElement("CalleeSimpleFor", "method1", 0));
               calls.add(new TraceElement("CalleeSimpleFor", "method2", 0));
            }
            calls.add(new TraceElement("CalleeSimpleFor", "methodG", 0));
         }
         calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
         calls.add(new TraceElement("CalleeSimpleFor", "methodD", 0));
      }
      calls.add(new TraceElement("CalleeSimpleFor", "methodE", 0));

      final TraceMethodReader reader = new TraceMethodReader(calls, new File("src/test/java/"));
      final TraceWithMethods trace = reader.getTraceWithMethods();

      testTrace(calls, trace);
   }

   private void testTrace(final List<TraceElement> calls, final TraceWithMethods trace) throws IOException, FileNotFoundException {
      File temp = File.createTempFile("tempfile", ".tmp", new File("target"));

      try (BufferedWriter bw = new BufferedWriter(new FileWriter(temp))) {
         bw.write(trace.getTraceMethods());
      }

      System.out.println(trace.getTraceMethods());

      List<String> readTrace = Sequitur.getExpandedTrace(temp);
      readTrace.forEach(System.out::println);
      for (int index = 0; index < readTrace.size(); index++) {
         Assert.assertEquals(calls.get(index).getClazz() + "#" + calls.get(index).getMethod(), readTrace.get(index));
      }
   }
}
