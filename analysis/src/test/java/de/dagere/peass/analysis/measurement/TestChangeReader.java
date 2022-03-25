package de.dagere.peass.analysis.measurement;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.ChangeReader;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;

public class TestChangeReader {

   private static final File ORDER_PROBLEM_FOLDER = new File(TestAnalyseFullData.DATA_READING_FOLDER, "changeReader-orderProblem");

   @Test
   public void testBasicUsage() throws JAXBException, JsonProcessingException {
      ChangeReader reader = new ChangeReader("android-example-correct", new ExecutionData());
      ProjectChanges changes = reader.readFile(TestAnalyseFullData.REGULAR_DATA_FOLDER.getParentFile());

      System.out.println(Constants.OBJECTMAPPER.writeValueAsString(changes));

      List<Change> methodChanges = changes.getVersionChanges().get("946e4318267b56838aa35da0a2a4e5c0528bfe04").getTestcaseChanges()
            .get("com.example.android_example.ExampleUnitTest");
      Assert.assertEquals(1, methodChanges.size());
      Assert.assertEquals("test_TestMe", methodChanges.get(0).getMethod());
      Assert.assertEquals(15087.66, methodChanges.get(0).getOldTime(), 0.1);

   }

   @Test
   public void testParameterizedUsage() throws JAXBException, JsonProcessingException {
      ChangeReader reader = new ChangeReader("demo-parameterized", new ExecutionData());
      ProjectChanges changes = reader.readFile(TestAnalyseFullData.PARAM_DATA_FOLDER);

      System.out.println(Constants.OBJECTMAPPER.writeValueAsString(changes));

      List<Change> methodChanges = checkParameterizedResult(changes);
      Assert.assertEquals(4232151.27, methodChanges.get(0).getOldTime(), 0.1);
   }

   @Test
   public void testOrderOfResults() throws StreamReadException, DatabindException, IOException, JAXBException {
      File staticTestSelectionFile = new File(ORDER_PROBLEM_FOLDER, "staticTestSelection_commons-fileupload3.json");
      File measurementsFolder = new File(ORDER_PROBLEM_FOLDER, "measurement_4ed6e923cb2033272fcb993978d69e325990a5aa_fdf011a5f9a15826771b19cdd6795b247b0bc3e4");
      StaticTestSelection staticTestSelection = Constants.OBJECTMAPPER.readValue(staticTestSelectionFile, StaticTestSelection.class);

      ResultsFolders resultsFolders = new ResultsFolders(new File("target/temp"), "temp");
      ChangeReader reader = new ChangeReader(resultsFolders, staticTestSelection);
      reader.readFile(measurementsFolder);

      ProjectChanges changes = Constants.OBJECTMAPPER.readValue(resultsFolders.getChangeFile(), ProjectChanges.class);
      Assert.assertNull(changes.getVersionChanges().get("fdf011a5f9a15826771b19cdd6795b247b0bc3e4"));
      Assert.assertEquals(changes.getVersionChanges().get("4ed6e923cb2033272fcb993978d69e325990a5aa").getTestcaseChanges().get("org.apache.commons.fileupload.ServletFileUploadTest").size(), 4);
      
      ProjectStatistics statistics = Constants.OBJECTMAPPER.readValue(resultsFolders.getStatisticsFile(), ProjectStatistics.class);
      Assert.assertEquals(statistics.getStatistics().get("4ed6e923cb2033272fcb993978d69e325990a5aa").size(), 8);
   }

   public static List<Change> checkParameterizedResult(ProjectChanges changes) {
      List<Change> methodChanges = changes.getVersionChanges().get("a12a0b7f4c162794fca0e7e3fcc6ea3b3a2cbc2b").getTestcaseChanges().get("de.dagere.peass.ExampleTest");
      Assert.assertEquals(1, methodChanges.size());
      Assert.assertEquals("test", methodChanges.get(0).getMethod());
      Assert.assertEquals("JUNIT_PARAMETERIZED-1", methodChanges.get(0).getParams());
      return methodChanges;
   }
}
