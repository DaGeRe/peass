package de.dagere.peass.visualization.html;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;

public class SingleTreeJSWriter {
   
   private final File propertyFolder;
   private final CallTreeNode root;
   
   public SingleTreeJSWriter(final File propertyFolder, final CallTreeNode root) {
      this.propertyFolder = propertyFolder;
      this.root = root;
   }
   
   public void writeJS(CauseSearchData data, File output, String jsName) {
      File outputJS = new File(output.getParentFile(), jsName);
      try (final BufferedWriter fileWriter = new BufferedWriter(new FileWriter(outputJS))) {
         fileWriter.write("if (document.getElementById('testcaseDiv') != null) { \n   document.getElementById('testcaseDiv').innerHTML=\"Commit: <a href='"
               + "javascript:fallbackCopyTextToClipboard(\\\"-commit " + data.getMeasurementConfig().getFixedCommitConfig().getCommit() +
               " -test " + data.getTestcase() + "\\\")'>"
               + data.getMeasurementConfig().getFixedCommitConfig().getCommit() + "</a><br>");
         fileWriter.write("Test Case: " + data.getTestcase() + "<br>\";\n");
         fileWriter.write("}\n");
         fileWriter.write("\n");
         
         writeSources(data, fileWriter);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   private void writeSources(final CauseSearchData data, final BufferedWriter fileWriter) throws IOException {
      if (propertyFolder != null) {
         final File methodSourceFolder = new File(propertyFolder, "methods");
         final SourceWriter writer = new SourceWriter(fileWriter, methodSourceFolder, data.getMeasurementConfig().getFixedCommitConfig().getCommit());
         writer.writeSources(root);
      }
   }
}
