package de.dagere.peass.measurement.cleaning;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;



import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.io.FileMatchers;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.TestMethod;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.vcs.GitCommit;

public class TestCleaner {
   
   @Test
   public void testParameterizedDataCleaner() throws IOException {
      VersionComparator.setVersions(Arrays.asList(new GitCommit[] {
            new GitCommit("49f75e8877c2e9b7cf6b56087121a35fdd73ff8b", null, null, null),
            new GitCommit("a12a0b7f4c162794fca0e7e3fcc6ea3b3a2cbc2b", null, null, null)
      }));
      
      File measurementsFolder = new File("src/test/resources/cleaning/measurementsFull");
      
      File goalFolder = new File("target/test/cleaned");
      if (goalFolder.exists()) {
         FileUtils.deleteDirectory(goalFolder);
      }
      goalFolder.mkdirs();
      Cleaner cleaner = new Cleaner(goalFolder);
      
      cleaner.processDataFolder(measurementsFolder);
      
      File expectedCleanedFolder_1 = new File(goalFolder, "ExampleTest_test(JUNIT_PARAMETERIZED-0).json");
      MatcherAssert.assertThat(expectedCleanedFolder_1, FileMatchers.anExistingFile());
      
      Kopemedata data = JSONDataLoader.loadData(expectedCleanedFolder_1);
      TestMethod testcase1 = data.getFirstMethodResult();
      Assert.assertEquals("test", testcase1.getMethod());
      Assert.assertEquals(10, testcase1.getDatacollectorResults().get(0).getChunks().get(0).getResults().size());
      
      File expectedCleanedFolder_2 = new File(goalFolder, "ExampleTest_test(JUNIT_PARAMETERIZED-1).json");
      MatcherAssert.assertThat(expectedCleanedFolder_2, FileMatchers.anExistingFile());
   }
}
