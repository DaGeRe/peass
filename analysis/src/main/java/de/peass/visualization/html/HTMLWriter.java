package de.peass.visualization.html;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.peass.visualization.GraphNode;
import de.peass.visualization.NodeDashboardWriter;
import de.peass.visualization.RCAGenerator;

public class HTMLWriter {
   
   private final GraphNode root;
   private final CauseSearchData data;
   private final File destFolder, propertyFolder;
   private final GraphNode converted;

   public HTMLWriter(final GraphNode root, final CauseSearchData data, final File destFolder, final File propertyFolder, final GraphNode converted) {
      this.root = root;
      this.data = data;
      this.destFolder = destFolder;
      this.propertyFolder = propertyFolder;
      this.converted = converted;
   }

   public void writeHTML() throws IOException, JsonProcessingException, FileNotFoundException, JAXBException {
      final File output = getOutputHTML(data);
      final String jsName = output.getName().replace(".html", ".js").replaceAll("#", "_");

      File nodeDashboard = new File(output.getParentFile(), output.getName().replace(".html", "_dashboard.html"));
      new NodeDashboardWriter(nodeDashboard, data).write(jsName);

      writeOverviewHTML(output, jsName);

      final JavascriptDataWriter javascriptDataWriter = new JavascriptDataWriter(propertyFolder, root);
      javascriptDataWriter.writeJS(data, output, jsName, converted);
   }
   
   private void writeOverviewHTML(final File output, final String jsName) throws IOException {
      try (final BufferedWriter fileWriter = new BufferedWriter(new FileWriter(output))) {
         final HTMLEnvironmentGenerator htmlGenerator = new HTMLEnvironmentGenerator(fileWriter);
         fileWriter.write("<!DOCTYPE html>\n");
         htmlGenerator.writeHTML("visualization/HeaderOfHTML.html");

         fileWriter.write("<script src='" + jsName + "'></script>\n");

         htmlGenerator.writeHTML("visualization/RestOfHTML.html");
         fileWriter.flush();
      }
   }

   private File getOutputHTML(final CauseSearchData data) {
      final File output;
      final String testcase = data.getCauseConfig().getTestCase().getTestclazzWithModuleName() + ChangedEntity.METHOD_SEPARATOR + data.getCauseConfig().getTestCase().getMethod();
      if (destFolder.getName().equals(data.getMeasurementConfig().getVersion())) {
         output = new File(destFolder, testcase.replace('#', '_') + ".html");
         copyResources(destFolder);
      } else {
         File versionFolder = new File(destFolder, data.getMeasurementConfig().getVersion());
         copyResources(versionFolder);
         output = new File(versionFolder, testcase.replace('#', '_') + ".html");
      }
      output.getParentFile().mkdirs();
      return output;
   }
   
   private void copyResources(final File folder) {
      try {
         URL difflib = RCAGenerator.class.getClassLoader().getResource("visualization/difflib.js");
         FileUtils.copyURLToFile(difflib, new File(folder, "difflib.js"));
         URL diffview = RCAGenerator.class.getClassLoader().getResource("visualization/diffview.js");
         FileUtils.copyURLToFile(diffview, new File(folder, "diffview.js"));
         URL diffviewcss = RCAGenerator.class.getClassLoader().getResource("visualization/diffview.css");
         FileUtils.copyURLToFile(diffviewcss, new File(folder, "diffview.css"));
         URL jsGraphSource = RCAGenerator.class.getClassLoader().getResource("visualization/jsGraphSource.js");
         FileUtils.copyURLToFile(jsGraphSource, new File(folder, "jsGraphSource.js"));
         URL dashboardStart = RCAGenerator.class.getClassLoader().getResource("visualization/peass-dashboard-start.js");
         FileUtils.copyURLToFile(dashboardStart, new File(folder, "peass-dashboard-start.js"));
         URL peassCode = RCAGenerator.class.getClassLoader().getResource("visualization/peass-visualization-code.js");
         FileUtils.copyURLToFile(peassCode, new File(folder, "peass-visualization-code.js"));
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
