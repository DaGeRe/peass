package de.dagere.peass.traceminimization;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.dagere.nodeDiffDetector.data.MethodCall;
import de.dagere.nodeDiffDetector.data.TestCase;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.dependency.traces.coverage.CoverageSelectionCommit;
import de.dagere.peass.dependency.traces.coverage.CoverageSelectionInfo;
import de.dagere.peass.dependency.traces.coverage.TraceCallSummary;
import de.dagere.peass.utils.Constants;

public class TestTestcaseSerialization {
   
   @Test
   public void testSerialization() throws JsonProcessingException {
      CoverageSelectionInfo info = new CoverageSelectionInfo();
      CoverageSelectionCommit commit = new CoverageSelectionCommit();
      TestMethodCall test = new TestMethodCall("ClazzA", "methodA2", "moduleA", "int,String");
      TraceCallSummary traceCallSummary = new TraceCallSummary();
      traceCallSummary.addCall("app" + MethodCall.MODULE_SEPARATOR + "ClazzB" + MethodCall.METHOD_SEPARATOR + "methodB(double)");
      traceCallSummary.setTestcase(test);
      commit.getTestcases().put(test, traceCallSummary);
      info.getCommits().put("000001", commit);
      
      String serialized = Constants.OBJECTMAPPER.writeValueAsString(info);
      
      System.out.println(serialized);
      
      CoverageSelectionInfo deserialized = Constants.OBJECTMAPPER.readValue(serialized, CoverageSelectionInfo.class);
      TestMethodCall deserializedTest = deserialized.getCommits().get("000001").getTestcases().keySet().iterator().next();
      Assert.assertEquals("ClazzA", deserializedTest.getClazz());
      Assert.assertEquals("methodA2", deserializedTest.getMethod());
      Assert.assertEquals("moduleA", deserializedTest.getModule());
      Assert.assertEquals("int,String", deserializedTest.getParams());
   }
   
   @Test
   public void testNoParamSerialization() throws JsonProcessingException {
      CoverageSelectionInfo info = new CoverageSelectionInfo();
      CoverageSelectionCommit commit = new CoverageSelectionCommit();
      TestMethodCall test = new TestMethodCall("ClazzA", "methodA2", "moduleA");
      TraceCallSummary traceCallSummary = new TraceCallSummary();
      traceCallSummary.addCall("app" + MethodCall.MODULE_SEPARATOR + "ClazzB" + MethodCall.METHOD_SEPARATOR + "methodB");
      traceCallSummary.setTestcase(test);
      commit.getTestcases().put(test, traceCallSummary);
      info.getCommits().put("000001", commit);
      
      String serialized = Constants.OBJECTMAPPER.writeValueAsString(info);
      
      System.out.println(serialized);
      
      CoverageSelectionInfo deserialized = Constants.OBJECTMAPPER.readValue(serialized, CoverageSelectionInfo.class);
      TestMethodCall deserializedTest = deserialized.getCommits().get("000001").getTestcases().keySet().iterator().next();
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
         TestCase test = new TestMethodCall("ClazzA", "methodA(A,B,C)", "moduleA", null);
      });
      
      Assertions.assertThrows(RuntimeException.class, () ->{
         TestCase test = new TestMethodCall("ClazzA#methodA", null, "moduleA", null);
      });
   }
}
