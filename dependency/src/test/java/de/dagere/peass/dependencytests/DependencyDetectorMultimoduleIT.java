package de.dagere.peass.dependencytests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsIterableContaining;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.javaparser.ParseException;

import co.unruly.matchers.StreamMatchers;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.changesreading.ClazzChangeData;
import de.dagere.peass.dependency.persistence.InitialDependency;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.dependencytests.helper.FakeFileIterator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.vcs.VersionIterator;

public class DependencyDetectorMultimoduleIT {

   private static final Logger LOG = LogManager.getLogger(DependencyDetectorMultimoduleIT.class);

   private static final File VERSIONS_FOLDER = new File(DependencyTestConstants.VERSIONS_FOLDER, "multimodule");
   private static final File BASIC_STATE = new File(VERSIONS_FOLDER, "basic_state");

   // private DependencyManager handler;

   @BeforeEach
   public void initialize() throws IOException, InterruptedException {
      Assert.assertTrue(VERSIONS_FOLDER.exists());

      FileUtils.deleteDirectory(DependencyTestConstants.CURRENT);
      FileUtils.copyDirectory(BASIC_STATE, DependencyTestConstants.CURRENT);
   }


   // This test is disabled since it takes too long and nearly tests the same as testTwoChanges; however, since it enables easier debugging, it is left in the code
   @Disabled
   @Test
   public void testNormalChange() throws IOException, InterruptedException, XmlPullParserException, ParseException, ViewNotFoundException {
      final File secondVersion = new File(VERSIONS_FOLDER, "normal_change");
      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      final Map<ChangedEntity, ClazzChangeData> changes = DependencyDetectorTestUtil.buildChanges("base-module", "de.dagere.base.BaseChangeable", "doSomething");

      ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final DependencyReader reader = new DependencyReader(DependencyTestConstants.DEFAULT_CONFIG_NO_VIEWS, new PeassFolders(DependencyTestConstants.CURRENT),
            DependencyTestConstants.NULL_RESULTS_FOLDERS, null, fakeIterator, changeManager, new ExecutionConfig(5), new KiekerConfig(true), new EnvironmentVariables());

      final boolean success = reader.readInitialVersion();
      Assert.assertTrue(success);

      StaticTestSelection dependencies = reader.getDependencies();
      checkInitialVersion(dependencies);

      fakeIterator.goToNextCommit();
      reader.analyseVersion(changeManager);

      testFirstChange(dependencies);
   }

   @Test
   public void testTwoChanges()
         throws IOException, XmlPullParserException, InterruptedException, ParseException, ViewNotFoundException {
      final File thirdVersion = new File(VERSIONS_FOLDER, "another_change");
      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(thirdVersion));

      ChangeManager changeManager = mockChangeManager();
      
      final DependencyReader reader = new DependencyReader(DependencyTestConstants.DEFAULT_CONFIG_NO_VIEWS, new PeassFolders(DependencyTestConstants.CURRENT),
            DependencyTestConstants.NULL_RESULTS_FOLDERS, null, fakeIterator, changeManager, new ExecutionConfig(5), new KiekerConfig(true), new EnvironmentVariables());
      final boolean success = reader.readInitialVersion();
      Assert.assertTrue(success);

      StaticTestSelection dependencies = reader.getDependencies();
      checkInitialVersion(dependencies);
      
      fakeIterator.goToNextCommit();
      reader.analyseVersion(changeManager);

      testFirstChange(dependencies);
      testSecondChange(dependencies);
   }


   private ChangeManager mockChangeManager() {
      final Map<ChangedEntity, ClazzChangeData> changes = DependencyDetectorTestUtil.buildChanges("base-module", "de.dagere.base.BaseChangeable", "doSomething");
      DependencyDetectorTestUtil.addChange(changes, "base-module", "de.dagere.base.NextBaseChangeable", "doSomething");

      ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);
      return changeManager;
   }
   
   private void checkInitialVersion(final StaticTestSelection dependencies) {
      LOG.debug(dependencies.getInitialversion().getInitialDependencies());
      final InitialDependency dependency = dependencies.getInitialversion().getInitialDependencies()
            .get(new TestCase("de.AnotherTest", "testMeAlso", "using-module"));
      LOG.debug(dependency.getEntities());
      MatcherAssert.assertThat(dependency.getEntities(), IsIterableContaining.hasItem(new ChangedEntity("de.dagere.base.BaseChangeable", "base-module", "doSomething")));
   }

   private void testSecondChange(final StaticTestSelection dependencies) {
      final TestSet foundDependency3 = DependencyDetectorTestUtil.findDependency(dependencies, "base-module§de.dagere.base.NextBaseChangeable#doSomething",
            DependencyTestConstants.VERSION_1);
      MatcherAssert.assertThat(foundDependency3.getTests().stream(), StreamMatchers.anyMatch(
            Matchers.allOf(Matchers.hasProperty("clazz", Matchers.is("de.NextTest")),
                  Matchers.hasProperty("method", Matchers.isOneOf("nextTestMe", "nextTestMeAlso")))));
   }

   private void testFirstChange(final StaticTestSelection dependencies) {
      final TestSet foundDependency2 = DependencyDetectorTestUtil.findDependency(dependencies, "base-module§de.dagere.base.BaseChangeable#doSomething",
            DependencyTestConstants.VERSION_1);
      testBaseChangeEffect(foundDependency2);
   }

   private void testBaseChangeEffect(final TestSet foundDependency) {

      System.out.println(foundDependency.getTestcases());

      MatcherAssert.assertThat(foundDependency.getTests().stream(), StreamMatchers.anyMatch(
            Matchers.allOf(Matchers.hasProperty("clazz", Matchers.is("de.dagere.base.BaseTest")),
                  Matchers.hasProperty("method", Matchers.isOneOf("testMe", "testMeAlso")))));

      MatcherAssert.assertThat(foundDependency.getTests().stream(), StreamMatchers.anyMatch(
            Matchers.allOf(Matchers.hasProperty("clazz", Matchers.is("de.NextTest")),
                  Matchers.hasProperty("method", Matchers.is("nextTestMe")))));

      MatcherAssert.assertThat(foundDependency.getTests().stream(), StreamMatchers.anyMatch(
            Matchers.allOf(Matchers.hasProperty("clazz", Matchers.is("de.AnotherTest")),
                  Matchers.hasProperty("method", Matchers.isOneOf("testMeAlso")))));
   }
}
