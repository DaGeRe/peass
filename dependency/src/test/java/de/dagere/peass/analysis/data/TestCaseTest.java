package de.dagere.peass.analysis.data;

import java.io.File;


import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class TestCaseTest {
   
   @Test
   public void testConstructionWithChunk() {
      File dataFile = new File("src/test/resources/testcaseTest/ExampleTest_test(JUNIT_PARAMETERIZED-0).json");
      Kopemedata kopemedata = JSONDataLoader.loadData(dataFile);
      
      TestCase test = new TestCase(kopemedata);
      
      Assert.assertEquals(test.getMethod(), "test");
      Assert.assertEquals(test.getParams(), "JUNIT_PARAMETERIZED-0");
   }
}
