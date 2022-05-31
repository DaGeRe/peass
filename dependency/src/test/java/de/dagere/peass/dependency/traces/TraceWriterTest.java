package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.requitur.ReducedTraceElement;
import de.dagere.requitur.content.StringContent;

public class TraceWriterTest {
   @TempDir
   File tempDir;

   @Test
   public void testSimpleWriting() throws IOException {
      ResultsFolders resultsFolders = new ResultsFolders(tempDir, "test");
      TestSelectionConfig testSelectionConfig = new TestSelectionConfig(1, false, true, true, true);
      TraceWriter writer = new TraceWriter("000001", new TestCase("ClazzA", "methodA"), resultsFolders, new TraceFileMapping(), testSelectionConfig);

      TraceWithMethods exampleTrace = getTrace();

      writer.writeTrace("000002", 3, Mockito.mock(TraceMethodReader.class), exampleTrace);

      File expectedResultFile = new File(resultsFolders.getViewFolder(), "view_000001/ClazzA/methodA/000002_method.zip");
      Assert.assertTrue(expectedResultFile.exists());
   }

   @Test
   public void testModuleWriting() throws IOException {
      ResultsFolders resultsFolders = new ResultsFolders(tempDir, "test");
      
      TestSelectionConfig testSelectionConfig = new TestSelectionConfig(1, false, true, true, true);
      TraceWriter writer = new TraceWriter("000001", new TestCase("ClazzA", "methodA", "moduleA"), resultsFolders, new TraceFileMapping(), testSelectionConfig);

      TraceWithMethods exampleTrace = getTrace();

      writer.writeTrace("000002", 3, Mockito.mock(TraceMethodReader.class), exampleTrace);

      File expectedResultFile = new File(resultsFolders.getViewFolder(), "view_000001/moduleA§ClazzA/methodA/000002_method.zip");
      Assert.assertTrue(expectedResultFile.exists());
   }

   private TraceWithMethods getTrace() {
      final LinkedList<ReducedTraceElement> elements = new LinkedList<>();
      elements.add(new ReducedTraceElement(new StringContent("ClazzB#methodB"), 1));
      elements.add(new ReducedTraceElement(new StringContent("ClazzA#methodA"), 5));
      TraceWithMethods exampleTrace = new TraceWithMethods(elements);
      return exampleTrace;
   }
}
