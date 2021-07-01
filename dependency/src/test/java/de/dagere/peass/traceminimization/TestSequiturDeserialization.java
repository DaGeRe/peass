package de.dagere.peass.traceminimization;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Test;

import de.dagere.peass.dependency.traces.requitur.Sequitur;

public class TestSequiturDeserialization {
   
   @Test
   public void testBasicReading() throws FileNotFoundException, IOException {
      File exampleTraceFile = new File("src/test/resources/example_trace_sequitur");
      List<String> methods = Sequitur.getExpandedTrace(exampleTraceFile);
      MatcherAssert.assertThat(methods, IsIterableContaining.hasItem("de.dagere.peass.ExampleTest#test"));
      MatcherAssert.assertThat(methods, IsIterableContaining.hasItem("de.dagere.peass.ExampleClazz#calleeMethod(int)"));
      MatcherAssert.assertThat(methods, IsIterableContaining.hasItem("de.dagere.peass.ExampleClazz#calleeMethod(int,java.lang.String)"));
   }
}
