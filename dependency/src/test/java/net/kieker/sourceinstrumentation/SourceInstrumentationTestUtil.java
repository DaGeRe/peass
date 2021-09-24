package net.kieker.sourceinstrumentation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import de.dagere.peass.TestConstants;

public class SourceInstrumentationTestUtil {
   
   public static void initSimpleProject(final String sourcePath) throws IOException {
      TestConstants.CURRENT_FOLDER.mkdirs();
      
      for (String path : new String[] {"src/main/java/de/peass/C0_0.java", 
            "src/test/java/de/peass/MainTest.java", 
            "pom.xml"}) {
         copyResource(path, sourcePath);
      }
      for (String path : new String[] {
            "src/main/java/de/peass/C1_0.java", 
            "src/main/java/de/peass/AddRandomNumbers.java"}) {
         File testFile = new File(TestConstants.CURRENT_FOLDER, path);
         testFile.delete();
      }
   }
   
   public static void initProject(final String sourcePath) throws IOException {
      TestConstants.CURRENT_FOLDER.mkdirs();
      
      for (String path : new String[] {"src/main/java/de/peass/C0_0.java", 
            "src/main/java/de/peass/C1_0.java", 
            "src/main/java/de/peass/AddRandomNumbers.java", 
            "src/test/java/de/peass/MainTest.java", 
            "pom.xml"}) {
         copyResource(path, sourcePath);
      }
   }
   
   public static File copyResource(final String name, final String sourcePath) throws IOException {
      File testFile = new File(TestConstants.CURRENT_FOLDER, name);
      if (!testFile.getParentFile().exists()) {
         testFile.getParentFile().mkdirs();
      }
      System.out.println(sourcePath + name);
      final URL exampleClass = TestSourceInstrumentation.class.getResource(sourcePath + name);
      FileUtils.copyURLToFile(exampleClass, testFile);
      return testFile;
   }
   
   public static void testFileIsNotInstrumented(final File testFile, final String fqn) throws IOException {
      String changedSource = FileUtils.readFileToString(testFile, StandardCharsets.UTF_8);

      MatcherAssert.assertThat(changedSource, Matchers.not(Matchers.containsString("MonitoringController.getInstance().isMonitoringEnabled()")));
      MatcherAssert.assertThat(changedSource, Matchers.not(Matchers.containsString(fqn)));
      MatcherAssert.assertThat(changedSource, Matchers.not(Matchers.containsString("new OperationExecutionRecord")));
      MatcherAssert.assertThat(changedSource, Matchers.not(Matchers.containsString("kieker.monitoring.core.controller.MonitoringController")));
   }
}
