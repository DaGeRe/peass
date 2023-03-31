package de.dagere.peass.dependencytests;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.javaparser.ParseException;

import de.dagere.nodeDiffDetector.data.MethodCall;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.nodeDiffDetector.data.Type;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.changesreading.ClazzChangeData;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependencytests.helper.FakeFileIterator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.vcs.CommitIterator;

public class SuperclassDetectorIT {

   private static final Logger LOG = LogManager.getLogger(SuperclassDetectorIT.class);

   private static final File VERSIONS_FOLDER = new File("src/test/resources/dependencyIT");
   private static final File BASIC_STATE = new File(VERSIONS_FOLDER, "superclass_old");

   @BeforeEach
   public void initialize() throws IOException, InterruptedException {
      Assert.assertTrue(VERSIONS_FOLDER.exists());

      FileUtils.deleteDirectory(DependencyTestConstants.CURRENT);
      FileUtils.copyDirectory(BASIC_STATE, DependencyTestConstants.CURRENT);
   }

   @Test
   public void testSuperclassChange() throws IOException, ParseException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
      final File secondVersion = new File(VERSIONS_FOLDER, "superclass_changed");

      final Map<Type, ClazzChangeData> changes = new TreeMap<>();
      changes.put(new Type("defaultpackage.NormalSuperclass", ""), new ClazzChangeData("defaultpackage.NormalSuperclass", false));

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final CommitIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = new DependencyReader(DependencyTestConstants.DEFAULT_CONFIG_NO_VIEWS, new PeassFolders(DependencyTestConstants.CURRENT), DependencyTestConstants.NULL_RESULTS_FOLDERS, null, fakeIterator, changeManager,
            new ExecutionConfig(5),new KiekerConfig(true), new EnvironmentVariables());
      reader.readInitialCommit();

      checkInitialDependencies(reader);

      fakeIterator.goToNextCommit();
      reader.analyseCommit(changeManager);

      checkFirstVersionDependencies(reader);
   }

   private void checkFirstVersionDependencies(final DependencyReader reader) {
      System.out.println(reader.getDependencies());

      final TestSet testMe = DependencyDetectorTestUtil.findDependency(reader.getDependencies(), "defaultpackage.NormalSuperclass", DependencyTestConstants.VERSION_1);
      final TestMethodCall testcase = testMe.getTestMethods().iterator().next();
      Assert.assertEquals("defaultpackage.TestMe", testcase.getClazz());
      Assert.assertEquals("testMe", testcase.getMethod());
   }

   private void checkInitialDependencies(final DependencyReader reader) {
      List<MethodCall> initialDependencies = reader.getDependencies().getInitialcommit().getInitialDependencies().values().iterator().next().getEntities();
      LOG.debug("Initial Dependencies: " + initialDependencies);
      MatcherAssert.assertThat(initialDependencies, Matchers.anyOf(Matchers.hasSize(4), Matchers.hasSize(5)));
   }

}
