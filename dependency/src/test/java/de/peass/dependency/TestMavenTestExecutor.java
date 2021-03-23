package de.peass.dependency;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependency.execution.MavenTestExecutor;
import de.peass.testtransformation.JUnitTestTransformer;

public class TestMavenTestExecutor {
   
   @Test
   public void testParameterConcatenation() throws IOException, XmlPullParserException, InterruptedException {
      JUnitTestTransformer testTransformerMock = Mockito.mock(JUnitTestTransformer.class);
      Mockito.when(testTransformerMock.getConfig()).thenReturn(new MeasurementConfiguration(5));
      
      PeASSFolders foldersMock = Mockito.mock(PeASSFolders.class);
      Mockito.when(foldersMock.getTempDir()).thenReturn(new File("/tmp/test2"));
      Mockito.when(foldersMock.getTempMeasurementFolder()).thenReturn(new File("/tmp/test2"));
      
      MavenTestExecutor executor = new MavenTestExecutor(foldersMock, 
            testTransformerMock, 
            new EnvironmentVariables("-Pvar1=1 -Pvar5=asd"));
      MavenTestExecutor testExecutor = Mockito.spy(executor);
      
      testExecutor.buildMavenProcess(new File("/tmp/test"), new String[] {"addition1", "addition2", "addition3"});
      
      ArgumentCaptor<String[]> parametersCaptor = ArgumentCaptor.forClass(String[].class);
      Mockito.verify(testExecutor).buildFolderProcess(Mockito.any(), Mockito.any(), parametersCaptor.capture());
      
      String[] capturedValue = parametersCaptor.getValue();
      System.out.println("Captured: " + Arrays.toString(capturedValue));
      Assert.assertEquals("test", capturedValue[1]);
      Assert.assertEquals("-Djava.io.tmpdir=/tmp/test2", capturedValue[10]);
      Assert.assertEquals("addition1", capturedValue[11]);
      Assert.assertEquals("addition3", capturedValue[13]);
   }
}
