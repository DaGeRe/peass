package de.dagere.peass.jmh;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;



import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.TestUtil;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.jmh.JmhKoPeMeConverter;

public class TestResultConversion {
   
   private static final File CONVERSION_FOLDER = new File(JmhTestConstants.JMH_EXAMPLE_FOLDER, "conversion");
   private static final File CONVERSION_TEMP_FOLDER = new File("target/jmh-it");
   
   @BeforeEach
   public void cleanup() {
      CONVERSION_TEMP_FOLDER.mkdirs();
      TestUtil.deleteContents(CONVERSION_TEMP_FOLDER);
   }
   
   @Test
   public void testDefaultConversion()  {
      File jmhFile = new File(CONVERSION_FOLDER, "testMethod.json");
      
      Set<File> resultFiles = convert(jmhFile);
      
      MatcherAssert.assertThat(resultFiles, Matchers.hasSize(1));
      
      File resultFile = resultFiles.iterator().next();
      Kopemedata data = JSONDataLoader.loadData(resultFile);
      
      List<VMResult> results = data.getFirstDatacollectorContent();
      MatcherAssert.assertThat(results, Matchers.hasSize(1));
      VMResult result = results.get(0);
      Assert.assertEquals(result.getValue(), 1101075.0, 0.01);
      Assert.assertEquals(result.getFulldata().getValues().get(0).getValue(), 1101075);
   }
   
   @Test
   public void testIterations()  {
      File jmhFile = new File(CONVERSION_FOLDER, "test.json");
      
      Set<File> resultFiles = convert(jmhFile);
      
      MatcherAssert.assertThat(resultFiles, Matchers.hasSize(1));
      
      File resultFile = resultFiles.iterator().next();
      Kopemedata data = JSONDataLoader.loadData(resultFile);
      
      List<VMResult> results = data.getFirstDatacollectorContent();
      MatcherAssert.assertThat(results, Matchers.hasSize(2));
      VMResult result = results.get(0);
      Assert.assertEquals(10, result.getIterations());
      Assert.assertEquals(0, result.getWarmup());
      System.out.println(result.getRepetitions());
   }
   
   @Test
   public void testThroughputConversion()  {
      File jmhFile = new File(CONVERSION_FOLDER, "testMethod_throughput.json");
      
      Set<File> resultFiles = convert(jmhFile);
      
      MatcherAssert.assertThat(resultFiles, Matchers.hasSize(1));
      
      File resultFile = resultFiles.iterator().next();
      Kopemedata data = JSONDataLoader.loadData(resultFile);
      
      List<VMResult> results = data.getFirstDatacollectorContent();
      MatcherAssert.assertThat(results, Matchers.hasSize(5));
      VMResult result = results.get(0);
      Assert.assertEquals(result.getValue(), 1064297.5, 0.01);
      Assert.assertEquals(result.getFulldata().getValues().get(0).getValue(), 1063790.0, 0.01);
   }
   
   @Test
   public void testOriginalJmhConversion()  {
      File jmhFile = new File(CONVERSION_FOLDER, "example.json");
      
      Set<File> resultFiles = convert(jmhFile);
      
      MatcherAssert.assertThat(resultFiles, Matchers.hasSize(1));
      
      File resultFile = resultFiles.iterator().next();
      Kopemedata data = JSONDataLoader.loadData(resultFile);
      
      List<VMResult> results = data.getFirstDatacollectorContent();
      MatcherAssert.assertThat(results, Matchers.hasSize(3));
      VMResult result = results.get(0);
      Assert.assertEquals(result.getValue(), 1.0286803E7, 0.01);
      Assert.assertEquals(result.getFulldata().getValues().get(0).getValue(), 10386085);
   }
   
   @Test
   public void testOriginalJmhConversionParams()  {
      File jmhFile = new File(CONVERSION_FOLDER, "example-with-params.json");
      
      Set<File> resultFiles = convert(jmhFile);
      
      MatcherAssert.assertThat(resultFiles, Matchers.hasSize(1));
      
      File resultFile = resultFiles.iterator().next();
      Kopemedata data = JSONDataLoader.loadData(resultFile);
      
      List<VMResult> results = data.getFirstDatacollectorContent();
      MatcherAssert.assertThat(results, Matchers.hasSize(3));
      VMResult result = results.get(0);
      Entry<String, String> parameter = result.getParameters().entrySet().iterator().next();
      Assert.assertEquals(parameter.getKey(), "TEST_PARAM");
      Assert.assertEquals(parameter.getValue(), "val1");
   }
   
   private Set<File> convert(final File jmhFile) {
      JmhKoPeMeConverter converter = new JmhKoPeMeConverter(new MeasurementConfig(-1));
      Set<File> resultFiles = converter.convertToXMLData(jmhFile, CONVERSION_TEMP_FOLDER);
      return resultFiles;
   }
}
