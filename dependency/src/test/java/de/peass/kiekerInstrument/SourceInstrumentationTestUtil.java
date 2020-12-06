package de.peass.kiekerInstrument;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import de.peass.TestConstants;

public class SourceInstrumentationTestUtil {
   public static void initProject() throws IOException {
      TestConstants.CURRENT_FOLDER.mkdirs();
      
      for (String path : new String[] {"src/main/java/de/peass/C0_0.java", 
            "src/main/java/de/peass/C1_0.java", 
            "src/main/java/de/peass/AddRandomNumbers.java", 
            "src/test/java/de/peass/MainTest.java", 
            "pom.xml"}) {
         copyResource(path);
      }
   }
   
   public static File copyResource(String name) throws IOException {
      File testFile = new File(TestConstants.CURRENT_FOLDER, name);
      if (!testFile.getParentFile().exists()) {
         testFile.getParentFile().mkdirs();
      }
      final URL exampleClass = TestSourceInstrumentation.class.getResource("/sourceInstrumentation/project_2/" + name);
      FileUtils.copyURLToFile(exampleClass, testFile);
      return testFile;
   }
}
