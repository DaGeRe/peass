package de.dagere.peass.testtransformation;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.RunnableTestInformation;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestSet;

public class TestRemovedMethodBuilding {

   private static final File RESOURCE_FOLDER = new File("src/test/resources/transformation/removed");

   @TempDir
   public static File testFolder;

   @Test
   public void testJUnit5MethodRemoved() throws IOException {
      File sourcesFolder = TestIgnoredMethodBuilding.initializeProject(testFolder);

      final File old2 = new File(RESOURCE_FOLDER, "TestMe_Removed.java");
      File testFile = new File(sourcesFolder, "TestMe.java");
      FileUtils.copyFile(old2, testFile);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder, MeasurementConfig.DEFAULT);

      ModuleClassMapping mapping = Mockito.mock(ModuleClassMapping.class);
      Mockito.when(mapping.getModules()).thenReturn(Arrays.asList(new File[] { testFolder }));

      tt.determineVersions(mapping.getModules());

      TestSet testsToUpdate = new TestSet();
      TestMethodCall normalTestcase = new TestMethodCall("TestMe", "testMethodNormal");
      testsToUpdate.addTest(normalTestcase);
      TestMethodCall removedTestcase = new TestMethodCall("TestMe", "testMethodRemoved");
      testsToUpdate.addTest(removedTestcase);
      TestMethodCall ignoredCall = new TestMethodCall("TestMe", "testMethodIgnored");
      testsToUpdate.addTest(ignoredCall);
      RunnableTestInformation tests = tt.buildTestMethodSet(testsToUpdate, mapping);

      System.out.println(tests.getTestsToUpdate());
      System.out.println(tests.getRemovedTests());

      MatcherAssert.assertThat(tests.getTestsToUpdate().getTestMethods(), IsIterableContaining.hasItem(normalTestcase));
      MatcherAssert.assertThat(tests.getTestsToUpdate().getTestMethods(), IsIterableContaining.hasItem(removedTestcase));
      MatcherAssert.assertThat(tests.getTestsToUpdate().getTestMethods(), IsIterableContaining.hasItem(ignoredCall));
      MatcherAssert.assertThat(tests.getRemovedTests().getTestMethods(), IsIterableContaining.hasItem(removedTestcase));
      MatcherAssert.assertThat(tests.getRemovedTests().getTestMethods(), Matchers.not(IsIterableContaining.hasItem(normalTestcase)));
      MatcherAssert.assertThat(tests.getRemovedTests().getTestMethods(), Matchers.not(IsIterableContaining.hasItem(ignoredCall)));
   }
}
