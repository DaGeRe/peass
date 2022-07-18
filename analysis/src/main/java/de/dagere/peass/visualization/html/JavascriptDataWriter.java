package de.dagere.peass.visualization.html;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.visualization.GraphNode;

public class JavascriptDataWriter {

   private static final int CHARACTER_SIZE = 7;

   private static final Logger LOG = LogManager.getLogger(JavascriptDataWriter.class);

   private final File propertyFolder;
   private final GraphNode root;

   public JavascriptDataWriter(final File propertyFolder, final GraphNode root) {
      this.propertyFolder = propertyFolder;
      this.root = root;
   }

   public void writeJS(final CauseSearchData data, final File output, final String jsName, final GraphNode converted) throws IOException, JsonProcessingException {
      File outputJS = new File(output.getParentFile(), jsName);
      try (final BufferedWriter fileWriter = new BufferedWriter(new FileWriter(outputJS))) {
         fileWriter.write("if (document.getElementById('testcaseDiv') != null) { \n   document.getElementById('testcaseDiv').innerHTML=\"Commit: <a href='"
               + "javascript:fallbackCopyTextToClipboard(\\\"-commit " + data.getMeasurementConfig().getFixedCommitConfig().getCommit() +
               " -test " + data.getTestcase() + "\\\")'>"
               + data.getMeasurementConfig().getFixedCommitConfig().getCommit() + "</a><br>");
         fileWriter.write("Test Case: " + data.getTestcase() + "<br>\";\n");
         writeDashboardLink(data, fileWriter);
         fileWriter.write("}\n");

         fileWriter.write("\n");
         if (propertyFolder != null) {
            final File methodSourceFolder = new File(propertyFolder, "methods");
            final SourceWriter writer = new SourceWriter(root, fileWriter, methodSourceFolder, data.getMeasurementConfig().getFixedCommitConfig().getCommit());
            writer.writeSources();
         }
         writeColoredTree(fileWriter);

         writeTreeDivSizes(fileWriter);

         fileWriter.write("var kopemeData = [\n");
         fileWriter.write(Constants.OBJECTMAPPER.writeValueAsString(converted));
         fileWriter.write("];\n");
      }
   }

   private void writeDashboardLink(final CauseSearchData data, final BufferedWriter fileWriter) throws IOException {
      fileWriter.write("   if (typeof jenkins !== 'undefined') {\n      document.getElementById('testcaseDiv').innerHTML+=");
      fileWriter.write("\"<p class='button-wrap'>"
            + "<a role='button' href='dashboard?call=overall&ess=-1' target='parent'>Inspect Overall Measurement</a>"
            + "</p>\";\n");
      fileWriter.write("   } else {\n");
      System.out.println(data.getTestcase());
      fileWriter.write("   document.getElementById('testcaseDiv').innerHTML+=\"<a href='" + data.getTestcase().substring(data.getTestcase().indexOf('#') + 1)
            + "_dashboard.html?call=overall&ess=-1' target='parent'>Inspect Overall Measurement</a>\";\n");
      fileWriter.write("   }\n");
   }

   private void writeTreeDivSizes(final BufferedWriter fileWriter) throws IOException {
      final int nodeHeight = getHeight(root);
      final int nodeDepthWidth = getDepth(root);

      // final int width = 500 * (nodeDepthWidth + 1);
      final int height = 35 * (nodeHeight + 1);
      final int left = CHARACTER_SIZE * root.getName().length();
      fileWriter.write("// ************** Generate the tree diagram   *****************\n" +
            "var margin = {top: 20, right: 120, bottom: 20, left: " + left + "},\n" +
            "   width = " + nodeDepthWidth + "- margin.right - margin.left,\n" +
            "   height = " + height + " - margin.top - margin.bottom;\n");
      LOG.info("Width: {} Height: {} Left: {}", nodeDepthWidth, height, left);
   }

   private void writeColoredTree(final BufferedWriter fileWriter) throws IOException, JsonProcessingException {
      fileWriter.write("var treeData = [\n");
      fileWriter.write(Constants.OBJECTMAPPER.writeValueAsString(root));
      fileWriter.write("];\n");
   }

   private int getDepth(final GraphNode root) {
      int thisNodeLength = 60 + root.getCall().length() * CHARACTER_SIZE;
      int currentMaxLength = thisNodeLength;
      for (final GraphNode child : root.getChildren()) {
         currentMaxLength = Math.max(currentMaxLength, getDepth(child) + thisNodeLength);
      }
      return currentMaxLength;
   }

   private int getHeight(final GraphNode root) {
      int height = root.getChildren().size();
      for (final GraphNode child : root.getChildren()) {
         height = Math.max(height, getHeight(child)) + 1;
      }
      return height;
   }
}
