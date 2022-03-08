package de.dagere.peass.jmh;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.javaparser.ParseException;

import de.dagere.peass.TestConstants;
import de.dagere.peass.TestUtil;
import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.VersionDiff;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.InitialDependency;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependency.reader.VersionKeeper;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.dependencytests.FakeGitUtil;
import de.dagere.peass.dependencytests.TraceGettingIT;
import de.dagere.peass.dependencytests.helper.FakeFileIterator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.GitUtils;

public class JmhDependencyReaderTest {

   @BeforeEach
   public void clearCurrent() throws IOException {
      TestUtil.deleteContents(TestConstants.CURRENT_FOLDER);
   }

   static class KiekerConfigurationProvider implements ArgumentsProvider {
      @Override
      public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
         KiekerConfig regularConfiguration = new KiekerConfig(true);
         KiekerConfig aspectJConfiguration = new KiekerConfig(true);
         aspectJConfiguration.setUseSourceInstrumentation(false);
         return Stream.of(
               Arguments.of(regularConfiguration),
               Arguments.of(aspectJConfiguration));
      }
   }

   @ParameterizedTest
   @ArgumentsSource(KiekerConfigurationProvider.class)
   public void testVersionReading(final KiekerConfig kiekerConfig)
         throws IOException, InterruptedException, XmlPullParserException, ParseException, ViewNotFoundException, ClassNotFoundException,
         InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
      try (MockedStatic<GitUtils> gitUtilsMock = Mockito.mockStatic(GitUtils.class)) {
         FakeGitUtil.prepareGitUtils(gitUtilsMock);
         FakeFileIterator iterator = mockIterator();

         ResultsFolders resultsFolders = new ResultsFolders(TraceGettingIT.VIEW_IT_PROJECTFOLDER, "test");

         TestSelectionConfig dependencyConfig = new TestSelectionConfig(1, false, true, false);

         ExecutionConfig jmhConfig = new ExecutionConfig();
         jmhConfig.setTestTransformer("de.dagere.peass.dependency.jmh.JmhTestTransformer");
         jmhConfig.setTestExecutor("de.dagere.peass.dependency.jmh.JmhTestExecutor");

         DependencyReader reader = new DependencyReader(dependencyConfig, new PeassFolders(TestConstants.CURRENT_FOLDER), resultsFolders,
               "", iterator, new VersionKeeper(new File("/dev/null")), jmhConfig, kiekerConfig, new EnvironmentVariables());
         reader.readInitialVersion();

         checkInitialVersion(resultsFolders);

         reader.readDependencies();

         checkChangedVersion(resultsFolders);
      }
   }

   private void checkChangedVersion(final ResultsFolders resultsFolders) throws IOException, JsonParseException, JsonMappingException {
      ExecutionData data = Constants.OBJECTMAPPER.readValue(resultsFolders.getTraceTestSelectionFile(), ExecutionData.class);
      TestCase changedBenchmark = new TestCase("de.dagere.peass.ExampleBenchmark#testMethod");
      MatcherAssert.assertThat(data.getVersions().get("000002").getTests(), Matchers.contains(changedBenchmark));
   }

   private void checkInitialVersion(final ResultsFolders resultsFolders) throws IOException, JsonParseException, JsonMappingException {
      StaticTestSelection dependencies = Constants.OBJECTMAPPER.readValue(resultsFolders.getStaticTestSelectionFile(), StaticTestSelection.class);
      Map<TestCase, InitialDependency> initialDependencies = dependencies.getInitialversion().getInitialDependencies();
      MatcherAssert.assertThat(initialDependencies.keySet(), Matchers.hasSize(1));
      InitialDependency initial = initialDependencies.get(new TestCase("de.dagere.peass.ExampleBenchmark", "testMethod", null));
      MatcherAssert.assertThat(initial.getEntities(), Matchers.hasSize(2));
   }

   private FakeFileIterator mockIterator() {
      List<File> versionList = Arrays.asList(JmhTestConstants.BASIC_VERSION, JmhTestConstants.SLOWER_VERSION);

      FakeFileIterator fakeIterator = new FakeFileIterator(TestConstants.CURRENT_FOLDER, versionList);
      fakeIterator.goToFirstCommit();
      FakeFileIterator iteratorspied = Mockito.spy(fakeIterator);
      VersionDiff fakedDiff = new VersionDiff(Arrays.asList(TestConstants.CURRENT_FOLDER), TestConstants.CURRENT_FOLDER);
      ExecutionConfig defaultConfig = new ExecutionConfig();
      fakedDiff.addChange("src/test/java/de/dagere/peass/ExampleBenchmark.java", defaultConfig);

      Mockito.doReturn(fakedDiff)
            .when(iteratorspied)
            .getChangedClasses(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
      return iteratorspied;
   }
}
