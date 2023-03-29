package de.dagere.peass.jmh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.nodeDiffGenerator.data.TestMethodCall;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.jmh.JmhTestTransformer;

public class TestBenchmarkChangeDetection {
   @Test
   public void testBenchmarkDetection() throws FileNotFoundException, IOException, XmlPullParserException {
      JmhTestTransformer jmhTransformer = new JmhTestTransformer(JmhTestConstants.BASIC_VERSION, TestBenchmarkDetection.JMH_CONFIG);
      
      TestSet originalTests = new TestSet(new TestMethodCall("de.dagere.peass.ExampleBenchmark", (String) null, ""));
      
      ModuleClassMapping mapping = Mockito.mock(ModuleClassMapping.class);
      Mockito.when(mapping.getModules()).thenReturn(Arrays.asList(new File[] {JmhTestConstants.BASIC_VERSION}));
      TestSet changedTests = jmhTransformer.buildTestMethodSet(originalTests, mapping).getTestsToUpdate();
      Assert.assertEquals(changedTests.getTestMethods().size(), 1);
      
      TestMethodCall test = changedTests.getTestMethods().iterator().next();
      Assert.assertEquals("de.dagere.peass.ExampleBenchmark#testMethod", test.getExecutable());
   }
}
