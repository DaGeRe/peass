package de.dagere.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

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

import de.dagere.nodeDiffDetector.data.TestCase;
import de.dagere.nodeDiffDetector.data.TestClazzCall;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.TestUtil;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.dependencytests.DependencyTestConstants;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.ProjectBuilderHelper;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class TestContinuousExecutor {

   private static final String COMMIT_OLD = "33ce17c04b5218c25c40137d4d09f40fbb3e4f0f";
   private static final String COMMIT_CURRENT = "a23e385264c31def8dcda86c3cf64faa698c62d8";
   private static final File fullPeassFolder = new File(DependencyTestConstants.CURRENT.getParentFile(), DependencyTestConstants.CURRENT.getName() + PeassFolders.PEASS_FULL_POSTFIX);

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
      MeasurementConfig measurementConfig = new MeasurementConfig(4);
      ContinuousExecutor executor = new ContinuousExecutor(DependencyTestConstants.CURRENT, measurementConfig, dependencyConfig, new EnvironmentVariables());

      Assert.assertEquals(COMMIT_CURRENT, executor.getIterator().getCommitName());
      
      ContinuousExecutor spied = Mockito.spy(executor);
      mockRegressionTestSelection(spied);
      mockMeasurement(executor, spied);

      spied.execute();

      checkChangesJson();
   }

   private void checkChangesJson() throws IOException, JsonParseException, JsonMappingException {
      File changeFile = new File(fullPeassFolder, "changes.json");
      ProjectChanges changes = Constants.OBJECTMAPPER.readValue(changeFile, ProjectChanges.class);

      String changedTestClass = changes.getCommitChanges(COMMIT_CURRENT).getTestcaseChanges().keySet().iterator().next();
      TestCase tc = new TestClazzCall(changedTestClass);
      Assert.assertEquals("de.test.CalleeTest", tc.getClazz());
   }

   private void mockMeasurement(final ContinuousExecutor executor, final ContinuousExecutor spied) throws IOException, InterruptedException, XmlPullParserException {
      Mockito.doAnswer(new Answer<File>() {

         @Override
         public File answer(final InvocationOnMock invocation) throws Throwable {

            File measurementFolder = new File(fullPeassFolder, executor.getLatestCommit() + "_" + executor.getCommitOld());
            measurementFolder.mkdirs();

            File measurementRawData = new File("src/test/resources/continuousExecutorTest");
            FileUtils.copyDirectory(measurementRawData, measurementFolder);

            return new File(measurementFolder, "measurements");
         }
      }).when(spied).executeMeasurement(Mockito.anySet());
   }

   private void mockRegressionTestSelection(final ContinuousExecutor spied) throws Exception {
      HashSet<TestMethodCall> tests = new HashSet<TestMethodCall>();
      tests.add(new TestMethodCall("defaultpackage.TestMe", "testMe"));
      RTSResult mockedResult = new RTSResult(tests, true);
      Mockito.doAnswer(new Answer<RTSResult>() {

         @Override
         public RTSResult answer(InvocationOnMock invocation) throws Throwable {
            File staticTestSelectionFile = new File(fullPeassFolder, "staticTestSelection_current.json");
            StaticTestSelection staticSelection = new StaticTestSelection();
            staticSelection.getInitialcommit().setCommit(COMMIT_OLD);
            staticSelection.getCommits().put(COMMIT_CURRENT, new CommitStaticSelection());
            Constants.OBJECTMAPPER.writeValue(staticTestSelectionFile, staticSelection);
            return mockedResult;
         }
      }).when(spied).executeRegressionTestSelection(Mockito.anyString());

      // Mockito.doReturn(mockedResult).when(spied).executeRegressionTestSelection(Mockito.anyString());
      // Mockito.when(spied.executeRegressionTestSelection(Mockito.anyString())).thenReturn(mockedResult);
   }

   private void initRepo() throws ZipException {
      ZipFile file = new ZipFile(new File("src/test/resources/simple-test-1.zip"));
      file.extractAll(DependencyTestConstants.CURRENT.getAbsolutePath());
      VersionComparator.setVersions(Arrays.asList(new String[] { COMMIT_OLD, COMMIT_CURRENT }));
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
