package de.dagere.peass.jmh;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.dagere.peass.TestConstants;
import de.dagere.peass.TestUtil;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.jmh.JmhResultMover;

public class TestResultMoving {
   
   @Before
   public void clearCurrent() {
      TestUtil.deleteContents(TestConstants.CURRENT_FOLDER);
   }
   
   @Test
   public void testResultMoving() throws IOException {
      File currentPeassFolder = new File(JmhTestConstants.JMH_EXAMPLE_FOLDER, "current_peass");
      FileUtils.copyDirectory(currentPeassFolder, new File(TestConstants.CURRENT_FOLDER.getParentFile(), "current_peass"));
      PeASSFolders folders = new PeASSFolders(TestConstants.CURRENT_FOLDER);
      
      File jsonResultFile = new File(folders.getTempMeasurementFolder(), "testMethod.json");
      new JmhResultMover(folders).moveToMethodFolder(new TestCase("de.dagere.peass.ExampleBenchmark#testMethod"), jsonResultFile);
      
      //TODO Assert correct file and fix JmhResultMover
      File expectedXMLFile = new File(folders.getTempMeasurementFolder(), "de.dagere.peass.ExampleBenchmark/testMethod.xml");
      Assert.assertTrue(expectedXMLFile.exists());
   }
}
