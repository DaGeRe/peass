package de.peass.visualization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

class SourceWriter {
   final GraphNode root;
   final BufferedWriter fileWriter;
   File sourceFolder;

   public SourceWriter(final GraphNode root, final BufferedWriter fileWriter, final File sourceFolder) {
      super();
      this.root = root;
      this.fileWriter = fileWriter;
      this.sourceFolder = sourceFolder;
   }

   void writeSources() throws IOException {
      fileWriter.write("var source = {");
      final Map<String, String> nameSourceMap = new HashMap<>();
      readSources(nameSourceMap, root);
      for (final Map.Entry<String, String> sources : nameSourceMap.entrySet()) {
         fileWriter.write("\"" + sources.getKey() + "\": `" + sources.getValue() + "`,");
      }
      fileWriter.write("};\n");
   }

   private void readSources(final Map<String, String> nameSourceMap, final GraphNode parent) throws IOException {
      getNodeSource(nameSourceMap, parent);
      for (final GraphNode node : parent.getChildren()) {
         readSources(nameSourceMap, node);
      }
   }

   private void getNodeSource(final Map<String, String> nameSourceMap, final GraphNode node) throws IOException {
      final int begin = node.getCall().lastIndexOf('.') + 1;
      final File test = new File(sourceFolder, node.getCall().substring(begin) + "_diff.txt");
      if (test.exists()) {
         final String source = FileUtils.readFileToString(test, Charset.defaultCharset());
         nameSourceMap.put(node.getCall(), source);
         final File main = new File(sourceFolder, node.getCall().substring(begin) + "_main.txt");
         final File old = new File(sourceFolder, node.getCall().substring(begin) + "_old.txt");
         if (main.exists() && old.exists()) {
            node.setHasSourceChange(true);
         }
      }
   }
}