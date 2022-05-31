package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;

public class TestDiffFileGenerator {

   private final File diffFolder = new File("target/test-diffs");
   private final File rawFileFolder = new File("target/raw-files");

   private final TestCase test = new TestCase("de.dagere.peass.ExampleTest#test");

   @BeforeEach
   public void init() throws IOException {
      diffFolder.mkdir();
      FileUtils.cleanDirectory(diffFolder);
      
      rawFileFolder.mkdir();
      FileUtils.cleanDirectory(rawFileFolder);
   }

   @Test
   public void testTxtDiffGeneration() throws IOException {
      TraceFileMapping mapping = generateFiles(test, TraceFileManager.TXT_ENDING);

      DiffFileGenerator generator = new DiffFileGenerator(diffFolder);
      VersionStaticSelection staticSelection = new VersionStaticSelection();
      staticSelection.getChangedClazzes().put(new ChangedEntity("de.SomeClass"), new TestSet(test));

      generator.generateAllDiffs("000002", staticSelection, mapping, new ExecutionData());

      File expectedResultFile = new File(diffFolder, "ExampleTest#test.txt");
      Assert.assertTrue(expectedResultFile.exists());

      File expectedResultFileNoComment = new File(diffFolder, "ExampleTest#test" + OneTraceGenerator.NOCOMMENT + ".txt");
      Assert.assertTrue(expectedResultFileNoComment.exists());

      File expectedResultFileMethod = new File(diffFolder, "ExampleTest#test" + OneTraceGenerator.METHOD + ".txt");
      Assert.assertTrue(expectedResultFileMethod.exists());

      File expectedResultFileMethodExpanded = new File(diffFolder, "ExampleTest#test" + OneTraceGenerator.METHOD_EXPANDED + ".txt");
      Assert.assertTrue(expectedResultFileMethodExpanded.exists());
   }

   @Test
   public void testZipDiffGeneration() throws IOException {
      TraceFileMapping mapping = generateFiles(test, TraceFileManager.ZIP_ENDING);
      
      DiffFileGenerator generator = new DiffFileGenerator(diffFolder);
      VersionStaticSelection staticSelection = new VersionStaticSelection();
      staticSelection.getChangedClazzes().put(new ChangedEntity("de.SomeClass"), new TestSet(test));

      generator.generateAllDiffs("000002", staticSelection, mapping, new ExecutionData());

      File expectedResultFile = new File(diffFolder, "ExampleTest#test.zip");
      Assert.assertTrue(expectedResultFile.exists());

      File expectedResultFileNoComment = new File(diffFolder, "ExampleTest#test" + OneTraceGenerator.NOCOMMENT + ".zip");
      Assert.assertTrue(expectedResultFileNoComment.exists());

      File expectedResultFileMethod = new File(diffFolder, "ExampleTest#test" + OneTraceGenerator.METHOD + ".zip");
      Assert.assertTrue(expectedResultFileMethod.exists());

      File expectedResultFileMethodExpanded = new File(diffFolder, "ExampleTest#test" + OneTraceGenerator.METHOD_EXPANDED + ".zip");
      Assert.assertTrue(expectedResultFileMethodExpanded.exists());

   }

   private TraceFileMapping generateFiles(TestCase test, String ending) throws IOException {
      TraceFileMapping mapping = new TraceFileMapping();

      File version1trace = new File(rawFileFolder, "version1" + ending);
      write(version1trace, "de.dagere.peass.ExampleTest#test\nSomeSource");
      mapping.addTraceFile(test, version1trace);

      File version2trace = new File(rawFileFolder, "version2" + ending);
      write(version2trace, "de.dagere.peass.ExampleTest#test\nChangedSource");
      mapping.addTraceFile(test, version2trace);

      for (String variant : new String[] { OneTraceGenerator.NOCOMMENT, OneTraceGenerator.METHOD, OneTraceGenerator.METHOD_EXPANDED, OneTraceGenerator.SUMMARY }) {
         File version1variantFile = new File(rawFileFolder, "version1" + variant + ending);
         write(version1variantFile, "de.dagere.peass.ExampleTest#test\nSomeSource");

         File version2variantFile = new File(rawFileFolder, "version2" + variant + ending);
         write(version2variantFile, "de.dagere.peass.ExampleTest#test\nChangedSource");
      }

      return mapping;
   }

   private void write(File goal, String content) throws IOException {
      if (goal.getName().endsWith(TraceFileManager.TXT_ENDING)) {
         FileUtils.writeStringToFile(goal, content, StandardCharsets.UTF_8);
      } else if (goal.getName().endsWith(TraceFileManager.ZIP_ENDING)) {
         ByteBuffer bytebuffer = StandardCharsets.UTF_8.encode(content);

         try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(goal));
               WritableByteChannel channel = Channels.newChannel(zipStream)) {
            ZipEntry entry = new ZipEntry("trace.txt");
            zipStream.putNextEntry(entry);
            channel.write(bytebuffer);
         }
      }
   }
}
