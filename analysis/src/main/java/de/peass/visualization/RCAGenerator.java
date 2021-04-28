package de.peass.visualization;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.dependency.CauseSearchFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.utils.Constants;
import de.peass.visualization.html.HTMLWriter;

public class RCAGenerator {

   private static final Logger LOG = LogManager.getLogger(RCAGenerator.class);

   private final File source, destFolder;
   private final CauseSearchData data;
   private final CauseSearchFolders folders;
   private File propertyFolder;

   private CallTreeNode rootPredecessor, rootVersion;

   public RCAGenerator(final File source, final File destFolder, final CauseSearchFolders folders) throws JsonParseException, JsonMappingException, IOException, JAXBException {
      this.source = source;
      this.destFolder = destFolder;
      this.folders = folders;
      File details = new File(source.getParentFile(), "details" + File.separator + source.getName());
      data = readData(details);
   }

   public void setPropertyFolder(final File propertyFolder) {
      this.propertyFolder = propertyFolder;
   }

   public void createVisualization() throws IOException, JsonParseException, JsonMappingException, JsonProcessingException, FileNotFoundException, JAXBException {
      LOG.info("Visualizing " + data.getTestcase());
      final NodePreparator preparator = new NodePreparator(rootPredecessor, rootVersion, data);
      preparator.prepare();
      final GraphNode rootNode = preparator.getRootNode();
      KoPeMeTreeConverter converter = new KoPeMeTreeConverter(folders, data.getMeasurementConfig().getVersion(), new TestCase(data.getTestcase()));
      HTMLWriter writer = new HTMLWriter(rootNode, data, destFolder, propertyFolder, converter.getData());
      writer.writeHTML();;
//      writeHTML(rootNode, data);
   }

   private CauseSearchData readData(final File details) throws IOException, JsonParseException, JsonMappingException {
      final CauseSearchData data;
      if (details.exists()) {
         data = Constants.OBJECTMAPPER.readValue(details, CauseSearchData.class);
      } else {
         data = Constants.OBJECTMAPPER.readValue(source, CauseSearchData.class);
      }
      return data;
   }

   public CauseSearchData getData() {
      return data;
   }

   public void setFullTree(final CallTreeNode rootPredecessor, final CallTreeNode rootVersion) {
      this.rootPredecessor = rootPredecessor;
      this.rootVersion = rootVersion;
   }
}
