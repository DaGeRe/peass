package de.dagere.peass.dependency;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.TestConstants;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.execution.maven.pom.MavenTestExecutor;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class TestDeletion {

   @BeforeEach
   public void init() throws IOException {
      FileUtils.deleteDirectory(TestConstants.CURRENT_FOLDER);
      FileUtils.deleteDirectory(TestConstants.CURRENT_PEASS);
      TestConstants.CURRENT_FOLDER.mkdir();
   }

   @Test
   public void testFileDeletion() throws IOException {
      TestExecutor executor = createExecutor(1);
      File file = createFile(2);
      executor.cleanAboveSize(TestConstants.CURRENT_PEASS, "txt");
      Assert.assertFalse(file.exists());
   }
   
   @Test
   public void testFileNoDeletion() throws IOException {
      TestExecutor executor = createExecutor(2);
      File file = createFile(1);
      
      executor.cleanAboveSize(TestConstants.CURRENT_PEASS, "txt");
      
      Assert.assertTrue(file.exists());
   }

   private TestExecutor createExecutor(int logSizeInMb) {
      JUnitTestTransformer transformer = Mockito.mock(JUnitTestTransformer.class);
      MeasurementConfig config = new MeasurementConfig(2);
      config.setMaxLogSizeInMb(logSizeInMb);
      Mockito.when(transformer.getConfig()).thenReturn(config);
      
      TestExecutor executor = new MavenTestExecutor(new PeassFolders(TestConstants.CURRENT_FOLDER), transformer, new EnvironmentVariables());
      return executor;
   }
   
   private File createFile(int sizeInMb) throws FileNotFoundException, IOException {
      File file = new File(TestConstants.CURRENT_PEASS, "myFile.txt");
      
      RandomAccessFile raf = new RandomAccessFile(file, "rw");
      
      raf.setLength(sizeInMb * 1024 * 1024);
      raf.close();
      return file;
   }
}
