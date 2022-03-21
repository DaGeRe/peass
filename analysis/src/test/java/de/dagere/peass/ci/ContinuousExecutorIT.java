package de.dagere.peass.ci;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.TestUtil;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependencytests.DependencyTestConstants;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.utils.Constants;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class ContinuousExecutorIT {

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
      MeasurementConfig measurementConfig = new MeasurementConfig(3);
      measurementConfig.setIterations(5);
      measurementConfig.setWarmup(5);
      measurementConfig.setRepetitions(2);
      ContinuousExecutor executor = new ContinuousExecutor(DependencyTestConstants.CURRENT, measurementConfig, dependencyConfig, new EnvironmentVariables());
      executor.execute();
      
      checkChangesJson();
   }


   private void checkChangesJson() throws IOException, JsonParseException, JsonMappingException {
      File changeFile = new File(fullPeassFolder, "changes.json");
      ProjectChanges changes = Constants.OBJECTMAPPER.readValue(changeFile, ProjectChanges.class);
      
      String changedTestClass = changes.getVersion("ff2ab99a0d24c90abe610fb318a17db6da473208").getTestcaseChanges().keySet().iterator().next();
      TestCase tc = new TestCase(changedTestClass);
      Assert.assertEquals("com.example.android_example.ExampleUnitTest", tc.getClazz());
   }

   private void initRepo() throws ZipException {
      try (ZipFile file = new ZipFile(new File("src/test/resources/test-gradle-1.zip"))) {
         file.extractAll(DependencyTestConstants.CURRENT.getAbsolutePath());
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
