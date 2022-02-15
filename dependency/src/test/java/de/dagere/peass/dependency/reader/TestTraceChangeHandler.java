package de.dagere.peass.dependency.reader;

import org.junit.jupiter.api.Test;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.Version;

public class TestTraceChangeHandler {

   @Test
   public void testAdding() {
      Version versionInfo = new Version();
      versionInfo.getChangedClazzes().put(new ChangedEntity("de.dagere.pass.TestA", "testA", "testModule"), new TestSet());

      TestSet test = new TestSet();
      test.addTest(new TestCase("de.dagere.pass.TestA", "testA", "testModule"));
      test.addTest(new TestCase("de.dagere.pass.TestA", "testA2", "testModule"));
      test.addTest(new TestCase("de.dagere.pass.TestB", "testB", "testModule"));
      test.addTest(new TestCase("de.dagere.pass.TestC", "testC", "testModuleC"));

      TraceChangeHandler.addAddedTests(versionInfo, test);
   }
}
