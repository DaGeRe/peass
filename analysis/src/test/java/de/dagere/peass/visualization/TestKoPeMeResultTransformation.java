package de.dagere.peass.visualization;

import java.io.File;



import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.folders.PeassFolders;

public class TestKoPeMeResultTransformation {
   private static final double DELTA = 0.0001;

   @Test
   public void testConversion()  {

      final TestCase testcase = new TestCase("com.example.android_example.ExampleUnitTest", "test_TestMe");
      CauseSearchFolders folders = new CauseSearchFolders(new File("src/test/resources/visualization/project_test"));
      KoPeMeTreeConverter converter = new KoPeMeTreeConverter(folders, "7675e29a368e5ac051e76c145e84c80af7ae1e88", testcase);
      GraphNode convertedNode = converter.getData();
      Assert.assertEquals(0.004, convertedNode.getStatistic().getMeanOld(), DELTA);
      Assert.assertEquals(0.003, convertedNode.getStatistic().getMeanCurrent(), DELTA);
      Assert.assertEquals(0.0035, convertedNode.getVmValues().getValues().get(0).get(0).getMean(), DELTA);
      Assert.assertEquals(0.0025, convertedNode.getVmValues().getValues().get(0).get(1).getMean(), DELTA);

      Assert.assertEquals(0.0045, convertedNode.getVmValuesPredecessor().getValues().get(0).get(0).getMean(), DELTA);
      Assert.assertEquals(0.0035, convertedNode.getVmValuesPredecessor().getValues().get(0).get(1).getMean(), DELTA);

      Assert.assertEquals(0.003, convertedNode.getValues()[0], DELTA);
      Assert.assertEquals(0.003, convertedNode.getValues()[1], DELTA);
      Assert.assertEquals(0.004, convertedNode.getValuesPredecessor()[0], DELTA);
      Assert.assertEquals(0.004, convertedNode.getValuesPredecessor()[1], DELTA);
   }

   @Test
   public void testDetailFolderReading()  {
      final TestCase testcase = new TestCase("com.example.android_example.ExampleUnitTest", "test_TestMe");
      PeassFolders folders = new PeassFolders(new File("src/test/resources/visualization/project_test"));
      KoPeMeTreeConverter converter = new KoPeMeTreeConverter(folders.getDetailResultFolder(), "7675e29a368e5ac051e76c145e84c80af7ae1e88", testcase);
      GraphNode convertedNode = converter.getData();

      Assert.assertEquals(0.003, convertedNode.getStatistic().getMeanCurrent(), DELTA);
      Assert.assertEquals(0.004, convertedNode.getStatistic().getMeanOld(), DELTA);
   }
   
   @Test
   public void testWrongName()  {
      final TestCase testcase = new TestCase("com.example.android_example.ExampleUnitTest", "test_TestMe");
      PeassFolders folders = new PeassFolders(new File("src/test/resources/visualization/project_wrong"));
      KoPeMeTreeConverter converter = new KoPeMeTreeConverter(folders.getDetailResultFolder(), "7675e29a368e5ac051e76c145e84c80af7ae1e88", testcase);
      GraphNode convertedNode = converter.getData();

      Assert.assertEquals(0.003, convertedNode.getStatistic().getMeanCurrent(), DELTA);
      Assert.assertEquals(0.004, convertedNode.getStatistic().getMeanOld(), DELTA);
   }
}
