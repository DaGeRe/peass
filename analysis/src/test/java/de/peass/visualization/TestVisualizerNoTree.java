package de.peass.visualization;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

public class TestVisualizerNoTree {
   
   @Ignore
   @Test
   public void testBasicVisualization() throws Exception {
      File dataFile = new File("src/test/resources/visualization/demo_peass");
      final File resultFolder = new File("target/current_visualization");
      new VisualizeRCA(new File[] {dataFile}, resultFolder).call();
      
      File resultfile = new File(resultFolder, "f347830fce35e02c0f76e15076658f2f2a2ee116/de.peass.DemoClass#method0.html");
      Assert.assertTrue(resultfile.exists());
      
      String text = new String(Files.readAllBytes(resultfile.toPath()), StandardCharsets.UTF_8);

      MatcherAssert.assertThat(text, Matchers.containsString("de.peass.MainTest#testMe"));
      MatcherAssert.assertThat(text, Matchers.containsString("Performance Measurement Tree"));
   }
}
