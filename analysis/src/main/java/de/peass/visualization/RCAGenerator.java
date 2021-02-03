package de.peass.visualization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.utils.Constants;

public class RCAGenerator {

   private static final Logger LOG = LogManager.getLogger(RCAGenerator.class);

   private final File source, details, destFolder;
   private final CauseSearchData data;
   private final CauseSearchFolders folders;
   private File propertyFolder;

   private CallTreeNode rootPredecessor, rootVersion;

   public RCAGenerator(final File source, final File destFolder, final CauseSearchFolders folders) throws JsonParseException, JsonMappingException, IOException, JAXBException {
      this.source = source;
      details = new File(source.getParentFile(), "details" + File.separator + source.getName());
      this.destFolder = destFolder;
      data = readData();
      this.folders = folders;
   }

   public void setPropertyFolder(final File propertyFolder) {
      this.propertyFolder = propertyFolder;
   }

   public void createVisualization() throws IOException, JsonParseException, JsonMappingException, JsonProcessingException, FileNotFoundException, JAXBException {
      LOG.info("Visualizing " + data.getTestcase());
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

   private void writeHTML(final GraphNode root, final CauseSearchData data) throws IOException, JsonProcessingException, FileNotFoundException, JAXBException {
      final File output = getOutputHTML(data);
      final String jsName = output.getName().replace(".html", ".js").replaceAll("#", "_");

      File nodeDashboard = new File(output.getParentFile(), output.getName().replace(".html", "_dashboard.html"));
      new NodeDashboardWriter(nodeDashboard, data).write(jsName);

      writeOverviewHTML(output, jsName);

      KoPeMeTreeConverter converter = new KoPeMeTreeConverter(folders, data.getMeasurementConfig().getVersion(), new TestCase(data.getTestcase()));
      final JavascriptDataWriter javascriptDataWriter = new JavascriptDataWriter(propertyFolder, root);
      javascriptDataWriter.writeJS(data, output, jsName, converter);
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

   public CauseSearchData getData() {
      return data;
   }

   public void setFullTree(final CallTreeNode rootPredecessor, final CallTreeNode rootVersion) {
      this.rootPredecessor = rootPredecessor;
      this.rootVersion = rootVersion;
   }
}
