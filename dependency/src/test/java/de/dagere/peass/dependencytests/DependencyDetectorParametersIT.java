package de.dagere.peass.dependencytests;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.javaparser.ParseException;

import de.dagere.nodeDiffGenerator.data.MethodCall;
import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.changesreading.ClazzChangeData;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependencytests.helper.FakeFileIterator;
import de.dagere.peass.vcs.CommitIterator;

public class DependencyDetectorParametersIT {

   @BeforeEach
   public void initialize() throws IOException, InterruptedException {
      Assert.assertTrue(DependencyTestConstants.VERSIONS_FOLDER.exists());

      FileUtils.deleteDirectory(DependencyTestConstants.CURRENT);
      FileUtils.copyDirectory(DependencyTestConstants.BASIC_STATE_PARAMETERS, DependencyTestConstants.CURRENT);

   }

   @Test
   public void testNormalChange() throws IOException, InterruptedException, XmlPullParserException, ParseException {

      final ChangeManager changeManager = changeManagerWithParameter();

      final CommitIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(DependencyTestConstants.NORMAL_CHANGE_PARAMETERS));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      System.out.println(reader.getDependencies());
      System.out.println(reader.getDependencies().getCommits().get(DependencyTestConstants.VERSION_1));

      DependencyDetectorTestUtil.checkTestMeAlsoTestChange(reader, "defaultpackage.NormalDependency#innerMethod(java.lang.Integer)", "defaultpackage.TestMe", DependencyTestConstants.VERSION_1);
   }

   public static ChangeManager changeManagerWithParameter() {
      final Map<MethodCall, ClazzChangeData> changes = DependencyDetectorTestUtil.buildChanges("", "defaultpackage.NormalDependency", "innerMethod(java.lang.Integer)");

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);
      return changeManager;
   }
}
