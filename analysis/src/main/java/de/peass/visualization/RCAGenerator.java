package de.peass.visualization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

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
      super();
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
      final File output = new File(destFolder, data.getTestcase() + ".html");
      try (final BufferedWriter fileWriter = new BufferedWriter(new FileWriter(output))) {
         final HTMLEnvironmentGenerator htmlGenerator = new HTMLEnvironmentGenerator(fileWriter);
         fileWriter.write("<!DOCTYPE html>\n");
         htmlGenerator.writeHTML("visualization/HeaderOfHTML.html");
         htmlGenerator.writeInfoDivs(data);
         
         writeTreeDiv(fileWriter);
         
         fileWriter.write("<script>\n");
         if (propertyFolder != null) {
            final File sourceFolder = new File(propertyFolder, "methods" + File.separator + data.getMeasurementConfig().getVersion());
            new SourceWriter(root, fileWriter, sourceFolder).writeSources();
         }
         writeColoredTree(root, fileWriter);

         writeTreeDivSizes(root, fileWriter);
         fileWriter.write("</script>\n");
         
         htmlGenerator.writeHTML("visualization/RestOfHTML.html");
         fileWriter.flush();
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

   private void writeTreeDiv(final BufferedWriter fileWriter) throws IOException {
      fileWriter.write("<div id='tree' style='position: absolute; top: 150px; right: 5px; left: 5px; bottom: 335px; "
            + "overflow: scroll; "
            + "border: 2px solid blue; border-radius: 1em 1em 1em 1em;'> </div>\n");
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
