package de.dagere.peass.dependency.persistence;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class TestExecutionData {
   @Test
   public void testConversion() {
      StaticTestSelection dependencies = buildExampleDependencies();

      Assert.assertArrayEquals(new String[] { "000001", "000002", "000004", "000005" }, dependencies.getCommitNames());

      ExecutionData executions = new ExecutionData(dependencies);
      Assert.assertArrayEquals(new String[] { "000001", "000002", "000004", "000005" }, executions.getCommitNames());
   }

   public static StaticTestSelection buildExampleDependencies() {
      StaticTestSelection dependencies = new StaticTestSelection();

      dependencies.setUrl("https://test");
      dependencies.getInitialcommit().setCommit("000001");
      dependencies.getCommits().put("000002", new CommitStaticSelection());
      dependencies.getCommits().put("000004", new CommitStaticSelection());
      dependencies.getCommits().put("000005", new CommitStaticSelection());
      return dependencies;
   }
   
   @Test
   public void testDoubleConversion() {
      StaticTestSelection dependencies = buildExampleDependencies();
      
      ExecutionData data = new ExecutionData(dependencies);
      
      StaticTestSelection doubleConverted = new StaticTestSelection(data);
      
      Assert.assertEquals("000001", doubleConverted.getInitialcommit().getCommit());
      Assert.assertEquals(dependencies.getCommits().size(), doubleConverted.getCommits().size());
   }
}
