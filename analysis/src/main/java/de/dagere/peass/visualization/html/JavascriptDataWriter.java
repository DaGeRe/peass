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
         String commit = data.getMeasurementConfig().getFixedCommitConfig().getCommit();
         String commitOld = data.getMeasurementConfig().getFixedCommitConfig().getCommitOld();
         fileWriter.write("if (typeof jenkins !== 'undefined') {} else {");
         fileWriter.write("if (document.getElementById('testcaseDiv') != null) { \n   document.getElementById('testcaseDiv').innerHTML=\"Commit: <a href='"
               + "javascript:fallbackCopyTextToClipboard(\\\"-commit " + commit +
               " -test " + data.getTestcase() + "\\\")'>"
               + commit + "</a><br>Predecessor: " + commitOld + "<br>Comitter: " + "<br>");
         fileWriter.write("Test Case: " + data.getTestcase() + "<br>\";\n");
         writeDashboardLink(data, fileWriter);
         fileWriter.write("  }\n}\n");
         fileWriter.write("\n");

         writeSources(data, fileWriter);

         writeColoredTree(fileWriter);

         JavascriptFunctions.writeTreeDivSizes(fileWriter, root);

         fileWriter.write("var kopemeData = [\n");
         fileWriter.write(Constants.OBJECTMAPPER.writeValueAsString(converted));
         fileWriter.write("];\n");
      }
   }

   private void writeSources(final CauseSearchData data, final BufferedWriter fileWriter) throws IOException {
      if (propertyFolder != null) {
         final File methodSourceFolder = new File(propertyFolder, "methods");
         String mainCommit = data.getMeasurementConfig().getFixedCommitConfig().getCommit();
         final SourceWriter writer = new SourceWriter(fileWriter, methodSourceFolder, mainCommit);
         writer.writeSources(root);
      }
   }

   private void writeDashboardLink(final CauseSearchData data, final BufferedWriter fileWriter) throws IOException {
      fileWriter.write("   if (typeof jenkins !== 'undefined') {\n      "); // dynamic content for jenkins could be added here, but is currently not needed
      fileWriter.write("   } else {\n");
      System.out.println(data.getTestcase());
      fileWriter.write("   document.getElementById('testcaseDiv').innerHTML+=\"<a href='" + data.getTestcase().substring(data.getTestcase().indexOf('#') + 1)
            + "_dashboard.html?call=overall&ess=-1' target='parent'>Inspect Overall Measurement</a>\";\n");
      fileWriter.write("   }\n");
   }

   private void writeColoredTree(final BufferedWriter fileWriter) throws IOException, JsonProcessingException {
      fileWriter.write("var treeData = [\n");
      fileWriter.write(Constants.OBJECTMAPPER.writeValueAsString(root));
      fileWriter.write("];\n");
   }
}
