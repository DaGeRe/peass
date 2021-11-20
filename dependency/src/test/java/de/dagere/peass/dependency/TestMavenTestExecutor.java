package de.dagere.peass.dependency;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.execution.processutils.ProcessBuilderHelper;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

//TODO Fix test by creating MavenTestExecutor and mocking the creation of the ProcessBuilderHelper with PowerMock
public class TestMavenTestExecutor {
   
   @Test
   public void testParameterConcatenation() throws IOException, XmlPullParserException, InterruptedException {
      JUnitTestTransformer testTransformerMock = Mockito.mock(JUnitTestTransformer.class);
      Mockito.when(testTransformerMock.getConfig()).thenReturn(new MeasurementConfig(5));
      
      PeassFolders foldersMock = Mockito.mock(PeassFolders.class);
      File folder = new File("/tmp/test2");
      folder.mkdirs();
      Mockito.when(foldersMock.getTempDir()).thenReturn(folder);
      Mockito.when(foldersMock.getTempMeasurementFolder()).thenReturn(folder);
      
      ProcessBuilderHelper helper = new ProcessBuilderHelper(new EnvironmentVariables("-Pvar1=1 -Pvar5=asd"), foldersMock);
//      MavenTestExecutor executor = new MavenTestExecutor(foldersMock, 
//            testTransformerMock, 
//            new EnvironmentVariables("-Pvar1=1 -Pvar5=asd"));
      ProcessBuilderHelper testExecutor = Mockito.spy(helper);
      
      testExecutor.buildFolderProcess(folder, new File("/tmp/log"), new String[] {"echo", "addition2", "addition3"});
      
      ArgumentCaptor<String[]> parametersCaptor = ArgumentCaptor.forClass(String[].class);
      Mockito.verify(testExecutor).buildFolderProcess(Mockito.any(), Mockito.any(), parametersCaptor.capture());
      
      String[] capturedValue = parametersCaptor.getValue();
      System.out.println("Captured: " + Arrays.toString(capturedValue));
      Assert.assertEquals("addition2", capturedValue[1]);
      Assert.assertEquals("addition3", capturedValue[2]);
//      Assert.assertEquals("-Djava.io.tmpdir=/tmp/test2", capturedValue[3]);
//      Assert.assertEquals("-DfailIfNoTests=false", capturedValue[10]);
//      Assert.assertEquals("addition1", capturedValue[11]);
//      Assert.assertEquals("addition3", capturedValue[13]);
   }
}
