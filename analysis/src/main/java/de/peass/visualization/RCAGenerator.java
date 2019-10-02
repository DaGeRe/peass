package de.peass.visualization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.measurement.searchcause.data.CauseSearchData;
import de.peass.measurement.searchcause.serialization.MeasuredNode;
import de.peass.measurement.searchcause.serialization.MeasuredValues;
import de.peass.utils.Constants;

public class RCAGenerator {

   private static final Logger LOG = LogManager.getLogger(RCAGenerator.class);

   private final File source, details, destFolder;
   private final CauseSearchData data;
   private File propertyFolder;

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
      final MeasuredNode parent = data.getNodes();
      final String longestPrefix = getLongestPrefix(parent);
      setPrefix(parent, longestPrefix);
      LOG.info("Prefix: {}", longestPrefix);

      final MeasuredNode measured = data.getNodes();
      final Node root = new Node();
      System.out.println(measured.getCall());
      setNodeData(measured, root);

      processNode(measured, root);

      writeHTML(root, data);
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

   private void setPrefix(final MeasuredNode parent, final String longestPrefix) {
      parent.setCall(parent.getCall().substring(longestPrefix.length()));
      for (final MeasuredNode node : parent.getChilds()) {
         setPrefix(node, longestPrefix);
      }
   }

   private String getLongestPrefix(final MeasuredNode parent) {
      String longestPrefix = parent.getCall();
      for (final MeasuredNode node : parent.getChilds()) {
         final String clazz = node.getCall().substring(0, node.getCall().lastIndexOf('.') + 1);
         longestPrefix = StringUtils.getCommonPrefix(longestPrefix, getLongestPrefix(node), clazz);
      }
      return longestPrefix;
   }

   private void writeHTML(final Node root, final CauseSearchData data) throws IOException, JsonProcessingException, FileNotFoundException {
      final File output = new File(destFolder, data.getTestcase() + ".html");
      try (final BufferedWriter fileWriter = new BufferedWriter(new FileWriter(output))) {
         final HTMLEnvironmentGenerator htmlGenerator = new HTMLEnvironmentGenerator(fileWriter);
         fileWriter.write("<!DOCTYPE html>\n");
         htmlGenerator.writeHTML("visualization/HeaderOfHTML.html");
         htmlGenerator.writeInfoDivs(data);

         fileWriter.write("<div id='tree' style='position: absolute; top: 150px; right: 5px; left: 5px; bottom: 335px; "
               + "overflow: scroll; "
               + "border: 2px solid blue; border-radius: 1em 1em 1em 1em;'> </div>\n");

         fileWriter.write("<script>\n" +
               "var treeData = [\n");
         fileWriter.write(Constants.OBJECTMAPPER.writeValueAsString(root));
         fileWriter.write("];\n");

         if (propertyFolder != null) {
            final File sourceFolder = new File(propertyFolder, "methods" + File.separator + data.getVersion());
            new SourceWriter(data, fileWriter, sourceFolder).writeSources();
         }

         final int nodeHeight = getHeight(root);
         final int nodeDepth = getDepth(root);

         final int width = 500 * nodeDepth;
         final int height = 35 * nodeHeight;
         final int left = 10 * root.getName().length();
         fileWriter.write("// ************** Generate the tree diagram   *****************\n" +
               "var margin = {top: 20, right: 120, bottom: 20, left: " + left + "},\n" +
               "   width = " + width + "- margin.right - margin.left,\n" +
               "   height = " + height + " - margin.top - margin.bottom;");
         fileWriter.write("</script>\n");
         LOG.info("Width: {} Height: {} Left: {}", width, height, left);
         htmlGenerator.writeHTML("visualization/RestOfHTML.html");
         fileWriter.flush();
      }
   }

   class SourceWriter {
      final CauseSearchData data;
      final BufferedWriter fileWriter;
      File sourceFolder;

      public SourceWriter(final CauseSearchData data, final BufferedWriter fileWriter, final File sourceFolder) {
         super();
         this.data = data;
         this.fileWriter = fileWriter;
         this.sourceFolder = sourceFolder;
      }

      private void writeSources() throws IOException {
         fileWriter.write("var source = {");
         final Map<String, String> nameSourceMap = new HashMap<>();
         readSources(nameSourceMap, data.getNodes());
         for (final Map.Entry<String, String> sources : nameSourceMap.entrySet()) {
            fileWriter.write("\"" + sources.getKey() + "\": `" + sources.getValue() + "`,");
         }
         fileWriter.write("};\n");
      }

      private void readSources(final Map<String, String> nameSourceMap, final MeasuredNode parent) throws IOException {
         getNodeSource(nameSourceMap, parent);
         for (final MeasuredNode node : parent.getChilds()) {
            readSources(nameSourceMap, node);
         }
      }

      private void getNodeSource(final Map<String, String> nameSourceMap, final MeasuredNode node) throws IOException {
         final int begin = node.getCall().lastIndexOf('.') + 1;
         final File test = new File(sourceFolder, node.getCall().substring(begin) + "_diff.txt");
         if (test.exists()) {
            final String source = FileUtils.readFileToString(test, Charset.defaultCharset());
            nameSourceMap.put(node.getCall(), source);
         }
      }
   }

   private int getDepth(final Node root) {
      int depth = 0;
      for (final Node child : root.getChildren()) {
         depth = Math.max(depth, getDepth(child)) + 1;
      }
      return depth;
   }

   private int getHeight(final Node root) {
      int height = root.getChildren().size();
      for (final Node child : root.getChildren()) {
         height = Math.max(height, getHeight(child)) + 1;
      }
      return height;
   }

   private void processNode(final MeasuredNode measuredParent, final Node parent) {
      for (final MeasuredNode measuredChild : measuredParent.getChilds()) {
         final Node newChild = new Node();
         newChild.setParent(measuredParent.getCall());
         setNodeData(measuredChild, newChild);

         parent.getChildren().add(newChild);
         processNode(measuredChild, newChild);
      }
   }

   private void setNodeData(final MeasuredNode measuredChild, final Node newChild) {
      newChild.setName(measuredChild.getCall());
      setNodeColor(measuredChild, newChild);
      if (measuredChild.getValues() != null) {
         final double[] values = getValueArray(measuredChild.getValues());
         newChild.setValues(values);
      }
      if (measuredChild.getValuesPredecessor() != null) {
         final double[] values = getValueArray(measuredChild.getValuesPredecessor());
         newChild.setValuesPredecessor(values);
      }
   }

   private double[] getValueArray(final MeasuredValues measured) {
      final double[] values = new double[measured.getValues().size()];
      for (int i = 0; i < values.length; i++) {
         values[i] = measured.getValues().get(i).get(0);
      }
      return values;
   }

   private void setNodeColor(final MeasuredNode measuredNode, final Node graphNode) {
      // System.out.println(measuredNode.getStatistic().getMeanCurrent() + " " + (measuredNode.getStatistic().getMeanCurrent() > 100));
      graphNode.setStatistic(measuredNode.getStatistic());
      if (measuredNode.isChange(data.getConfig().getType2error()) && measuredNode.getStatistic().getMeanCurrent() > 100 && measuredNode.getStatistic().getMeanOld() > 100) {
         if (measuredNode.getStatistic().getTvalue() < 0) {
            graphNode.setColor("#FF0000");
         } else {
            graphNode.setColor("#00FF00");
         }
      } else {
         graphNode.setColor("#FFFFFF");
      }
   }
}
