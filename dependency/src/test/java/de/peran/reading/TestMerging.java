package de.peran.reading;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependency.analysis.data.TestSet;
import de.peran.dependency.traces.ViewGenerator;
import de.peran.generated.Versiondependencies;
import de.peran.reduceddependency.ChangedTraceTests;

public class TestMerging {

   private static final ObjectMapper MAPPER = new ObjectMapper();
   
   
   @Test
   public void testMerge() throws IOException {
      final File testFolder = new File("target/mergetest/real");
      if (testFolder.exists()) {
         FileUtils.deleteDirectory(testFolder);
      }
      final File testFolderTemp = new File("target/mergetest/temp");
      if (testFolderTemp.exists()) {
         FileUtils.deleteDirectory(testFolderTemp);
      }
      testFolder.mkdirs();
      testFolderTemp.mkdirs();
      final File projectFolder = new File(testFolder, "projekt");
      final File viewFolder = new File(testFolder, "views");
      
      final File executefile = new File(testFolder, "execute.json");

      final File tempResultFolder = new File(testFolderTemp, "views");
      final File tempExecutefile = new File(testFolderTemp, "execute.json");
      final File tempViewfolder = new File(tempResultFolder, "view_1");
      tempViewfolder.mkdirs();

      final ChangedTraceTests tests = new ChangedTraceTests();
      final TestSet tests2 = new TestSet();
      tests2.addTest(new TestCase("Test#method"));
      tests.addCall("1", tests2);
      
      MAPPER.writeValue(tempExecutefile, tests);

      final ViewGenerator generator = new ViewGenerator(projectFolder, new Versiondependencies(), executefile, viewFolder);

      generator.mergeParallelVersion("1", tempResultFolder, tempExecutefile);
      
      final ChangedTraceTests readTests = MAPPER.readValue(executefile, ChangedTraceTests.class);
      Assert.assertThat(readTests.getVersions().keySet(), Matchers.contains("1"));
      
      final File realViewFolder = new File(viewFolder, "view_1");
      Assert.assertTrue(realViewFolder.exists());
   }
}
