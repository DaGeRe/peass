package de.dagere.peass.jmh;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.TestConstants;
import de.dagere.peass.TestUtil;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.jmh.JmhResultMover;
import de.dagere.peass.dependency.traces.KiekerFolderUtil;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.folders.PeassFolders;

public class TestResultMoving {

   @BeforeEach
   public void clearCurrent() throws IOException {
      TestUtil.deleteContents(TestConstants.CURRENT_FOLDER);
      TestUtil.deleteContents(TestConstants.CURRENT_PEASS);

      FileUtils.copyDirectory(JmhTestConstants.BASIC_VERSION, TestConstants.CURRENT_FOLDER);

      File currentPeassFolder = new File(JmhTestConstants.JMH_EXAMPLE_FOLDER, "current_peass");
      FileUtils.copyDirectory(currentPeassFolder, TestConstants.CURRENT_PEASS);
   }

   @Test
   public void testResultMoving() throws IOException, ViewNotFoundException {
      PeassFolders folders = new PeassFolders(TestConstants.CURRENT_FOLDER);

      File jsonResultFile = new File(folders.getTempMeasurementFolder(), "testMethod.json");
      TestCase testcase = new TestCase("de.dagere.peass.ExampleBenchmark#testMethod");
      new JmhResultMover(folders, new MeasurementConfig(1)).moveToMethodFolder(testcase, jsonResultFile);

      // TODO Assert correct file and fix JmhResultMover
      File expectedXMLFile = new File(folders.getTempMeasurementFolder(), "de.dagere.peass/example/de.dagere.peass.ExampleBenchmark/testMethod.xml");
      Assert.assertTrue(expectedXMLFile.exists());

      final File moduleResultsFolder = KiekerFolderUtil.getModuleResultFolder(folders, testcase);
      final File kiekerResultFolder = KiekerFolderUtil.getClazzMethodFolder(testcase, moduleResultsFolder)[0];
      
      List<File> fileNames = Arrays.asList(kiekerResultFolder.listFiles()).stream()
            .filter(file -> file.getName().endsWith(".dat"))
            .collect(Collectors.toList());
      System.out.println(fileNames);
      MatcherAssert.assertThat(fileNames.get(0).getName(), Matchers.startsWith("kieker-"));
   }
}
