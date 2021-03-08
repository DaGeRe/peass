package de.peass.visualization;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import de.peass.analysis.properties.TestMethodChangeReader;

public class TestSourceWriter {
   
   @Before
   public void init() throws FileNotFoundException, IOException {
      TestMethodChangeReader.writeConstructor(TestMethodChangeReader.methodSourceFolder);
      TestMethodChangeReader.writeInit(TestMethodChangeReader.methodSourceFolder);
   }
   
   @Test
   public void testSourceWriter() throws IOException {
      SourceWriter writer = new SourceWriter(new GraphNode("de.Test#methodA", "public void de.Test.methodA()", "public void de.Test.methodA()"), 
            Mockito.mock(BufferedWriter.class), TestMethodChangeReader.methodSourceFolder);
      
      writer.writeSources();
   }
}
