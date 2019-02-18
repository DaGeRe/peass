package de.peran.reading;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.vcs.GitCommit;

public class TestChangedTraceTestSerialisation {
   
   @Before
   public void initializeComparator() {
      VersionComparator.setVersions(Arrays.asList(new GitCommit("v1", "", "", ""), new GitCommit("v2", "", "", "")));
   }
   
   @Test
   public void testModule() throws IOException {
      final ExecutionData allVersionsTests = new ExecutionData();
      final TestSet testSet = new TestSet();
      testSet.addTest(new TestCase("MyClazz", "myMethod", "module-3-bla"));
      allVersionsTests.addCall("v1", testSet);
      
      final String serialized = new ObjectMapper().writeValueAsString(allVersionsTests);
      System.out.println(serialized);
      
      final ExecutionData deserialized = new ObjectMapper().readValue(serialized, ExecutionData.class);
      final TestSet testSetDeserialized = deserialized.getVersions().get("v1");
      Assert.assertNotNull(testSetDeserialized);
      
      final TestCase testcaseDeserialized = testSetDeserialized.getTests().iterator().next();
      Assert.assertEquals("MyClazz", testcaseDeserialized.getClazz());
   }
   
   @Test
   public void testVersionContent() throws IOException {
      final ExecutionData tests = new ExecutionData();
      tests.addCall("v1", "v0", new TestCase("Test1#test"));
      tests.addCall("v2", "v1", new TestCase("Test1#test"));
      
      final ObjectMapper mapper = new ObjectMapper();
      final String json =  mapper.writeValueAsString(tests);
      
      Assert.assertNotNull(json);
      System.out.println(json);
      
      final ExecutionData deserialize = mapper.readValue(json, ExecutionData.class);
      
      Assert.assertNotNull(deserialize);
      Assert.assertEquals(tests.getVersions().get("v1").getTestcases(), deserialize.getVersions().get("v1").getTestcases());
   }
}
