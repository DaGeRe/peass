package de.peass.visualization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.utils.Constants;

public class RCAGenerator {

   private static final Logger LOG = LogManager.getLogger(RCAGenerator.class);

   private final File source, details, destFolder;
   private final CauseSearchData data;
   private File propertyFolder;

   private CallTreeNode rootPredecessor, rootVersion;

   public RCAGenerator(final File source, final File destFolder) throws JsonParseException, JsonMappingException, IOException {
      this.source = source;
      details = new File(source.getParentFile(), "details" + File.separator + source.getName());
      this.destFolder = destFolder;
      data = readData();
   }

   public void setPropertyFolder(final File propertyFolder) {
      this.propertyFolder = propertyFolder;
   }

   public void createVisualization() throws IOException, JsonParseException, JsonMappingException, JsonProcessingException, FileNotFoundException {
      final NodePreparator preparator = new NodePreparator(rootPredecessor, rootVersion, data);
      preparator.prepare();
      final GraphNode rootNode = preparator.getRootNode();
      writeHTML(rootNode, data);
   }

   private CauseSearchData readData() throws IOException, JsonParseException, JsonMappingException {
      final CauseSearchData data;
      if (details.exists()) {
         data = Constants.OBJECTMAPPER.readValue(details, CauseSearchData.class);
      } else {
         data = Constants.OBJECTMAPPER.readValue(source, CauseSearchData.class);
      }
      return data;
   }

   private void writeHTML(final GraphNode root, final CauseSearchData data) throws IOException, JsonProcessingException, FileNotFoundException {
      final File output = getOutputHTML(data);
      final String jsName = output.getName().replace(".html", ".js").replaceAll("#", "_");
      try (final BufferedWriter fileWriter = new BufferedWriter(new FileWriter(output))) {
         final HTMLEnvironmentGenerator htmlGenerator = new HTMLEnvironmentGenerator(fileWriter);
         fileWriter.write("<!DOCTYPE html>\n");
         htmlGenerator.writeHTML("visualization/HeaderOfHTML.html");

         fileWriter.write("<script src='"+jsName+"'></script>\n");

         htmlGenerator.writeHTML("visualization/RestOfHTML.html");
         fileWriter.flush();
      }
      
      File outputJS = new File(output.getParentFile(), jsName);
      try (final BufferedWriter fileWriter = new BufferedWriter(new FileWriter(outputJS))) {
         fileWriter.write("document.getElementById('testcaseDiv').innerHTML=\"Version: <a href='"
               + "javascript:fallbackCopyTextToClipboard(\\\"-version " + data.getMeasurementConfig().getVersion() + 
               " -test " + data.getTestcase() + "\\\")'>"
               + data.getMeasurementConfig().getVersion() + "</a><br>");
         fileWriter.write("Test Case: " + data.getTestcase() + "<br>\";\n");
         fileWriter.write("\n");
         if (propertyFolder != null) {
            final File sourceFolder = new File(propertyFolder, "methods" + File.separator + data.getMeasurementConfig().getVersion());
            final SourceWriter writer = new SourceWriter(root, fileWriter, sourceFolder);
            writer.writeSources();
         }
         writeColoredTree(root, fileWriter);

         writeTreeDivSizes(root, fileWriter);
      }
      
   }

   private File getOutputHTML(final CauseSearchData data) {
      final File output;
      if (destFolder.getName().equals(data.getMeasurementConfig().getVersion())) {
         output = new File(destFolder, data.getTestcase() + ".html");
         copyResources(destFolder);
      } else {
         File versionFolder = new File(destFolder, data.getMeasurementConfig().getVersion());
         copyResources(versionFolder);
         output = new File(versionFolder, data.getTestcase() + ".html");
      }
      output.getParentFile().mkdirs();
      return output;
   }

   private void copyResources(File folder) {
      try {
         URL difflib = RCAGenerator.class.getClassLoader().getResource("visualization/difflib.js");
         FileUtils.copyURLToFile(difflib, new File(folder, "difflib.js"));
         URL diffview = RCAGenerator.class.getClassLoader().getResource("visualization/diffview.js");
         FileUtils.copyURLToFile(diffview, new File(folder, "diffview.js"));
         URL diffviewcss = RCAGenerator.class.getClassLoader().getResource("visualization/diffview.css");
         FileUtils.copyURLToFile(diffviewcss, new File(folder, "diffview.css"));
         URL peassCode = RCAGenerator.class.getClassLoader().getResource("visualization/peass-visualization-code.js");
         FileUtils.copyURLToFile(peassCode, new File(folder, "peass-visualization-code.js"));
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   private void writeTreeDivSizes(final GraphNode root, final BufferedWriter fileWriter) throws IOException {
      final int nodeHeight = getHeight(root);
      final int nodeDepth = getDepth(root);

      final int width = 500 * nodeDepth;
      final int height = 35 * nodeHeight;
      final int left = 10 * root.getName().length();
      fileWriter.write("// ************** Generate the tree diagram   *****************\n" +
            "var margin = {top: 20, right: 120, bottom: 20, left: " + left + "},\n" +
            "   width = " + width + "- margin.right - margin.left,\n" +
            "   height = " + height + " - margin.top - margin.bottom;");
      LOG.info("Width: {} Height: {} Left: {}", width, height, left);
   }

   private void writeColoredTree(final GraphNode root, final BufferedWriter fileWriter) throws IOException, JsonProcessingException {
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

   public CauseSearchData getData() {
      return data;
   }

   public void setFullTree(final CallTreeNode rootPredecessor, final CallTreeNode rootVersion) {
      this.rootPredecessor = rootPredecessor;
      this.rootVersion = rootVersion;
   }
}
