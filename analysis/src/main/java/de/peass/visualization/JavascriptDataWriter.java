package de.peass.visualization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.utils.Constants;

public class JavascriptDataWriter {

   private static final Logger LOG = LogManager.getLogger(JavascriptDataWriter.class);

   private final File propertyFolder;
   private final GraphNode root;

   public JavascriptDataWriter(final File propertyFolder, final GraphNode root) {
      this.propertyFolder = propertyFolder;
      this.root = root;
   }

   public void writeJS(final CauseSearchData data, final File output, final String jsName, final KoPeMeTreeConverter converter) throws IOException, JsonProcessingException {
      File outputJS = new File(output.getParentFile(), jsName);
      try (final BufferedWriter fileWriter = new BufferedWriter(new FileWriter(outputJS))) {
         fileWriter.write("if (document.getElementById('testcaseDiv') != null) \n   document.getElementById('testcaseDiv').innerHTML=\"Version: <a href='"
               + "javascript:fallbackCopyTextToClipboard(\\\"-version " + data.getMeasurementConfig().getVersion() +
               " -test " + data.getTestcase() + "\\\")'>"
               + data.getMeasurementConfig().getVersion() + "</a><br>");
         fileWriter.write("Test Case: " + data.getTestcase() + "<br>");
         fileWriter.write("<a href='" + data.getTestcase().replace("#", "_") + "_dashboard.html?call=overall&ess=-1' target='parent'>Inspect Overall Measurement</a>");
         fileWriter.write("\";\n");
         
         fileWriter.write("\n");
         if (propertyFolder != null) {
            final File sourceFolder = new File(propertyFolder, "methods" + File.separator + data.getMeasurementConfig().getVersion());
            final SourceWriter writer = new SourceWriter(root, fileWriter, sourceFolder);
            writer.writeSources();
         }
         writeColoredTree(fileWriter);

         writeTreeDivSizes(fileWriter);

         fileWriter.write("var kopemeData = [\n");
         fileWriter.write(Constants.OBJECTMAPPER.writeValueAsString(converter.getData()));
         fileWriter.write("];\n");
      }
   }

   private void writeTreeDivSizes(final BufferedWriter fileWriter) throws IOException {
      final int nodeHeight = getHeight(root);
      final int nodeDepth = getDepth(root);

      final int width = 500 * nodeDepth;
      final int height = 35 * nodeHeight;
      final int left = 10 * root.getName().length();
      fileWriter.write("// ************** Generate the tree diagram   *****************\n" +
            "var margin = {top: 20, right: 120, bottom: 20, left: " + left + "},\n" +
            "   width = " + width + "- margin.right - margin.left,\n" +
            "   height = " + height + " - margin.top - margin.bottom;\n");
      LOG.info("Width: {} Height: {} Left: {}", width, height, left);
   }

   private void writeColoredTree(final BufferedWriter fileWriter) throws IOException, JsonProcessingException {
      fileWriter.write("var treeData = [\n");
      fileWriter.write(Constants.OBJECTMAPPER.writeValueAsString(root));
      fileWriter.write("];\n");
   }

   private int getDepth(final GraphNode root) {
      int depth = 0;
      for (final GraphNode child : root.getChildren()) {
         depth = Math.max(depth, getDepth(child)) + 1;
      }
      return depth;
   }

   private int getHeight(final GraphNode root) {
      int height = root.getChildren().size();
      for (final GraphNode child : root.getChildren()) {
         height = Math.max(height, getHeight(child)) + 1;
      }
      return height;
   }
}
