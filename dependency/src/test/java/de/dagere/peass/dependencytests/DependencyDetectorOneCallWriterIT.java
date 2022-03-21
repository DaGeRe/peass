package de.dagere.peass.dependencytests;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.javaparser.ParseException;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.dependencytests.helper.FakeFileIterator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.vcs.VersionIterator;

public class DependencyDetectorOneCallWriterIT {

   @BeforeEach
   public void initialize() throws Exception {
      Assert.assertTrue(DependencyTestConstants.VERSIONS_FOLDER.exists());

      FileUtils.deleteDirectory(DependencyTestConstants.CURRENT);
      FileUtils.copyDirectory(DependencyTestConstants.BASIC_STATE, DependencyTestConstants.CURRENT);

   }

   @Test
   public void testNormalChange() throws Exception {

      final ChangeManager changeManager = DependencyDetectorTestUtil.defaultChangeManager();

      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(DependencyTestConstants.NORMAL_CHANGE));

      final DependencyReader reader = readTwoVersions(changeManager, fakeIterator);

      System.out.println(reader.getDependencies());

      DependencyDetectorTestUtil.checkTestMeAlsoTestChange(reader, "defaultpackage.NormalDependency#executeThing", "defaultpackage.TestMe", DependencyTestConstants.VERSION_1);
   }
   
   public static DependencyReader readTwoVersions(final ChangeManager changeManager, final VersionIterator fakeIterator)
         throws IOException, InterruptedException, XmlPullParserException, ParseException, ViewNotFoundException {
      return readTwoVersions(changeManager, fakeIterator, new ExecutionConfig(5), DependencyTestConstants.DEFAULT_CONFIG_NO_VIEWS, DependencyTestConstants.TARGET_RESULTS_FOLDERS);
   }

   public static DependencyReader readTwoVersions(final ChangeManager changeManager, final VersionIterator fakeIterator, final ExecutionConfig config, final TestSelectionConfig dependencyConfig, final ResultsFolders resultsFolders) {
      try {
         KiekerConfig kiekerConfig = new KiekerConfig(true);
         kiekerConfig.setOnlyOneCallRecording(true);
         kiekerConfig.setUseSourceInstrumentation(false);
         final DependencyReader reader = new DependencyReader(dependencyConfig, new PeassFolders(DependencyTestConstants.CURRENT),
               resultsFolders, null, fakeIterator, changeManager, config, kiekerConfig, new EnvironmentVariables());
         boolean success = reader.readInitialVersion();
         Assert.assertTrue(success);
         fakeIterator.goToNextCommit();

         reader.analyseVersion(changeManager);
         return reader;
      } catch (IOException | InterruptedException | XmlPullParserException | ParseException | ViewNotFoundException e) {
         throw new RuntimeException(e);
      }
   }
}
