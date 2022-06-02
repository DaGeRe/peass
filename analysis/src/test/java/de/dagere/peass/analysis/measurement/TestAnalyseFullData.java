package de.dagere.peass.analysis.measurement;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.StatisticsConfig;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.GitCommit;

public class TestAnalyseFullData {
   
   public static final File DATA_READING_FOLDER = new File("src/test/resources/dataReading");

   public static final File REGULAR_DATA_FOLDER = new File(DATA_READING_FOLDER, "measurementsFull/measurements/");
   public static final File PARAM_DATA_FOLDER = new File(DATA_READING_FOLDER, "measurement_a12a0b7f4c162794fca0e7e3fcc6ea3b3a2cbc2b_49f75e8877c2e9b7cf6b56087121a35fdd73ff8b");
   
   @Test
   public void testReading() throws InterruptedException {
      List<String> versions = Arrays.asList(new String[] {"946e4318267b56838aa35da0a2a4e5c0528bfe04","fd79b2039667c09167c721b2823425629fad6d11" });
      VersionComparator.setVersions(versions);
      
      File baseFolder = new File(DATA_READING_FOLDER, "android-example-correct");
      ModuleClassMapping mapping = new ModuleClassMapping(baseFolder, new ProjectModules(new File(baseFolder, "app")), new ExecutionConfig());
      AnalyseFullData afd = new AnalyseFullData(new File("target/test.json"), new ProjectStatistics(), mapping, new StatisticsConfig());
      
      afd.analyseFolder(REGULAR_DATA_FOLDER);
      
      ProjectChanges changes = afd.getProjectChanges();
      Set<String> changedTests = changes.getVersion("946e4318267b56838aa35da0a2a4e5c0528bfe04").getTestcaseChanges().keySet();
     
      MatcherAssert.assertThat(changedTests, Matchers.contains("app§com.example.android_example.ExampleUnitTest"));
   }
   
   @Test
   public void testReadingWithParams() throws InterruptedException {
      
      List<String> versions = Arrays.asList(new String[] {"49f75e8877c2e9b7cf6b56087121a35fdd73ff8b","a12a0b7f4c162794fca0e7e3fcc6ea3b3a2cbc2b" });
      VersionComparator.setVersions(versions);
      
      ModuleClassMapping mapping = Mockito.mock(ModuleClassMapping.class);
      ProjectStatistics statistics = new ProjectStatistics();
      AnalyseFullData afd = new AnalyseFullData(new File("target/test.json"), statistics, mapping, new StatisticsConfig());
      
      afd.analyseFolder(new File(PARAM_DATA_FOLDER, "measurements"));
      
      ProjectChanges changes = afd.getProjectChanges();
      Set<String> changedTests = changes.getVersion("a12a0b7f4c162794fca0e7e3fcc6ea3b3a2cbc2b").getTestcaseChanges().keySet();
     
      try {
         System.out.println(Constants.OBJECTMAPPER.writeValueAsString(statistics));
         
         System.out.println(Constants.OBJECTMAPPER.writeValueAsString(changes));
      } catch (JsonProcessingException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      
      MatcherAssert.assertThat(changedTests, Matchers.contains("de.dagere.peass.ExampleTest"));
      
      List<Change> methodChanges = TestChangeReader.checkParameterizedResult(changes);
      Assert.assertEquals(4152486.88, methodChanges.get(0).getOldTime(), 0.1);
   }
}
