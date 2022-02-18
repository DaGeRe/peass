package de.dagere.peass.analysis.data;

import java.io.File;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class TestCaseTest {
   
   @Test
   public void testConstructionWithChunk() throws JAXBException {
      File dataFile = new File("src/test/resources/testcaseTest/ExampleTest_test(JUNIT_PARAMETERIZED-0).xml");
      Kopemedata kopemedata = XMLDataLoader.loadData(dataFile);
      
      TestCase test = new TestCase(kopemedata.getTestcases());
      
      Assert.assertEquals(test.getMethod(), "test");
      Assert.assertEquals(test.getParams(), "JUNIT_PARAMETERIZED-0");
   }
}
