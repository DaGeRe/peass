package de.dagere.peass.dependency;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import de.dagere.peass.TestConstants;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.execution.ProjectModules;
import de.dagere.peass.dependency.execution.TestExecutor;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class TestDependencyManager {
   
   private static final Logger LOG = LogManager.getLogger(TestDependencyManager.class);
   
   @Test
   public void testBigFolderDeletion() throws IOException, InterruptedException, XmlPullParserException {
      final PeassFolders folders = new PeassFolders(TestConstants.CURRENT_FOLDER);

      final TestExecutor testExecutorMock = Mockito.mock(TestExecutor.class);

      final File testFolder = new File(folders.getTempMeasurementFolder(), "MyTestClass/15231312");
      final File rubishFile = new File(testFolder, "myRubish.txt");
      
      prepareMock(folders, testExecutorMock, testFolder, rubishFile);

      JUnitTestTransformer transformer = new JUnitTestTransformer(folders.getProjectFolder(), new MeasurementConfiguration(5));
      DependencyManager manager = new DependencyManager(testExecutorMock, folders, transformer);

      manager.setDeleteFolderSize(1);
      manager.initialyGetTraces("1");
      
      Assert.assertFalse(rubishFile.exists());
      Assert.assertFalse(testFolder.exists());
   }

   private void prepareMock(final PeassFolders folders, final TestExecutor testExecutorMock, final File testFolder, final File rubishFile) {
      try {
         Mockito.when(testExecutorMock.getModules()).thenReturn(Mockito.mock(ProjectModules.class));
      } catch (IOException | XmlPullParserException  e) {
         e.printStackTrace();
      }
   }
}
