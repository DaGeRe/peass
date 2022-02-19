package de.dagere.peass.analysis.measurement;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.ChangeReader;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.dependency.persistence.SelectedTests;
import de.dagere.peass.utils.Constants;

public class TestChangeReader {
   
   @Test
   public void testBasicUsage() throws JAXBException, JsonProcessingException {
      ChangeReader reader = new ChangeReader("android-example-correct", new SelectedTests());
      ProjectChanges changes = reader.readFile(new File(TestChangeReading.DATA_READING_FOLDER, "measurementsFull"));
      
      System.out.println(Constants.OBJECTMAPPER.writeValueAsString(changes));
      
      List<Change> methodChanges = changes.getVersionChanges().get("946e4318267b56838aa35da0a2a4e5c0528bfe04").getTestcaseChanges().get("com.example.android_example.ExampleUnitTest");
      Assert.assertEquals(1, methodChanges.size());
      Assert.assertEquals("test_TestMe", methodChanges.get(0).getMethod());
      Assert.assertEquals(15087.66, methodChanges.get(0).getOldTime(), 0.1);
      
   }
   
   @Test
   public void testParameterizedUsage() throws JAXBException, JsonProcessingException {
      ChangeReader reader = new ChangeReader("demo-parameterized", new SelectedTests());
      ProjectChanges changes = reader.readFile(new File(TestChangeReading.DATA_READING_FOLDER, "measurement_a12a0b7f4c162794fca0e7e3fcc6ea3b3a2cbc2b_49f75e8877c2e9b7cf6b56087121a35fdd73ff8b"));
      
      System.out.println(Constants.OBJECTMAPPER.writeValueAsString(changes));
      
      List<Change> methodChanges = changes.getVersionChanges().get("a12a0b7f4c162794fca0e7e3fcc6ea3b3a2cbc2b").getTestcaseChanges().get("de.dagere.peass.ExampleTest");
      Assert.assertEquals(1, methodChanges.size());
      Assert.assertEquals("test", methodChanges.get(0).getMethod());
      Assert.assertEquals("JUNIT_PARAMETERIZED-1", methodChanges.get(0).getParams());
      Assert.assertEquals(4232151.27, methodChanges.get(0).getOldTime(), 0.1);
   }
}
