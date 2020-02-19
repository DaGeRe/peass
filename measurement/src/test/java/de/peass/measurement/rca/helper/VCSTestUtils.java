package de.peass.measurement.rca.helper;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import de.peass.dependency.PeASSFolders;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionControlSystem;

public class VCSTestUtils {
   
   private static final Logger LOG = LogManager.getLogger(VCSTestUtils.class);
   
   public static void mockGetVCS() {
      PowerMockito.mockStatic(VersionControlSystem.class);
      PowerMockito.doAnswer(new Answer<VersionControlSystem>() {

         @Override
         public VersionControlSystem answer(final InvocationOnMock invocation) throws Throwable {
            return VersionControlSystem.GIT;
         }
      }).when(VersionControlSystem.class);
      VersionControlSystem.getVersionControlSystem(Mockito.any(File.class));
   }
   
   public static void mockClone(final File projectFolderTemp, final File clonedDir) throws InterruptedException, IOException {
      PowerMockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            FileUtils.copyDirectory(clonedDir, projectFolderTemp);
            return null;
         }
      }).when(GitUtils.class);
      GitUtils.clone(Mockito.any(PeASSFolders.class), Mockito.any(File.class));
   }
   
   public static void mockGoToTagAny(final File anyVersion) {
      PowerMockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            final File destFile = (File) invocation.getArgument(1);
            LOG.debug("Loading version..");
            FileUtils.deleteDirectory(destFile);
            
            FileUtils.copyDirectory(anyVersion, destFile);
            return null;
         }
      }).when(GitUtils.class);
      GitUtils.goToTag(Mockito.anyString(), Mockito.any(File.class));
   }

   public static void mockGoToTag(final PeASSFolders folders, final File statePredecessor, final File stateMainVersion) {
      PowerMockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            final File destFile = (File) invocation.getArgument(1);
            LOG.debug("Loading faster..");
            FileUtils.deleteDirectory(destFile);
            FileUtils.copyDirectory(statePredecessor, destFile);
            return null;
         }
      }).when(GitUtils.class);
      GitUtils.goToTag(Mockito.eq("000001~1"), Mockito.any(File.class));

      PowerMockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            final File destFile = (File) invocation.getArgument(1);
            LOG.debug("Loading slower..");
            FileUtils.copyDirectory(stateMainVersion, destFile);
            return null;
         }
      }).when(GitUtils.class);
      GitUtils.goToTag(Mockito.eq("000001"), Mockito.any(File.class));
   }
}
