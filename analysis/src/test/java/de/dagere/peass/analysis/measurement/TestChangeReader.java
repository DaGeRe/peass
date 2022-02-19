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
      
      List<Change> methodChanges = changes.getVersionChanges().get("946e4318267b56838aa35da0a2a4e5c0528bfe04").getTestcaseChanges().get("com.example.android_example.ExampleUnitTest");
      Assert.assertEquals(1, methodChanges.size());
      Assert.assertEquals("test_TestMe", methodChanges.get(0).getMethod());
      
      System.out.println(Constants.OBJECTMAPPER.writeValueAsString(changes));
   }
   
   @Test
   public void testParameterizedUsage() {
      // TODO
   }
}
