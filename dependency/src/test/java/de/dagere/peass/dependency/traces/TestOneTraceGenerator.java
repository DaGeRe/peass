package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;

public class TestOneTraceGenerator {

   private static final File measurementsTempFolder_OOM = new File("src/test/resources/testOneTraceGenerator/demo-oom_peass/measurementsTemp/");
   private static final File measurementsTempFolder_Fine = new File("src/test/resources/testOneTraceGenerator/demo-fine_peass/measurementsTemp/");
   
   private static final File expectedResultFolder = new File("target/views_test-results/");
   private static final File expectedResultFile = new File(expectedResultFolder, "view_d4d964daa4a77bac09422174509c31a19d082ed4/de.dagere.peass.ExampleTest/test/d4d964.txt");

   @BeforeEach
   public void clean() throws IOException {
      FileUtils.cleanDirectory(expectedResultFolder);
   }

   @Test
   public void testFailedTestSkipping() {
      analyzeFolder(measurementsTempFolder_OOM);
      Assert.assertFalse(expectedResultFile.exists());
   }

   @Test
   public void testRegularExecution() {
      analyzeFolder(measurementsTempFolder_Fine);
      Assert.assertTrue(expectedResultFile.exists());
   }
   

   private void analyzeFolder(File moduleExampleResultsFolder) {
      try (MockedStatic<KiekerFolderUtil> kfu = Mockito.mockStatic(KiekerFolderUtil.class, Mockito.CALLS_REAL_METHODS)) {
         kfu.when(() -> KiekerFolderUtil.getModuleResultFolder(Mockito.any(), Mockito.any()))
               .thenReturn(new File(moduleExampleResultsFolder, "demo-project-gradle"));

         ResultsFolders resultsFolders = new ResultsFolders(new File("target/"), "test-results");
         PeassFolders folders = Mockito.mock(PeassFolders.class);
         TestCase testcase = new TestCase("de.dagere.peass.ExampleTest#test");
         OneTraceGenerator generator = new OneTraceGenerator(resultsFolders, folders, testcase, new TraceFileMapping(), "d4d964daa4a77bac09422174509c31a19d082ed4",
               new LinkedList<>(),
               Mockito.mock(ModuleClassMapping.class), new KiekerConfig(), new TestSelectionConfig(1, false));
         generator.generateTrace("d4d964daa4a77bac09422174509c31a19d082ed4");
      }
   }
}
