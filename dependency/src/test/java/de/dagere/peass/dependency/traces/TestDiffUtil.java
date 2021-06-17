package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class TestDiffUtil {
   
   private static final File DIFF_FILE = new File("target/diff.txt");
   
   @Test
   public void testNoDiff() throws IOException {
      File file1 = new File("target/test1.txt");
      File file2 = new File("target/test2.txt");
      
      buildSameContents(file1, file2);
      
      DiffUtil.generateDiffFile(DIFF_FILE, Arrays.asList(file1, file2), "");
      
      String diff = FileUtils.readFileToString(DIFF_FILE, StandardCharsets.UTF_8);
      MatcherAssert.assertThat(diff, Matchers.not(Matchers.containsString("|")));
   }
   
   @Test
   public void testDiff() throws IOException {
      File file1 = new File("target/test1.txt");
      File file2 = new File("target/test2.txt");
      
      buildDifferentContents(file1, file2);
      
      DiffUtil.generateDiffFile(DIFF_FILE, Arrays.asList(file1, file2), "");
      
      String diff = FileUtils.readFileToString(DIFF_FILE, StandardCharsets.UTF_8);
      MatcherAssert.assertThat(diff, Matchers.containsString("|"));
   }

   @Test
   public void testNameWithSpace() throws IOException {
      File file1 = new File("target/test file1.txt");
      File file2 = new File("target/test file2.txt");
      
      buildSameContents(file1, file2);
      
      DiffUtil.generateDiffFile(DIFF_FILE, Arrays.asList(file1, file2), "");
      
      String diff = FileUtils.readFileToString(DIFF_FILE, StandardCharsets.UTF_8);
      MatcherAssert.assertThat(diff, Matchers.not(Matchers.containsString("|")));
   }
   
   @Test
   public void testNameWithSpaceOnlyDiff() throws IOException {
      File file1 = new File("target/test file1.txt");
      File file2 = new File("target/test file2.txt");
      
      buildSameContents(file1, file2);
      
      String hasDiff = DiffUtil.getDiff(file1, file2);
      
      MatcherAssert.assertThat(hasDiff, Matchers.hasLength(0));
   }

   private void buildDifferentContents(final File file1, final File file2) throws IOException {
      FileUtils.write(file1, "Line 1\nLine 2", StandardCharsets.UTF_8);
      FileUtils.write(file2, "Line 1\nLine 3", StandardCharsets.UTF_8);
   }
   
   private void buildSameContents(final File file1, final File file2) throws IOException {
      FileUtils.write(file1, "Line 1\nLine 2", StandardCharsets.UTF_8);
      FileUtils.write(file2, "Line 1\nLine 2", StandardCharsets.UTF_8);
   }
}
