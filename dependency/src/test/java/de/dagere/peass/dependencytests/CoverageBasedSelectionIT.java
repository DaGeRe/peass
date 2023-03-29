package de.dagere.peass.dependencytests;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsIterableContaining;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.github.javaparser.ParseException;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependencytests.helper.FakeFileIterator;
import de.dagere.peass.vcs.CommitIterator;
import de.dagere.peass.vcs.GitUtils;

public class CoverageBasedSelectionIT {

   @BeforeEach
   public void initialize() throws IOException, InterruptedException {
      Assert.assertTrue(DependencyTestConstants.COVERAGE_VERSIONS_FOLDER.exists());

      FileUtils.deleteDirectory(DependencyTestConstants.CURRENT);
      FileUtils.copyDirectory(DependencyTestConstants.COVERAGE_BASIC_STATE, DependencyTestConstants.CURRENT);
   }

   @Test
   public void testNormalChange() throws IOException, ParseException {
      try (MockedStatic<GitUtils> staticMock = Mockito.mockStatic(GitUtils.class)) {
         final ChangeManager changeManager = DependencyDetectorTestUtil.defaultChangeManager();

         final CommitIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(DependencyTestConstants.COVERAGE_NORMAL_CHANGE));

         final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator, new ExecutionConfig(5),
               DependencyTestConstants.DEFAULT_CONFIG_WITH_COVERAGE, DependencyTestConstants.TARGET_RESULTS_FOLDERS);

         System.out.println(reader.getDependencies());

         DependencyDetectorTestUtil.checkChange(reader, "defaultpackage.NormalDependency#executeThing", "defaultpackage.TestMe", DependencyTestConstants.VERSION_1, "testFirst");

         System.out.println(reader.getCoverageBasedSelection());

         TestSet tests = reader.getCoverageBasedSelection().getCommits().get(DependencyTestConstants.VERSION_1);
         MatcherAssert.assertThat(tests.getTestMethods(), IsIterableContaining.hasItem(new TestMethodCall("defaultpackage.TestMe", "testSecond")));
      }
   }

}
