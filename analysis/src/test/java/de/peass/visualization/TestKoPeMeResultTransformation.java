package de.peass.visualization;

import java.io.File;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.TestCase;

public class TestKoPeMeResultTransformation {
   @Test
   public void testConversion() throws JAXBException {
      
      final TestCase testcase = new TestCase("com.example.android_example.ExampleUnitTest", "test_TestMe");
      CauseSearchFolders folders = new CauseSearchFolders(new File("src/test/resources/visualization/project_test"));
      KoPeMeTreeConverter converter = new KoPeMeTreeConverter(folders, "7675e29a368e5ac051e76c145e84c80af7ae1e88", testcase);
      GraphNode convertedNode = converter.getData();
      Assert.assertEquals(40, convertedNode.getStatistic().getMeanOld(), 0.01);
      Assert.assertEquals(30, convertedNode.getStatistic().getMeanCurrent(), 0.01);
      Assert.assertEquals(35, convertedNode.getVmValues().getValues().get(0).get(0).getMean(), 0.01);
      Assert.assertEquals(25, convertedNode.getVmValues().getValues().get(0).get(1).getMean(), 0.01);
      
      Assert.assertEquals(45, convertedNode.getVmValuesPredecessor().getValues().get(0).get(0).getMean(), 0.01);
      Assert.assertEquals(35, convertedNode.getVmValuesPredecessor().getValues().get(0).get(1).getMean(), 0.01);
      
   }
}
