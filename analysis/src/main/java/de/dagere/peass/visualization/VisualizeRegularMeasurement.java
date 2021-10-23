package de.dagere.peass.visualization;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.visualization.html.HTMLWriter;

public class VisualizeRegularMeasurement {
   
   private static final Logger LOG = LogManager.getLogger(VisualizeRegularMeasurement.class);

   private final File resultFolder;

   public VisualizeRegularMeasurement(final File resultFolder) {
      this.resultFolder = resultFolder;
   }

   public void analyzeFile(final File peassFolder) throws JAXBException, JsonProcessingException, FileNotFoundException, IOException {
      PeassFolders folders = new PeassFolders(peassFolder);
      for (File kopemeFile : folders.getFullMeasurementFolder().listFiles((FilenameFilter) new WildcardFileFilter("*xml"))) {
         LOG.debug("Visualizing: {}", kopemeFile);
         Kopemedata data = XMLDataLoader.loadData(kopemeFile);
         for (TestcaseType test : data.getTestcases().getTestcase()) {
            for (Chunk chunk : test.getDatacollector().get(0).getChunk()) {
               List<String> versions = getVersions(chunk);
               TestCase testcase = new TestCase(data.getTestcases().getClazz(), test.getName());
               for (String version : versions) {
                  KoPeMeTreeConverter koPeMeTreeConverter = new KoPeMeTreeConverter(folders, version, testcase);
                  GraphNode node = koPeMeTreeConverter.getData();
                  if (node != null) {
                     visualizeNode(versions, testcase, node);
                  }
               }
            }
         }
      }
   }

   private void visualizeNode(final List<String> versions, final TestCase testcase, final GraphNode node) throws IOException, JsonProcessingException, FileNotFoundException, JAXBException {
      File destFolder = new File(resultFolder, versions.get(0));
      GraphNode emptyNode = new GraphNode(testcase.getExecutable(), "void " + testcase.getExecutable().replace("#", ".") + "()", CauseSearchData.ADDED);
      emptyNode.setName(testcase.getExecutable());
      CauseSearchData data2 = createEmptyData(versions, testcase);
      HTMLWriter htmlWriter = new HTMLWriter(emptyNode, data2, destFolder, null, node);
      htmlWriter.writeHTML();
   }

   private CauseSearchData createEmptyData(final List<String> versions, final TestCase testcase) {
      CauseSearchData data2 = new CauseSearchData();
      data2.setCauseConfig(new CauseSearcherConfig(testcase, false, false, 1.0, false, false, null, 1));
      data2.setConfig(new MeasurementConfig(2, versions.get(0), versions.get(1)));
      return data2;
   }

   private List<String> getVersions(final Chunk chunk) {
      List<String> versions = new LinkedList<>();
      for (Result result : chunk.getResult()) {
         if (!versions.contains(result.getVersion().getGitversion())) {
            versions.add(result.getVersion().getGitversion());
         }
      }
      return versions;
   }

}
