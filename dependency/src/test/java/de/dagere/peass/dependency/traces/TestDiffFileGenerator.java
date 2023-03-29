package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.nodeDiffGenerator.data.MethodCall;
import de.dagere.nodeDiffGenerator.data.TestMethodCall;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.traces.diff.DiffFileGenerator;
import de.dagere.peass.dependency.traces.diff.TraceFileUtil;

public class TestDiffFileGenerator {

   private final File diffFolder = new File("target/test-diffs");
   private final File rawFileFolder = new File("target/raw-files");

   private final TestMethodCall test = new TestMethodCall("de.dagere.peass.ExampleTest", "test");

   @BeforeEach
   public void init() throws IOException {
      diffFolder.mkdir();
      FileUtils.cleanDirectory(diffFolder);

      rawFileFolder.mkdir();
      FileUtils.cleanDirectory(rawFileFolder);
   }

   @Test
   public void testTxtDiffGeneration() throws IOException {
      TraceFileMapping mapping = DiffFileGeneraturTestUtil.generateFiles(rawFileFolder, test, TraceFileManager.TXT_ENDING, true, true);

      DiffFileGenerator generator = new DiffFileGenerator(diffFolder);
      CommitStaticSelection staticSelection = new CommitStaticSelection();
      staticSelection.getChangedClazzes().put(new MethodCall("de.SomeClass"), new TestSet(test));

      generator.generateAllDiffs("000002", staticSelection, mapping, new ExecutionData());

      File expectedResultFile = new File(diffFolder, "ExampleTest#test.txt");
      Assert.assertTrue(expectedResultFile.exists());

      File expectedResultFileNoComment = new File(diffFolder, "ExampleTest#test" + OneTraceGenerator.NOCOMMENT + ".txt");
      Assert.assertTrue(expectedResultFileNoComment.exists());

      checkResultDiff(expectedResultFileNoComment);

      File expectedResultFileMethod = new File(diffFolder, "ExampleTest#test" + OneTraceGenerator.METHOD + ".txt");
      Assert.assertTrue(expectedResultFileMethod.exists());

      File expectedResultFileMethodExpanded = new File(diffFolder, "ExampleTest#test" + OneTraceGenerator.METHOD_EXPANDED + ".txt");
      Assert.assertTrue(expectedResultFileMethodExpanded.exists());
   }

   @Test
   public void testZipDiffGeneration_different() throws IOException {
      TraceFileMapping mapping = DiffFileGeneraturTestUtil.generateFiles(rawFileFolder, test, TraceFileManager.ZIP_ENDING, true, true);

      DiffFileGenerator generator = new DiffFileGenerator(diffFolder);
      CommitStaticSelection staticSelection = new CommitStaticSelection();
      staticSelection.getChangedClazzes().put(new MethodCall("de.SomeClass"), new TestSet(test));

      generator.generateAllDiffs("000002", staticSelection, mapping, new ExecutionData());

      File expectedResultFile = new File(diffFolder, "ExampleTest#test.zip");
      Assert.assertTrue(expectedResultFile.exists());

      File expectedResultFileNoComment = new File(diffFolder, "ExampleTest#test" + OneTraceGenerator.NOCOMMENT + ".zip");
      Assert.assertTrue(expectedResultFileNoComment.exists());

      checkResultDiff(expectedResultFileNoComment);

      File expectedResultFileMethod = new File(diffFolder, "ExampleTest#test" + OneTraceGenerator.METHOD + ".zip");
      Assert.assertTrue(expectedResultFileMethod.exists());

      File expectedResultFileMethodExpanded = new File(diffFolder, "ExampleTest#test" + OneTraceGenerator.METHOD_EXPANDED + ".zip");
      Assert.assertTrue(expectedResultFileMethodExpanded.exists());
   }
   
   @Test
   public void testZipDiffGeneration_equal() throws IOException {
      TraceFileMapping mapping = DiffFileGeneraturTestUtil.generateFiles(rawFileFolder, test, TraceFileManager.ZIP_ENDING, true, false);

      DiffFileGenerator generator = new DiffFileGenerator(diffFolder);
      CommitStaticSelection staticSelection = new CommitStaticSelection();
      staticSelection.getChangedClazzes().put(new MethodCall("de.SomeClass"), new TestSet(test));

      generator.generateAllDiffs("000002", staticSelection, mapping, new ExecutionData());

      File expectedResultFile = new File(diffFolder, "ExampleTest#test.zip");
      Assert.assertFalse(expectedResultFile.exists());
   }

   @Test
   public void testNoExpandedZipDiffGeneration() throws IOException {
      TraceFileMapping mapping = DiffFileGeneraturTestUtil.generateFiles(rawFileFolder, test, TraceFileManager.ZIP_ENDING, false, true);

      DiffFileGenerator generator = new DiffFileGenerator(diffFolder);
      CommitStaticSelection staticSelection = new CommitStaticSelection();
      staticSelection.getChangedClazzes().put(new MethodCall("de.SomeClass"), new TestSet(test));

      generator.generateAllDiffs("000002", staticSelection, mapping, new ExecutionData());

      File expectedResultFile = new File(diffFolder, "ExampleTest#test.zip");
      Assert.assertTrue(expectedResultFile.exists());

      File expectedResultFileNoComment = new File(diffFolder, "ExampleTest#test" + OneTraceGenerator.NOCOMMENT + ".zip");
      Assert.assertTrue(expectedResultFileNoComment.exists());

      checkResultDiff(expectedResultFileNoComment);

      File expectedResultFileMethodExpanded = new File(diffFolder, "ExampleTest#test" + OneTraceGenerator.METHOD_EXPANDED + ".zip");
      Assert.assertFalse(expectedResultFileMethodExpanded.exists());
   }

   private void checkResultDiff(File expectedResultFileNoComment) throws IOException {
      List<String> text = TraceFileUtil.getText(expectedResultFileNoComment);
      MatcherAssert.assertThat(text.get(1), Matchers.containsString("SomeSource"));
      MatcherAssert.assertThat(text.get(1), Matchers.containsString("ChangedSource"));
      MatcherAssert.assertThat(text.get(1), Matchers.containsString("|"));
   }

}
