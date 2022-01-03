package de.dagere.peass.measurement.rca.helper;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.ExecutorCreator;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.MavenTestExecutorMocker;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionControlSystem;

public class VCSTestUtils {

   private static final Logger LOG = LogManager.getLogger(VCSTestUtils.class);

   public static void mockGetVCS(final MockedStatic<VersionControlSystem> mockedVCS) {
      mockedVCS.when(() -> VersionControlSystem.getVersionControlSystem(Mockito.any()))
            .thenReturn(VersionControlSystem.GIT);
   }

   public static void mockGoToTagAny(final MockedStatic<GitUtils> mockedGitUtils, final File anyVersion) {
      mockedGitUtils.when(() -> GitUtils.goToTag(Mockito.any(), Mockito.any()))
            .thenAnswer(new Answer<Void>() {

               @Override
               public Void answer(final InvocationOnMock invocation) throws Throwable {
                  final File destFile = (File) invocation.getArgument(1);
                  LOG.debug("Loading version..");
                  FileUtils.deleteDirectory(destFile);

                  FileUtils.copyDirectory(anyVersion, destFile);
                  return null;
               }
            });
   }

   public static void mockClone(final MockedStatic<GitUtils> mockedGitUtils, final File projectFolderTemp, final File clonedDir) throws InterruptedException, IOException {
      mockedGitUtils.when(() -> GitUtils.clone(Mockito.any(PeassFolders.class), Mockito.any(File.class)))
            .thenAnswer(new Answer<Void>() {

               @Override
               public Void answer(final InvocationOnMock invocation) throws Throwable {
                  FileUtils.copyDirectory(clonedDir, projectFolderTemp);
                  return null;
               }
            });
   }
   
   public static void mockGoToTag(final MockedStatic<GitUtils> mockedGitUtils, final PeassFolders folders, final File statePredecessor, final File stateMainVersion) {
      mockedGitUtils.when(() -> GitUtils.goToTag(Mockito.eq("000001~1"), Mockito.any(File.class))).thenAnswer(new Answer<Void>() {

         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            final File destFile = (File) invocation.getArgument(1);
            LOG.debug("Loading faster..");
            FileUtils.deleteDirectory(destFile);
            FileUtils.copyDirectory(statePredecessor, destFile);
            return null;
         }
      });
      
      mockedGitUtils.when(() -> GitUtils.goToTag(Mockito.eq("000001"), Mockito.any(File.class))).thenAnswer(new Answer<Void>() {

         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            final File destFile = (File) invocation.getArgument(1);
            LOG.debug("Loading slower..");
            FileUtils.copyDirectory(stateMainVersion, destFile);
            return null;
         }
      });
   }
   
   public static void mockExecutor(final MockedStatic<ExecutorCreator> executor, final PeassFolders folders, final MeasurementConfig config) {
      TestExecutor mocked = Mockito.mock(TestExecutor.class);
      executor.when(() -> ExecutorCreator.createExecutor(Mockito.any(), Mockito.any(), Mockito.any()))
      .then(new Answer<TestExecutor>() {

         @Override
         public TestExecutor answer(final InvocationOnMock invocation) throws Throwable {
            PeassFolders folders = invocation.getArgument(0);
            MavenTestExecutorMocker.writeValue(folders, 100);
            return mocked;
         }
      });
      
      Mockito.when(mocked.getTestTransformer()).thenReturn(new JUnitTestTransformer(folders.getProjectFolder(), config));
   }
   
   public static void mockExecutor(final MockedStatic<ExecutorCreator> executor) {
      TestExecutor mocked = Mockito.mock(TestExecutor.class);
      executor.when(() -> ExecutorCreator.createExecutor(Mockito.any(), Mockito.any(), Mockito.any()))
      .then(new Answer<TestExecutor>() {

         @Override
         public TestExecutor answer(final InvocationOnMock invocation) throws Throwable {
            PeassFolders folders = invocation.getArgument(0);
            MavenTestExecutorMocker.writeValue(folders, 100);
            return mocked;
         }
      });
   }
}
