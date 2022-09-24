package de.dagere.peass.dependency.reader;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.DependencyManager;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.testData.TestClazzCall;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.folders.PeassFolders;

public class TestTraceChangeHandler {

   @Test
   public void testAdding() {
      ExecutionConfig emptyConfig = new ExecutionConfig();

      CommitStaticSelection versionInfo = new CommitStaticSelection();
      ChangedEntity onlyChange = new ChangedEntity("de.dagere.peass.TestA", "testModule", "testA");
      versionInfo.getChangedClazzes().put(onlyChange, new TestSet());

      TestSet test = getTestSet(emptyConfig, versionInfo);

      MatcherAssert.assertThat(test.getTests(), IsIterableContaining.hasItem(new TestClazzCall("de.dagere.peass.TestA", "testModule")));
   }

   @Test
   public void testAddingWithIncludes() {
      ExecutionConfig executionConfigWithBasicInclude = new ExecutionConfig();
      executionConfigWithBasicInclude.getIncludes().add("*Test*");

      CommitStaticSelection versionInfo = new CommitStaticSelection();
      ChangedEntity testA = new ChangedEntity("de.dagere.peass.TestA", "testModule", "testA");
      versionInfo.getChangedClazzes().put(testA, new TestSet());
      ChangedEntity myIntegrationTest = new ChangedEntity("de.dagere.peass.MyIntegrationT", "testModule", "testA");
      versionInfo.getChangedClazzes().put(myIntegrationTest, new TestSet());

      TestSet test = getTestSet(executionConfigWithBasicInclude, versionInfo);

      MatcherAssert.assertThat(test.getTests(), IsIterableContaining.hasItem(new TestClazzCall("de.dagere.pass.TestA", "testModule")));
      MatcherAssert.assertThat(test.getTests(), Matchers.not(IsIterableContaining.hasItem(new TestClazzCall("de.dagere.peass.MyIntegrationT", "testModule"))));
   }
   
   @Test
   public void testAddingWithMethodIncludes() {
      ExecutionConfig executionConfigWithBasicInclude = new ExecutionConfig();
      executionConfigWithBasicInclude.getIncludes().add("*MySpecialTestcase#myMethod");

      CommitStaticSelection versionInfo = new CommitStaticSelection();
      ChangedEntity testA = new ChangedEntity("de.dagere.peass.TestA", "testModule", "testA");
      versionInfo.getChangedClazzes().put(testA, new TestSet());
      ChangedEntity myIntegrationTest = new ChangedEntity("de.dagere.peass.MySpecialTestcase", "testModule", "myMethod");
      versionInfo.getChangedClazzes().put(myIntegrationTest, new TestSet());

      TestSet test = getTestSet(executionConfigWithBasicInclude, versionInfo);

      MatcherAssert.assertThat(test.getTests(), IsIterableContaining.hasItem(new TestClazzCall("de.dagere.peass.MySpecialTestcase", "testModule")));
      MatcherAssert.assertThat(test.getTests(), Matchers.not(IsIterableContaining.hasItem(new TestClazzCall("de.dagere.peass.TestA", "testModule"))));
   }

   private TestSet getTestSet(ExecutionConfig executionConfigWithBasicInclude, CommitStaticSelection versionInfo) {
      TestSet test = new TestSet();

      TraceChangeHandler handler = new TraceChangeHandler(Mockito.mock(DependencyManager.class), Mockito.mock(PeassFolders.class), executionConfigWithBasicInclude, "version1");
      handler.addAddedTests(versionInfo, test);
      return test;
   }
}
