package de.dagere.peass.jmh;

import java.io.File;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.peass.TestUtil;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.jmh.JmhKoPeMeConverter;

public class TestResultConversion {
   
   private static final File CONVERSION_FOLDER = new File(JmhTestConstants.JMH_EXAMPLE_FOLDER, "conversion");
   private static final File CONVERSION_TEMP_FOLDER = new File("target/jmh-it");
   
   @Before
   public void cleanup() {
      CONVERSION_TEMP_FOLDER.mkdirs();
      TestUtil.deleteContents(CONVERSION_TEMP_FOLDER);
   }
   
   @Test
   public void testDefaultConversion() throws JAXBException {
      File jmhFile = new File(CONVERSION_FOLDER, "testMethod.json");
      
      Set<File> resultFiles = convert(jmhFile);
      
      Assert.assertThat(resultFiles, Matchers.hasSize(1));
      
      File resultFile = resultFiles.iterator().next();
      Kopemedata data = XMLDataLoader.loadData(resultFile);
      
      List<Result> results = data.getTestcases().getTestcase().get(0).getDatacollector().get(0).getResult();
      Assert.assertThat(results, Matchers.hasSize(1));
      Result result = results.get(0);
      Assert.assertEquals(result.getValue(), 1101075.0, 0.01);
      Assert.assertEquals(result.getFulldata().getValue().get(0).getValue(), 1101075);
   }
   
   @Test
   public void testOriginalJmhConversion() throws JAXBException {
      File jmhFile = new File(CONVERSION_FOLDER, "example.json");
      
      Set<File> resultFiles = convert(jmhFile);
      
      Assert.assertThat(resultFiles, Matchers.hasSize(1));
      
      File resultFile = resultFiles.iterator().next();
      Kopemedata data = XMLDataLoader.loadData(resultFile);
      
      List<Result> results = data.getTestcases().getTestcase().get(0).getDatacollector().get(0).getResult();
      Assert.assertThat(results, Matchers.hasSize(3));
      Result result = results.get(0);
      Assert.assertEquals(result.getValue(), 97.0, 0.01);
      Assert.assertEquals(result.getFulldata().getValue().get(0).getValue(), 96);
   }
   
   @Test
   public void testOriginalJmhConversionParams() throws JAXBException {
      File jmhFile = new File(CONVERSION_FOLDER, "example-with-params.json");
      
      Set<File> resultFiles = convert(jmhFile);
      
      Assert.assertThat(resultFiles, Matchers.hasSize(1));
      
      File resultFile = resultFiles.iterator().next();
      Kopemedata data = XMLDataLoader.loadData(resultFile);
      
      List<Result> results = data.getTestcases().getTestcase().get(0).getDatacollector().get(0).getResult();
      Assert.assertThat(results, Matchers.hasSize(3));
      Result result = results.get(0);
      Assert.assertEquals(result.getParams().getParam().get(0).getKey(), "TEST_PARAM");
      Assert.assertEquals(result.getParams().getParam().get(0).getValue(), "val1");
   }
   
   private Set<File> convert(final File jmhFile) {
      JmhKoPeMeConverter converter = new JmhKoPeMeConverter(new MeasurementConfiguration(-1));
      Set<File> resultFiles = converter.convertToXMLData(jmhFile, CONVERSION_TEMP_FOLDER);
      return resultFiles;
   }
}
