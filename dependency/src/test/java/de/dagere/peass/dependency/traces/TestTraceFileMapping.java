package de.dagere.peass.dependency.traces;

import java.io.File;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Test;

import de.dagere.nodeDiffGenerator.data.TestMethodCall;

public class TestTraceFileMapping {

   private static final TestMethodCall TEST_A = new TestMethodCall("TestA", "methodA");
   private static final TestMethodCall TEST_B = new TestMethodCall("TestB", "methodB");

   @Test
   public void testBasicInsertion() {
      TraceFileMapping mapping = new TraceFileMapping();

      mapping.addTraceFile(TEST_A, new File("1A"));
      mapping.addTraceFile(TEST_B, new File("1B"));
      mapping.addTraceFile(TEST_A, new File("2A"));
      mapping.addTraceFile(TEST_B, new File("2B"));

      List<File> aFiles = mapping.getTestcaseMap(TEST_A);
      MatcherAssert.assertThat(aFiles, IsIterableContaining.hasItems(new File("1A"), new File("2A")));

      List<File> bFiles = mapping.getTestcaseMap(TEST_B);
      MatcherAssert.assertThat(bFiles, IsIterableContaining.hasItems(new File("1B"), new File("2B")));
   }

   @Test
   public void testMoreVersions() {
      TraceFileMapping mapping = new TraceFileMapping();

      mapping.addTraceFile(TEST_A, new File("1A"));
      mapping.addTraceFile(TEST_B, new File("1B"));
      mapping.addTraceFile(TEST_A, new File("2A"));
      mapping.addTraceFile(TEST_B, new File("2B"));
      mapping.addTraceFile(TEST_A, new File("3A"));
      mapping.addTraceFile(TEST_B, new File("3B"));

      List<File> aFiles = mapping.getTestcaseMap(TEST_A);
      MatcherAssert.assertThat(aFiles, IsIterableContaining.hasItems(new File("2A"), new File("3A")));
      MatcherAssert.assertThat(aFiles, Matchers.hasSize(2));

      List<File> bFiles = mapping.getTestcaseMap(TEST_B);
      MatcherAssert.assertThat(bFiles, IsIterableContaining.hasItems(new File("2B"), new File("3B")));
      MatcherAssert.assertThat(bFiles, Matchers.hasSize(2));
   }
}
