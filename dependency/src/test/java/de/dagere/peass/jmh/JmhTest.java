package de.dagere.peass.jmh;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.github.javaparser.ParseException;

import de.dagere.peass.TestConstants;
import de.dagere.peass.TestUtil;
import de.dagere.peass.config.DependencyConfig;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.ResultsFolders;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.VersionDiff;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.InitialDependency;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependency.reader.VersionKeeper;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.dependencytests.FakeGitUtil;
import de.dagere.peass.dependencytests.TraceGettingIT;
import de.dagere.peass.dependencytests.helper.FakeFileIterator;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.GitUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest(GitUtils.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.dom.*" })
public class JmhTest {
   
   @Before
   public void clearCurrent() throws IOException {
      TestUtil.deleteContents(TestConstants.CURRENT_FOLDER.getParentFile());
   }
   
   @Test
   public void testVersionReading() throws IOException, InterruptedException, XmlPullParserException, ParseException, ViewNotFoundException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
      FakeGitUtil.prepareGitUtils();
      FakeFileIterator iterator = mockIterator();
      
      ResultsFolders resultsFolders = new ResultsFolders(TraceGettingIT.VIEW_IT_PROJECTFOLDER, "test");
      
      DependencyConfig dependencyConfig = new DependencyConfig(1, false, true);
      
      ExecutionConfig jmhConfig = new ExecutionConfig();
      jmhConfig.setTestTransformer("de.dagere.peass.dependency.jmh.JMHTestTransformer");
      jmhConfig.setTestExecutor("de.dagere.peass.dependency.jmh.JMHTestExecutor");
      
      DependencyReader reader = new DependencyReader(dependencyConfig, new PeASSFolders(TestConstants.CURRENT_FOLDER), resultsFolders,
            "", iterator, new VersionKeeper(new File("/dev/null")), jmhConfig, new EnvironmentVariables());
      reader.readInitialVersion();
      
      Dependencies dependencies = Constants.OBJECTMAPPER.readValue(resultsFolders.getDependencyFile(), Dependencies.class);
      Map<ChangedEntity, InitialDependency> initialDependencies = dependencies.getInitialversion().getInitialDependencies();
      Assert.assertThat(initialDependencies.keySet(), Matchers.hasSize(1));
      InitialDependency initial = initialDependencies.get(new ChangedEntity("de.dagere.peass.ExampleBenchmark", null, "testMethod"));
      Assert.assertThat(initial.getEntities(), Matchers.hasSize(2));
      
   }
   
   private FakeFileIterator mockIterator() {
      List<File> versionList = Arrays.asList(JmhTestConstants.BASIC_VERSION, JmhTestConstants.SLOWER_VERSION);

      FakeFileIterator fakeIterator = new FakeFileIterator(TestConstants.CURRENT_FOLDER, versionList);
      fakeIterator.goToFirstCommit();
      FakeFileIterator iteratorspied = Mockito.spy(fakeIterator);
      VersionDiff fakedDiff = new VersionDiff(Arrays.asList(TestConstants.CURRENT_FOLDER), TestConstants.CURRENT_FOLDER);
      fakedDiff.addChange("src/test/java/de/dagere/peass/ExampleBenchmark.java");

      Mockito.doReturn(fakedDiff)
            .when(iteratorspied)
            .getChangedClasses(Mockito.any(), Mockito.any(), Mockito.any());
      return iteratorspied;
   }
}
