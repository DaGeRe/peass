package de.peass.measurement;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import de.peass.dependencyprocessors.ProgressWriter;

public class TestProgressWriter {
   
   @Test
   public void testProgressWriter() throws IOException {
      final File testfile = new File("target/test.txt");
      ProgressWriter pw = new ProgressWriter(testfile, 200);
      
      pw.write(40, 1);
      pw.write(60, 2);
      pw.write(40, 3);
      pw.write(60, 4);
      
      String text = new String(Files.readAllBytes(testfile.toPath()), StandardCharsets.UTF_8);
      MatcherAssert.assertThat(text, Matchers.containsString("Remaining: 2h 12"));
      MatcherAssert.assertThat(text, Matchers.containsString("Remaining: 2h 43"));
   }
   
}
