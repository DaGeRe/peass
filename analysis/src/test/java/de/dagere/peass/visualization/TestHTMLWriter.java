package de.dagere.peass.visualization;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;



import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.dagere.peass.TestConstants;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.CauseSearcherConfigMixin;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.visualization.html.HTMLWriter;

public class TestHTMLWriter {
   
   @Test
   public void testRegularWriting() throws JsonProcessingException, FileNotFoundException, IOException {
      GraphNode rootNode = new GraphNode("Test#test", "public void Test.test()", "public void Test.test()");
      rootNode.setName("Test#test");
      GraphNode kopemeConvertedData = new GraphNode("Test#test", "public void Test.test()", "public void Test.test()");
      CauseSearchData data = new CauseSearchData();
      data.setCauseConfig(new CauseSearcherConfig(new TestCase("Test", "test"), new CauseSearcherConfigMixin()));
      data.setConfig(new MeasurementConfig(10));
      data.getMeasurementConfig().getExecutionConfig().setCommit("1");
      
      HTMLWriter writer = new HTMLWriter(rootNode, data, TestConstants.CURRENT_FOLDER, null, kopemeConvertedData);
      writer.writeHTML();
      
      File htmlFile = new File(TestConstants.CURRENT_FOLDER, "1/Test_test.html");
      Assert.assertTrue(htmlFile.exists());
      File dashboardFile = new File(TestConstants.CURRENT_FOLDER, "1/Test_test_dashboard.html");
      Assert.assertTrue(dashboardFile.exists());
      File jsFile = new File(TestConstants.CURRENT_FOLDER, "1/Test_test.js");
      Assert.assertTrue(jsFile.exists());
   }
   
   @Test
   public void testParameterizedWriting() throws JsonProcessingException, FileNotFoundException, IOException {
      GraphNode rootNode = new GraphNode("Test#test", "public void Test.test(int)", "public void Test.test(int)");
      rootNode.setName("Test#test");
      GraphNode kopemeConvertedData = new GraphNode("Test#test", "public void Test.test(int)", "public void Test.test(int)");
      CauseSearchData data = new CauseSearchData();
      data.setCauseConfig(new CauseSearcherConfig(new TestCase("Test", "test", "", "int"), new CauseSearcherConfigMixin()));
      data.setConfig(new MeasurementConfig(10));
      data.getMeasurementConfig().getExecutionConfig().setCommit("1");
      
      HTMLWriter writer = new HTMLWriter(rootNode, data, TestConstants.CURRENT_FOLDER, null, kopemeConvertedData);
      writer.writeHTML();
      
      File htmlFile = new File(TestConstants.CURRENT_FOLDER, "1/Test_test(int).html");
      Assert.assertTrue(htmlFile.exists());
      File dashboardFile = new File(TestConstants.CURRENT_FOLDER, "1/Test_test(int)_dashboard.html");
      Assert.assertTrue(dashboardFile.exists());
      File jsFile = new File(TestConstants.CURRENT_FOLDER, "1/Test_test(int).js");
      Assert.assertTrue(jsFile.exists());
   }
}
