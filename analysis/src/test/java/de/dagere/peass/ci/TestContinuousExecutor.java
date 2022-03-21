package de.dagere.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.TestUtil;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.dependencytests.DependencyTestConstants;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.GitCommit;
import de.dagere.peass.vcs.ProjectBuilderHelper;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class TestContinuousExecutor {

   private static final String NEWER_VERSION = "a23e385264c31def8dcda86c3cf64faa698c62d8";
   private static final File fullPeassFolder = new File(DependencyTestConstants.CURRENT.getParentFile(), DependencyTestConstants.CURRENT.getName() + "_fullPeass");

   @Before
   public void clearFolders() {
      if (!DependencyTestConstants.CURRENT.exists()) {
         DependencyTestConstants.CURRENT.mkdirs();
      } else {
         TestUtil.deleteContents(DependencyTestConstants.CURRENT);
      }
      if (fullPeassFolder.exists()) {
         TestUtil.deleteContents(fullPeassFolder);
      }
   }
   
   
   @Test
   public void testChangeIdentification() throws Exception {
      initRepo();
      
      TestSelectionConfig dependencyConfig = new TestSelectionConfig(1, false);
      MeasurementConfig measurementConfig = new MeasurementConfig(2);
      ContinuousExecutor executor = new ContinuousExecutor(DependencyTestConstants.CURRENT, measurementConfig, dependencyConfig, new EnvironmentVariables());

      ContinuousExecutor spied = Mockito.spy(executor);
      mockRegressionTestSelection(spied);
      mockMeasurement(executor, spied);

      spied.execute();
      
      checkChangesJson();
   }


   private void checkChangesJson() throws IOException, JsonParseException, JsonMappingException {
      File changeFile = new File(fullPeassFolder, "changes.json");
      ProjectChanges changes = Constants.OBJECTMAPPER.readValue(changeFile, ProjectChanges.class);
      
      String changedTestClass = changes.getVersion(NEWER_VERSION).getTestcaseChanges().keySet().iterator().next();
      TestCase tc = new TestCase(changedTestClass);
      Assert.assertEquals("de.test.CalleeTest", tc.getClazz());
   }


   private void mockMeasurement(final ContinuousExecutor executor, final ContinuousExecutor spied) throws IOException, InterruptedException, JAXBException, XmlPullParserException {
      Mockito.doAnswer(new Answer<File>() {

         @Override
         public File answer(final InvocationOnMock invocation) throws Throwable {

            File measurementFolder = new File(fullPeassFolder, executor.getLatestVersion() + "_" + executor.getVersionOld());
            measurementFolder.mkdirs();
            
            File measurementRawData = new File("src/test/resources/continuousExecutorTest");
            FileUtils.copyDirectory(measurementRawData, measurementFolder);
            
            return new File(measurementFolder, "measurements");
         }
      }).when(spied).executeMeasurement(Mockito.anySet());
   }


   private void mockRegressionTestSelection(final ContinuousExecutor spied) throws Exception {
      HashSet<TestCase> tests = new HashSet<TestCase>();
      tests.add(new TestCase("defaultpackage.TestMe#testMe"));
      RTSResult mockedResult = new RTSResult(tests, true);
      Mockito.doAnswer(new Answer<RTSResult>() {

         @Override
         public RTSResult answer(InvocationOnMock invocation) throws Throwable {
            File staticTestSelectionFile = new File(fullPeassFolder, "staticTestSelection_current.json");
            StaticTestSelection staticSelection = new StaticTestSelection();
            staticSelection.getInitialversion().setVersion("33ce17c04b5218c25c40137d4d09f40fbb3e4f0f");
            staticSelection.getVersions().put(NEWER_VERSION, new VersionStaticSelection());
            Constants.OBJECTMAPPER.writeValue(staticTestSelectionFile, staticSelection);
            return mockedResult;
         }
      }).when(spied).executeRegressionTestSelection(Mockito.anyString());
      
//      Mockito.doReturn(mockedResult).when(spied).executeRegressionTestSelection(Mockito.anyString());
//      Mockito.when(spied.executeRegressionTestSelection(Mockito.anyString())).thenReturn(mockedResult);
   }


   private void initRepo() throws ZipException {
      ZipFile file = new ZipFile(new File("src/test/resources/simple-test-1.zip"));
      file.extractAll(DependencyTestConstants.CURRENT.getAbsolutePath());
      VersionComparator.setVersions(Arrays.asList(new GitCommit[] {
            new GitCommit("33ce17c04b5218c25c40137d4d09f40fbb3e4f0f", null, null, null),
            new GitCommit(NEWER_VERSION, null, null, null)}));
   }

   public void buildRepo() throws InterruptedException, IOException {
      TestUtil.deleteContents(DependencyTestConstants.CURRENT);
      ProjectBuilderHelper.init(DependencyTestConstants.CURRENT);
      FileUtils.copyDirectory(DependencyTestConstants.BASIC_STATE, DependencyTestConstants.CURRENT);
      ProjectBuilderHelper.commit(DependencyTestConstants.CURRENT, "Version 0");
      FileUtils.copyDirectory(DependencyTestConstants.NORMAL_CHANGE, DependencyTestConstants.CURRENT);
      ProjectBuilderHelper.commit(DependencyTestConstants.CURRENT, "Version 1");
   }

   
}
