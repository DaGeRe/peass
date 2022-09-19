package de.dagere.peass.traceminimization;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependency.traces.coverage.CoverageSelectionCommit;
import de.dagere.peass.dependency.traces.coverage.CoverageSelectionInfo;
import de.dagere.peass.dependency.traces.coverage.TraceCallSummary;
import de.dagere.peass.utils.Constants;

public class TestTestcaseSerialization {
   
   @Test
   public void testSerialization() throws JsonProcessingException {
      CoverageSelectionInfo info = new CoverageSelectionInfo();
      CoverageSelectionCommit version = new CoverageSelectionCommit();
      TestMethodCall test = new TestMethodCall("ClazzA", "methodA2", "moduleA", "int,String");
      TraceCallSummary traceCallSummary = new TraceCallSummary();
      traceCallSummary.addCall("app" + ChangedEntity.MODULE_SEPARATOR + "ClazzB" + ChangedEntity.METHOD_SEPARATOR + "methodB(double)");
      traceCallSummary.setTestcase(test);
      version.getTestcases().put(test, traceCallSummary);
      info.getVersions().put("000001", version);
      
      String serialized = Constants.OBJECTMAPPER.writeValueAsString(info);
      
      System.out.println(serialized);
      
      CoverageSelectionInfo deserialized = Constants.OBJECTMAPPER.readValue(serialized, CoverageSelectionInfo.class);
      TestCase deserializedTest = deserialized.getVersions().get("000001").getTestcases().keySet().iterator().next();
      Assert.assertEquals("ClazzA", deserializedTest.getClazz());
      Assert.assertEquals("methodA2", deserializedTest.getMethod());
      Assert.assertEquals("moduleA", deserializedTest.getModule());
      Assert.assertEquals("int,String", deserializedTest.getParams());
   }
   
   @Test
   public void testNoParamSerialization() throws JsonProcessingException {
      CoverageSelectionInfo info = new CoverageSelectionInfo();
      CoverageSelectionCommit version = new CoverageSelectionCommit();
      TestMethodCall test = new TestMethodCall("ClazzA", "methodA2", "moduleA");
      TraceCallSummary traceCallSummary = new TraceCallSummary();
      traceCallSummary.addCall("app" + ChangedEntity.MODULE_SEPARATOR + "ClazzB" + ChangedEntity.METHOD_SEPARATOR + "methodB");
      traceCallSummary.setTestcase(test);
      version.getTestcases().put(test, traceCallSummary);
      info.getVersions().put("000001", version);
      
      String serialized = Constants.OBJECTMAPPER.writeValueAsString(info);
      
      System.out.println(serialized);
      
      CoverageSelectionInfo deserialized = Constants.OBJECTMAPPER.readValue(serialized, CoverageSelectionInfo.class);
      TestCase deserializedTest = deserialized.getVersions().get("000001").getTestcases().keySet().iterator().next();
      Assert.assertEquals("ClazzA", deserializedTest.getClazz());
      Assert.assertEquals("methodA2", deserializedTest.getMethod());
      Assert.assertEquals("moduleA", deserializedTest.getModule());
      Assert.assertNull(deserializedTest.getParams());
   }
   
   @Test
   public void testCreation() {
      TestMethodCall test = new TestMethodCall("ClazzA", "methodA(A,B,C)", "moduleA");
      Assert.assertEquals("ClazzA", test.getClazz());
      Assert.assertEquals("methodA", test.getMethod());
      Assert.assertEquals("moduleA", test.getModule());
      Assert.assertEquals("A,B,C", test.getParams());
   }
   
   @Test
   public void testError() {
      Assertions.assertThrows(RuntimeException.class, () ->{
         TestCase test = new TestCase("ClazzA", "methodA(A,B,C)", "moduleA", null);
      });
      
      Assertions.assertThrows(RuntimeException.class, () ->{
         TestCase test = new TestCase("ClazzA#methodA", null, "moduleA", null);
      });
   }
}
