package de.peass.visualization;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class TestVisualizer {
   
   @Test
   public void testBasicVisualization() throws Exception {
      File dataFile = new File("src/test/resources/visualization/project_3_peass");
      final File resultFolder = new File("target/current_visualization");
      new VisualizeRCA(new File[] {dataFile}, resultFolder).call();
      
      File resultfile = new File(resultFolder, "9177678d505bfacb64a95c2271fb03b1e18475a8/de.peass.MainTest#testMe.html");
      Assert.assertTrue(resultfile.exists());
      
      String text = new String(Files.readAllBytes(resultfile.toPath()), StandardCharsets.UTF_8);

      MatcherAssert.assertThat(text, Matchers.containsString("de.peass.MainTest#testMe"));
      MatcherAssert.assertThat(text, Matchers.containsString("Performance Measurement Tree"));
   }
}
