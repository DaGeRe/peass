package de.dagere.peass.jmh;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.javaparser.ParseException;

import de.dagere.peass.TestConstants;
import de.dagere.peass.TestUtil;
import de.dagere.peass.config.DependencyConfig;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfiguration;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.ResultsFolders;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.data.VersionDiff;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.InitialDependency;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependency.reader.VersionKeeper;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.dependencytests.FakeGitUtil;
import de.dagere.peass.dependencytests.TraceGettingIT;
import de.dagere.peass.dependencytests.helper.FakeFileIterator;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.GitUtils;

public class JmhDependencyReaderMultiParamTest {

   @Before
   public void clearCurrent() throws IOException {
      TestUtil.deleteContents(TestConstants.CURRENT_FOLDER.getParentFile());
   }

   @Test
   public void testVersionReading() throws IOException, InterruptedException, XmlPullParserException, ParseException, ViewNotFoundException, ClassNotFoundException,
         InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
      try (MockedStatic<GitUtils> gitUtilsMock = Mockito.mockStatic(GitUtils.class)) {
         FakeGitUtil.prepareGitUtils(gitUtilsMock);
         FakeFileIterator iterator = mockIterator();

         ResultsFolders resultsFolders = new ResultsFolders(TraceGettingIT.VIEW_IT_PROJECTFOLDER, "test");

         DependencyConfig dependencyConfig = new DependencyConfig(1, false, true, false);

         ExecutionConfig jmhConfig = new ExecutionConfig();
         jmhConfig.setTestTransformer("de.dagere.peass.dependency.jmh.JmhTestTransformer");
         jmhConfig.setTestExecutor("de.dagere.peass.dependency.jmh.JmhTestExecutor");

         DependencyReader reader = new DependencyReader(dependencyConfig, new PeassFolders(TestConstants.CURRENT_FOLDER), resultsFolders,
               "", iterator, new VersionKeeper(new File("/dev/null")), jmhConfig, new KiekerConfiguration(true), new EnvironmentVariables());
         reader.readInitialVersion();

         checkInitialVersion(resultsFolders);

         reader.readDependencies();

         checkChangedVersion(resultsFolders);
      }
   }

   private void checkChangedVersion(final ResultsFolders resultsFolders) throws IOException, JsonParseException, JsonMappingException {
      ExecutionData data = Constants.OBJECTMAPPER.readValue(resultsFolders.getExecutionFile(), ExecutionData.class);
      TestCase changedBenchmark = new TestCase("de.dagere.peass.ExampleBenchmark#testMethod");
      TestSet versionTestSet = data.getVersions().get("000002");
      Assert.assertThat(versionTestSet.getTests(), Matchers.contains(changedBenchmark));
   }

   private void checkInitialVersion(final ResultsFolders resultsFolders) throws IOException, JsonParseException, JsonMappingException {
      Dependencies dependencies = Constants.OBJECTMAPPER.readValue(resultsFolders.getDependencyFile(), Dependencies.class);
      Map<ChangedEntity, InitialDependency> initialDependencies = dependencies.getInitialversion().getInitialDependencies();
      Assert.assertThat(initialDependencies.keySet(), Matchers.hasSize(1));
      InitialDependency initial = initialDependencies.get(new ChangedEntity("de.dagere.peass.ExampleBenchmark", null, "testMethod"));
      Assert.assertThat(initial.getEntities(), Matchers.hasSize(4));

      TestCase changedBenchmark = new TestCase("de.dagere.peass.ExampleBenchmark#testMethod");
      File viewFolder = resultsFolders.getViewMethodDir("000001", changedBenchmark);
      File methodOrderFile = new File(viewFolder, "000001_method");
      String allMethods = FileUtils.readFileToString(methodOrderFile, StandardCharsets.UTF_8);
      Assert.assertThat(allMethods, Matchers.containsString("de.dagere.peass.ExampleBenchmark#someCalledMethod"));
      Assert.assertThat(allMethods, Matchers.containsString("de.dagere.peass.ExampleBenchmark#otherCalledMethod"));
   }

   private FakeFileIterator mockIterator() {
      List<File> versionList = Arrays.asList(JmhTestConstants.MULTIPARAM_VERSION, JmhTestConstants.MULTIPARAM_VERSION_CHANGE);

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
