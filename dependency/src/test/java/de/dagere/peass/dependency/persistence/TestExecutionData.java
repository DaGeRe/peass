package de.dagere.peass.dependency.persistence;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class TestExecutionData {
   @Test
   public void testConversion() {
      Dependencies dependencies = new Dependencies();

      dependencies.setUrl("https://test");
      dependencies.getInitialversion().setVersion("1");
      dependencies.getVersions().put("2", new Version());
      dependencies.getVersions().put("4", new Version());
      dependencies.getVersions().put("5", new Version());

      Assert.assertArrayEquals(new String[] { "1", "2", "4", "5" }, dependencies.getVersionNames());

      ExecutionData executions = new ExecutionData(dependencies);
      Assert.assertArrayEquals(new String[] { "1", "2", "4", "5" }, executions.getVersionNames());
   }
}
