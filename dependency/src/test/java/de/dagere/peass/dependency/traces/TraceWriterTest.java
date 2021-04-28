package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.traces.TraceMethodReader;
import de.dagere.peass.dependency.traces.TraceWithMethods;
import de.dagere.peass.dependency.traces.TraceWriter;
import de.dagere.peass.dependency.traces.requitur.ReducedTraceElement;
import de.dagere.peass.dependency.traces.requitur.content.StringContent;

public class TraceWriterTest {
   @Rule
   public TemporaryFolder folder = new TemporaryFolder();

   @Test
   public void testSimpleWriting() throws IOException {
      TraceWriter writer = new TraceWriter("000001", new TestCase("ClazzA", "methodA"), folder.getRoot());

      TraceWithMethods exampleTrace = getTrace();

      writer.writeTrace("000002", 3, Mockito.mock(TraceMethodReader.class), exampleTrace, new LinkedList<>());

      File expectedResultFile = new File(folder.getRoot(), "view_000001/ClazzA/methodA/000002_method");
      Assert.assertTrue(expectedResultFile.exists());
   }

   @Test
   public void testModuleWriting() throws IOException {
      TraceWriter writer = new TraceWriter("000001", new TestCase("ClazzA", "methodA", "moduleA"), folder.getRoot());

      TraceWithMethods exampleTrace = getTrace();

      writer.writeTrace("000002", 3, Mockito.mock(TraceMethodReader.class), exampleTrace, new LinkedList<>());

      File expectedResultFile = new File(folder.getRoot(), "view_000001/moduleA§ClazzA/methodA/000002_method");
      Assert.assertTrue(expectedResultFile.exists());
   }

   private TraceWithMethods getTrace() {
      final LinkedList<ReducedTraceElement> elements = new LinkedList<>();
      elements.add(new ReducedTraceElement(new StringContent("ClazzA#methodA"), 5));
      TraceWithMethods exampleTrace = new TraceWithMethods(elements);
      return exampleTrace;
   }
}
