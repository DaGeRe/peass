package de.dagere.peass.measurement.rca.strategytest;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsIterableContaining;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.analysis.properties.ChangedMethodManager;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.CauseSearcherConfigMixin;
import de.dagere.peass.measurement.rca.analyzer.SourceChangeTreeAnalyzer;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.kieker.BothTreeReader;
import kieker.analysis.exception.AnalysisConfigurationException;

public class TestSourceChangeTreeAnalyzer {
   @Test
   public void testNodeSelection() throws InterruptedException, IOException, XmlPullParserException, ViewNotFoundException, AnalysisConfigurationException {
      final MeasurementConfig config = new MeasurementConfig(15, "fd2c8ddf3fa52973ee54c4db87b47bb587886200", "fd2c8ddf3fa52973ee54c4db87b47bb587886200~1");
      BothTreeReader treeReader = new BothTreeReader(new CauseSearcherConfig(new TestMethodCall("de.peass.MainTest", "testMe"), new CauseSearcherConfigMixin()),
            config,
            new CauseSearchFolders(new File("src/test/resources/sourceChangeRCATest/project_3")),
            new EnvironmentVariables());
      treeReader.readTrees();

      File methodSourceFolder = new File("src/test/resources/sourceChangeRCATest/properties_project_3", "methods");
      ChangedMethodManager manager = new ChangedMethodManager(methodSourceFolder);
      SourceChangeTreeAnalyzer analyzer = new SourceChangeTreeAnalyzer(treeReader.getRootVersion(), treeReader.getRootPredecessor(), manager, config);

      List<String> instrumentedCalls = analyzer.getMeasurementNodesPredecessor().stream().map(node -> node.getCall()).collect(Collectors.toList());
      MatcherAssert.assertThat(instrumentedCalls, IsIterableContaining.hasItem("de.peass.MainTest#testMe"));
      MatcherAssert.assertThat(instrumentedCalls, IsIterableContaining.hasItem("de.peass.C0_0#method0"));
      MatcherAssert.assertThat(instrumentedCalls, IsIterableContaining.hasItem("de.peass.C1_0#method0"));

      MatcherAssert.assertThat(instrumentedCalls, Matchers.not(IsIterableContaining.hasItem("de.peass.AddRandomNumbers#addSomething")));
      MatcherAssert.assertThat(instrumentedCalls, Matchers.not(IsIterableContaining.hasItem("de.peass.C1_0#method1")));
   }

   @Test
   public void testNodeSelectionOnlyStructureChange() {
      final MeasurementConfig config = new MeasurementConfig(15, "fd2c8ddf3fa52973ee54c4db87b47bb587886200", "fd2c8ddf3fa52973ee54c4db87b47bb587886200~1");

      CallTreeNode firstRoot = new CallTreeNode("de.mypackage.MyClazz#testA", "de.mypackage.MyClazz.testA()", "de.mypackage.MyClazz.testA()", config);
      CallTreeNode secondRoot = new CallTreeNode("de.mypackage.MyClazz#testA", "de.mypackage.MyClazz.testA()", "de.mypackage.MyClazz.testA()", config);

      firstRoot.appendChild("de.mypackage.Callee1#methodA", "de.mypackage.Callee1.methodA()", "de.mypackage.Callee1.methodA()");
      secondRoot.appendChild("de.mypackage.Callee2#methodA", "de.mypackage.Callee2.methodA()", "de.mypackage.Callee2.methodA()");

      ChangedMethodManager mockedChangeManager = mockChangeManager(firstRoot);

      SourceChangeTreeAnalyzer analyzer = new SourceChangeTreeAnalyzer(firstRoot, secondRoot, mockedChangeManager, config);

      Assert.assertEquals(2, analyzer.getMeasurementNodesPredecessor().size());

   }

   private ChangedMethodManager mockChangeManager(CallTreeNode firstRoot) {
      ChangedMethodManager mockedChangeManager = Mockito.mock(ChangedMethodManager.class);
      File unexistingFileMock = Mockito.mock(File.class);
      Mockito.when(unexistingFileMock.exists()).thenReturn(false);
      File existingFileMock = Mockito.mock(File.class);
      Mockito.when(existingFileMock.exists()).thenReturn(true);
      Mockito.when(mockedChangeManager.getMethodMainFile("fd2c8ddf3fa52973ee54c4db87b47bb587886200", firstRoot.toEntity())).thenReturn(unexistingFileMock);
      Mockito.when(mockedChangeManager.getMethodOldFile("fd2c8ddf3fa52973ee54c4db87b47bb587886200", firstRoot.toEntity())).thenReturn(unexistingFileMock);
      Mockito.when(mockedChangeManager.getMethodDiffFile("fd2c8ddf3fa52973ee54c4db87b47bb587886200", firstRoot.toEntity())).thenReturn(existingFileMock);
      return mockedChangeManager;
   }
}
