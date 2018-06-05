package de.peran;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.peran.dependency.analysis.data.TestCase;
import de.peran.reduceddependency.ChangedTraceTests;

public class TestSerialization {
   @Test
   public void testSerialization() throws IOException {
      final ChangedTraceTests tests = new ChangedTraceTests();
      tests.addCall("v1", new TestCase("Test1#test"));
      tests.addCall("v2", new TestCase("Test1#test"));
      
      final ObjectMapper mapper = new ObjectMapper();
      final String json =  mapper.writeValueAsString(tests);
      
      Assert.assertNotNull(json);
      System.out.println(json);
      
      final ChangedTraceTests deserialize = mapper.readValue(json, ChangedTraceTests.class);
      
      Assert.assertNotNull(deserialize);
      Assert.assertEquals(tests.getVersions().get("v1").getTestcases(), deserialize.getVersions().get("v1").getTestcases());
   }
}
