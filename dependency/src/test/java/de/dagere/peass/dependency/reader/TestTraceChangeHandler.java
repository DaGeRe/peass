package de.dagere.peass.dependency.reader;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.DependencyManager;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
import de.dagere.peass.folders.PeassFolders;

public class TestTraceChangeHandler {

   @Test
   public void testAdding() {
      ExecutionConfig emptyConfig = new ExecutionConfig();

      VersionStaticSelection versionInfo = new VersionStaticSelection();
      ChangedEntity onlyChange = new ChangedEntity("de.dagere.peass.TestA", "testModule", "testA");
      versionInfo.getChangedClazzes().put(onlyChange, new TestSet());

      TestSet test = getTestSet(emptyConfig, versionInfo);

      MatcherAssert.assertThat(test.getTests(), IsIterableContaining.hasItem(new TestCase("de.dagere.peass.TestA", null, "testModule")));
   }

   @Test
   public void testAddingWithIncludes() {
      ExecutionConfig executionConfigWithBasicInclude = new ExecutionConfig();
      executionConfigWithBasicInclude.getIncludes().add("*Test*");

      VersionStaticSelection versionInfo = new VersionStaticSelection();
      ChangedEntity testA = new ChangedEntity("de.dagere.peass.TestA", "testModule", "testA");
      versionInfo.getChangedClazzes().put(testA, new TestSet());
      ChangedEntity myIntegrationTest = new ChangedEntity("de.dagere.peass.MyIntegrationT", "testModule", "testA");
      versionInfo.getChangedClazzes().put(myIntegrationTest, new TestSet());

      TestSet test = getTestSet(executionConfigWithBasicInclude, versionInfo);

      MatcherAssert.assertThat(test.getTests(), IsIterableContaining.hasItem(new TestCase("de.dagere.pass.TestA", null, "testModule")));
      MatcherAssert.assertThat(test.getTests(), Matchers.not(IsIterableContaining.hasItem(new TestCase("de.dagere.peass.MyIntegrationT", null, "testModule"))));
   }
   
   @Test
   public void testAddingWithMethodIncludes() {
      ExecutionConfig executionConfigWithBasicInclude = new ExecutionConfig();
      executionConfigWithBasicInclude.getIncludes().add("*MySpecialTestcase#myMethod");

      VersionStaticSelection versionInfo = new VersionStaticSelection();
      ChangedEntity testA = new ChangedEntity("de.dagere.peass.TestA", "testModule", "testA");
      versionInfo.getChangedClazzes().put(testA, new TestSet());
      ChangedEntity myIntegrationTest = new ChangedEntity("de.dagere.peass.MySpecialTestcase", "testModule", "myMethod");
      versionInfo.getChangedClazzes().put(myIntegrationTest, new TestSet());

      TestSet test = getTestSet(executionConfigWithBasicInclude, versionInfo);

      MatcherAssert.assertThat(test.getTests(), IsIterableContaining.hasItem(new TestCase("de.dagere.peass.MySpecialTestcase", null, "testModule")));
      MatcherAssert.assertThat(test.getTests(), Matchers.not(IsIterableContaining.hasItem(new TestCase("de.dagere.peass.TestA", null, "testModule"))));
   }

   private TestSet getTestSet(ExecutionConfig executionConfigWithBasicInclude, VersionStaticSelection versionInfo) {
      TestSet test = new TestSet();

      TraceChangeHandler handler = new TraceChangeHandler(Mockito.mock(DependencyManager.class), Mockito.mock(PeassFolders.class), executionConfigWithBasicInclude, "version1");
      handler.addAddedTests(versionInfo, test);
      return test;
   }
}
