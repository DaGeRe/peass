package de.dagere.peass.jmh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Test;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.jmh.JmhTestTransformer;

public class TestBenchmarkChangeDetection {
   @Test
   public void testBenchmarkDetection() throws FileNotFoundException, IOException, XmlPullParserException {
      JmhTestTransformer jmhTransformer = new JmhTestTransformer(JmhTestConstants.BASIC_VERSION, new MeasurementConfiguration(3));
      
      TestSet originalTests = new TestSet(new TestCase("de.dagere.peass.ExampleBenchmark", null, ""));
      
      TestSet changedTests = jmhTransformer.buildTestMethodSet(originalTests, Arrays.asList(new File[] {JmhTestConstants.BASIC_VERSION}));
      TestCase test = changedTests.getTests().iterator().next();
      Assert.assertEquals("de.dagere.peass.ExampleBenchmark#testMethod", test.getExecutable());
   }
}
