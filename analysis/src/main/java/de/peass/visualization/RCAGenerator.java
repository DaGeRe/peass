package de.peass.visualization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.measurement.searchcause.data.CauseSearchData;
import de.peass.measurement.searchcause.data.MeasuredNode;
import de.peass.utils.Constants;

public class RCAGenerator {
   
   private static final Logger LOG = LogManager.getLogger(RCAGenerator.class);
   
   private final File source, destFolder;
   
   
   public RCAGenerator(final File source, final File destFolder) {
      super();
      this.source = source;
      this.destFolder = destFolder;
   }

   public void createVisualization() throws IOException, JsonParseException, JsonMappingException, JsonProcessingException, FileNotFoundException {
      final CauseSearchData data = Constants.OBJECTMAPPER.readValue(source, CauseSearchData.class);

      final MeasuredNode parent = data.getNodes();
      final String longestPrefix = getLongestPrefix(parent);
      setPrefix(parent, longestPrefix);
      LOG.info("Prefix: {}", longestPrefix);

      final MeasuredNode measured = data.getNodes();
      final Node root = new Node();
      System.out.println(measured.getCall());
      root.setName(measured.getCall());
      setNodeColor(measured, root);

      processNode(measured, root);

      writeHTML(root, data);
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
      final BufferedWriter fileWriter = new BufferedWriter(new FileWriter(output));

      fileWriter.write("<!DOCTYPE html>\n" +
            "<script>\n" +
            "var treeData = [\n");
      fileWriter.write(Constants.OBJECTMAPPER.writeValueAsString(root));
      fileWriter.write("];\n");

      int nodeHeight = 0;
      for (final Node child : root.getChildren()) {
         nodeHeight += child.getChildren().size();
      }
      final int nodeDepth = getDepth(root);

      final int width = 400 * nodeDepth;
      final int height = 100 * nodeHeight;
      final int left = 10 * root.getName().length();
      fileWriter.write("// ************** Generate the tree diagram   *****************\n" +
            "var margin = {top: 20, right: 120, bottom: 20, left: " + left + "},\n" +
            "   width = " + width + "- margin.right - margin.left,\n" +
            "   height = 300 - margin.top - margin.bottom;");
      fileWriter.write("</script>\n");
      LOG.info("Width: {} Height: {} Left: {}", width, height, left);
      final InputStream htmlStream = GenerateRCAHTML.class.getClassLoader().getResourceAsStream("visualization/RestOfHTML.html");
      final BufferedReader reader = new BufferedReader(new InputStreamReader(htmlStream));

      String line;
      while ((line = reader.readLine()) != null) {
         fileWriter.write(line + "\n");
      }
      fileWriter.flush();
      fileWriter.close();
   }

   private int getDepth(final Node root) {
      int depth = 0;
      for (final Node child : root.getChildren()) {
         depth = Math.max(depth, getDepth(child)) + 1;
      }
      return depth;
   }

   private void processNode(final MeasuredNode measuredParent, final Node parent) {
      for (final MeasuredNode measuredChild : measuredParent.getChilds()) {
         final Node newChild = new Node();
         newChild.setName(measuredChild.getCall());
         newChild.setParent(measuredParent.getCall());
         setNodeColor(measuredChild, newChild);
         parent.getChildren().add(newChild);
         processNode(measuredChild, newChild);
      }
   }

   private void setNodeColor(final MeasuredNode measuredNode, final Node graphNode) {
      if (measuredNode.getStatistic().isChange()) {
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
