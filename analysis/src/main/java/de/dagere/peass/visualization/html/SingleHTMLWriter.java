package de.dagere.peass.visualization.html;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.visualization.GraphNode;

public class SingleHTMLWriter {
   private final GraphNode root;
   private final File destFolder, propertyFolder;

   public SingleHTMLWriter(GraphNode root, File destFolder, File propertyFolder) {
      this.root = root;
      this.destFolder = destFolder;
      this.propertyFolder = propertyFolder;
   }

   public void writeHTML(CauseSearchData data, String commit) throws IOException {
      TestMethodCall testcaseObject = data.getCauseConfig().getTestCase();
      String outputName = data.getMeasurementConfig().getFixedCommitConfig().getCommit() + "/" + testcaseObject.getClassWithModule() + "/"
            + testcaseObject.getMethodWithParams() + "_" + commit + ".html";
      String jsName = outputName.replace(".html", ".js");
      File singleVisualizationFile = new File(destFolder, outputName);

      writeOverviewHTML(singleVisualizationFile, jsName.substring(jsName.lastIndexOf('/') + 1));
      
      SingleTreeJSWriter jsWriter = new SingleTreeJSWriter(propertyFolder, root);
      jsWriter.writeJS(data, singleVisualizationFile, jsName.substring(jsName.lastIndexOf('/') + 1));
   }

   private void writeOverviewHTML(final File output, final String jsName) throws IOException {
      try (final BufferedWriter fileWriter = new BufferedWriter(new FileWriter(output))) {
         final HTMLEnvironmentGenerator htmlGenerator = new HTMLEnvironmentGenerator(fileWriter);
         fileWriter.write("<!DOCTYPE html>\n");
         htmlGenerator.writeHTML("visualization/TreeStructureHeader.html");

         fileWriter.write("<script src='" + jsName + "'></script>\n");

         htmlGenerator.writeHTML("visualization/RestOfHTML.html");
         fileWriter.flush();
      }
   }
}
