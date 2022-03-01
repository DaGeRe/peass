package de.dagere.peass.measurement.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.TestExecutionData;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;

public class TestCreateMeasurementExecutionScript {
   @Test
   public void testFromDependencies() throws IOException {
      ExecutionData executiondata = buildExecutionDataWithTests();

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (PrintStream ps = new PrintStream(baos)) {
         RunCommandWriter writer = new RunCommandWriter(ps, "experiment-1", executiondata);
         CreateMeasurementExecutionScript.generateExecuteCommands(executiondata, "experiment-1", writer);
      }

      String result = baos.toString();
      System.out.println(result);
      
      MatcherAssert.assertThat(result, Matchers.containsString("-test Test1#testMe"));
      MatcherAssert.assertThat(result, Matchers.containsString("-test Test5#testMe"));
   }

   private ExecutionData buildExecutionDataWithTests() {
      Dependencies dependencies = TestExecutionData.buildExampleDependencies();

      VersionStaticSelection version2 = dependencies.getVersions().get("000002");
      version2.getChangedClazzes().put(new ChangedEntity("Test1#testMe"), new TestSet(new TestCase("Test1#testMe")));
      
      VersionStaticSelection version5 = dependencies.getVersions().get("000005");
      version5.getChangedClazzes().put(new ChangedEntity("Test5#testMe"), new TestSet(new TestCase("Test5#testMe")));

      ExecutionData executiondata = new ExecutionData(dependencies);
      return executiondata;
   }
}
