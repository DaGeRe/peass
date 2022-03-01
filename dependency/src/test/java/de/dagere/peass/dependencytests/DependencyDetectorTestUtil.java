package de.dagere.peass.dependencytests;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.mockito.Mockito;

import com.github.javaparser.ParseException;

import de.dagere.peass.TestConstants;
import de.dagere.peass.config.DependencyConfig;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.changesreading.ClazzChangeData;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.vcs.VersionIterator;

public class DependencyDetectorTestUtil {

   private static final Logger LOG = LogManager.getLogger(DependencyDetectorTestUtil.class);

   public static TestSet findDependency(final Dependencies dependencies, final String changedClass, final String version) {
      final VersionStaticSelection versionDependencies = dependencies.getVersions().get(version);
      System.out.println(dependencies.getVersions().keySet());
      Assert.assertNotNull("Searching for " + changedClass + " in " + version, versionDependencies);

      TestSet testcase = null;
      for (final Entry<ChangedEntity, TestSet> candidate : versionDependencies.getChangedClazzes().entrySet()) {
         final String changeclassInDependencies = candidate.getKey().toString();
         if (changeclassInDependencies.equals(changedClass)) {
            testcase = candidate.getValue();
         }
      }
      return testcase;
   }

   public static Map<ChangedEntity, ClazzChangeData> buildChanges(final String module, final String clazz, final String method) {
      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      addChange(changes, module, clazz, method);
      return changes;
   }

   public static void addChange(final Map<ChangedEntity, ClazzChangeData> changes, final String module, final String clazz, final String method) {
      final ChangedEntity baseChangedClazz = new ChangedEntity(clazz, module);
      final ClazzChangeData methodChanges = new ClazzChangeData(baseChangedClazz);
      methodChanges.addChange(clazz.substring(clazz.lastIndexOf('.') + 1), method);
      changes.put(baseChangedClazz, methodChanges);
   }

   public static ChangeManager defaultChangeManager() {
      final Map<ChangedEntity, ClazzChangeData> changes = DependencyDetectorTestUtil.buildChanges("", "defaultpackage.NormalDependency", "executeThing");

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);
      return changeManager;
   }
   
   public static ChangeManager changedTestClassChangeManager() {
      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      changes.put(new ChangedEntity("defaultpackage.TestMe", ""), new ClazzChangeData("defaultpackage.TestMe", false));

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);
      return changeManager;
   }

   public static ChangeManager mockAddedChangeManager() {
      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      changes.put(new ChangedEntity("defaultpackage.NormalDependency", ""), new ClazzChangeData("defaultpackage.NormalDependency", false));
      changes.put(new ChangedEntity("defaultpackage.TestMeAlso", ""), new ClazzChangeData("defaultpackage.TestMeAlso", false));

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);
      return changeManager;
   }

   public static void checkTestMeAlsoTestChange(final DependencyReader reader, final String change, final String changedTest, final String version) {
      checkChange(reader, change, changedTest, version, "testMe");
   }
   
   public static void checkChange(final DependencyReader reader, final String change, final String changedTest, final String version, final String testMethod) {
      final TestSet testMe = DependencyDetectorTestUtil.findDependency(reader.getDependencies(), change, version);
      System.out.println(testMe);
      final TestCase testcase = testMe.getTests().iterator().next();
      Assert.assertEquals(changedTest, testcase.getClazz());
      Assert.assertEquals(testMethod, testcase.getMethod());
   }

   public static DependencyReader readTwoVersions(final ChangeManager changeManager, final VersionIterator fakeIterator)
         throws IOException, InterruptedException, XmlPullParserException, ParseException, ViewNotFoundException {
      return readTwoVersions(changeManager, fakeIterator, new ExecutionConfig(5), DependencyTestConstants.DEFAULT_CONFIG_NO_VIEWS, DependencyTestConstants.TARGET_RESULTS_FOLDERS);
   }

   public static DependencyReader readTwoVersions(final ChangeManager changeManager, final VersionIterator fakeIterator, final ExecutionConfig config, final DependencyConfig dependencyConfig, final ResultsFolders resultsFolders) {
      try {
         final DependencyReader reader = new DependencyReader(dependencyConfig, new PeassFolders(DependencyTestConstants.CURRENT),
               resultsFolders, null, fakeIterator, changeManager, config, new KiekerConfig(true), new EnvironmentVariables());
         boolean success = reader.readInitialVersion();
         Assert.assertTrue(success);
         fakeIterator.goToNextCommit();

         reader.analyseVersion(changeManager);
         return reader;
      } catch (IOException | InterruptedException | XmlPullParserException | ParseException | ViewNotFoundException e) {
         throw new RuntimeException(e);
      }
   }

   public static void init(final File folder) {
      File peassDirectory = new File(TestConstants.CURRENT_FOLDER.getParentFile(), TestConstants.CURRENT_FOLDER.getName() + "_peass");
      try {
         if (TraceGettingIT.VIEW_IT_PROJECTFOLDER.exists()) {
            FileUtils.deleteDirectory(TraceGettingIT.VIEW_IT_PROJECTFOLDER);
         }
         TraceGettingIT.VIEW_IT_PROJECTFOLDER.mkdirs();
         FileUtils.deleteDirectory(TestConstants.CURRENT_FOLDER);
         FileUtils.deleteDirectory(peassDirectory);
         FileUtils.copyDirectory(folder, TestConstants.CURRENT_FOLDER);
      } catch (final IOException e) {
         e.printStackTrace();
         LOG.error("Part of the I/O-process failed; files in {}", peassDirectory.getAbsolutePath(), peassDirectory.listFiles().length);
         for (File child : peassDirectory.listFiles()) {
            LOG.error(child.getAbsolutePath());
         }
      }
   }
}
