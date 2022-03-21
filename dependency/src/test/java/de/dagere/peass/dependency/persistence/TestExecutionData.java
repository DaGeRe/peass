package de.dagere.peass.dependency.persistence;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class TestExecutionData {
   @Test
   public void testConversion() {
      StaticTestSelection dependencies = buildExampleDependencies();

      Assert.assertArrayEquals(new String[] { "000001", "000002", "000004", "000005" }, dependencies.getVersionNames());

      ExecutionData executions = new ExecutionData(dependencies);
      Assert.assertArrayEquals(new String[] { "000001", "000002", "000004", "000005" }, executions.getVersionNames());
   }

   public static StaticTestSelection buildExampleDependencies() {
      StaticTestSelection dependencies = new StaticTestSelection();

      dependencies.setUrl("https://test");
      dependencies.getInitialversion().setVersion("000001");
      dependencies.getVersions().put("000002", new VersionStaticSelection());
      dependencies.getVersions().put("000004", new VersionStaticSelection());
      dependencies.getVersions().put("000005", new VersionStaticSelection());
      return dependencies;
   }
   
   @Test
   public void testDoubleConversion() {
      StaticTestSelection dependencies = buildExampleDependencies();
      
      ExecutionData data = new ExecutionData(dependencies);
      
      StaticTestSelection doubleConverted = new StaticTestSelection(data);
      
      Assert.assertEquals("000001", doubleConverted.getInitialversion().getVersion());
      Assert.assertEquals(dependencies.getVersions().size(), doubleConverted.getVersions().size());
   }
}
