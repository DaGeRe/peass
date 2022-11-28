package de.dagere.peass.visualization.html;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.measurement.rca.data.BasicNode;
import de.dagere.peass.visualization.GraphNode;

public class JavascriptFunctions {
   
   private static final Logger LOG = LogManager.getLogger(JavascriptFunctions.class);
   
   private static final int CHARACTER_SIZE = 7;
   
   public static void writeTreeDivSizes(final BufferedWriter fileWriter, BasicNode root) throws IOException {
      final int nodeHeight = getHeight(root);
      final int nodeDepthWidth = getDepth(root);

      // final int width = 500 * (nodeDepthWidth + 1);
      final int height = 35 * (nodeHeight + 1);
      
      int nameLength = (root instanceof GraphNode) ? ((GraphNode)root).getName().length() : root.getCall().length();
      
      final int left = CHARACTER_SIZE * nameLength;
      fileWriter.write("// ************** Generate the tree diagram   *****************\n" +
            "var margin = {top: 20, right: 120, bottom: 20, left: " + left + "},\n" +
            "   width = " + nodeDepthWidth + "- margin.right - margin.left,\n" +
            "   height = " + height + " - margin.top - margin.bottom;\n");
      LOG.info("Width: {} Height: {} Left: {}", nodeDepthWidth, height, left);
   }
   
   private static int getDepth(final BasicNode root) {
      int thisNodeLength = 60 + root.getCall().length() * CHARACTER_SIZE;
      int currentMaxLength = thisNodeLength;
      for (final BasicNode child : root.getChildren()) {
         currentMaxLength = Math.max(currentMaxLength, getDepth(child) + thisNodeLength);
      }
      return currentMaxLength;
   }

   private static int getHeight(final BasicNode root) {
      int height = root.getChildren().size();
      for (final BasicNode child : root.getChildren()) {
         height = Math.max(height, getHeight(child)) + 1;
      }
      return height;
   }
}
