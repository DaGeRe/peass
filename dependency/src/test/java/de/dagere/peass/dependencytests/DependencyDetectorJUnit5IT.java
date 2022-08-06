package de.dagere.peass.dependencytests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.javaparser.ParseException;

import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependencytests.helper.FakeFileIterator;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.CommitIterator;

public class DependencyDetectorJUnit5IT {

   public static final File CONTAINING_FOLDER = new File(DependencyTestConstants.VERSIONS_FOLDER, "JUnit5");
   public static final File BASIC_STATE = new File(CONTAINING_FOLDER, "basic_state");
   public static final File NORMAL_CHANGE = new File(CONTAINING_FOLDER, "normal_change");
   
   @BeforeEach
   public void initialize() throws IOException, InterruptedException {
      Assert.assertTrue(DependencyTestConstants.VERSIONS_FOLDER.exists());

      FileUtils.deleteDirectory(DependencyTestConstants.CURRENT);
      FileUtils.copyDirectory(BASIC_STATE, DependencyTestConstants.CURRENT);
      
   }

   @Test
   public void testNormalChange() throws IOException, InterruptedException, XmlPullParserException, ParseException {
      final ChangeManager changeManager = DependencyDetectorTestUtil.defaultChangeManager();

      final CommitIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(NORMAL_CHANGE));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      System.out.println(Constants.OBJECTMAPPER.writeValueAsString(reader.getDependencies()));

      DependencyDetectorTestUtil.checkTestMeAlsoTestChange(reader, "defaultpackage.NormalDependency#executeThing", "defaultpackage.TestMe", DependencyTestConstants.VERSION_1);
   }
}
