package de.dagere.peass.ci.analysis.changes;

import java.io.File;

import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.Test;

import de.dagere.peass.analysis.changes.ChangeReader;
import de.dagere.peass.ci.TestChangeReading;

public class TestChangeReader {

   @Test
   public void testParameterizedReading() throws JAXBException {
      ChangeReader reader = new ChangeReader("demo");
      reader.readFile(new File(TestChangeReading.DATA_READING_FOLDER, "parameterized_measurementsFull"));
   }
}
