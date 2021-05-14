package de.dagere.peass.dependency.jmh;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Params;
import de.dagere.kopeme.generated.Result.Params.Param;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.dagere.kopeme.generated.Versioninfo;
import de.dagere.peass.config.MeasurementConfiguration;

public class ExternalJmhDataConverter {

   private static final Logger LOG = LogManager.getLogger(ExternalJmhDataConverter.class);

   public static void main(final String[] args) throws JAXBException {
      Set<File> allResultFiles = new LinkedHashSet<>();
      for (String input : args) {
         File inputFile = new File(input);
         if (inputFile.isDirectory()) {
            for (File child : inputFile.listFiles()) {
               if (child.getName().endsWith(".json")) {
                  Set<File> currentResultFiles = convertFileNoData(child);
                  allResultFiles.addAll(currentResultFiles);
               }
            }
         } else {
            Set<File> currentResultFiles = convertFileNoData(inputFile);
            allResultFiles.addAll(currentResultFiles);
         }
      }

      for (File resultFile : allResultFiles) {
         LOG.debug("Handling " + resultFile);
         Kopemedata data = XMLDataLoader.loadData(resultFile);
         mergeDoubleChunks(data);
         XMLDataStorer.storeData(resultFile, data);
      }
   }

   private static void mergeDoubleChunks(final Kopemedata data) {
      Datacollector datacollector = data.getTestcases().getTestcase().get(0).getDatacollector().get(0);
      for (Iterator<Chunk> iterator = datacollector.getChunk().iterator(); iterator.hasNext();) {
         Chunk currentChunk = iterator.next();
         String paramString = paramsToString(currentChunk.getResult().get(0).getParams());
         for (Iterator<Chunk> innerIterator = datacollector.getChunk().iterator(); innerIterator.hasNext();) {
            Chunk innerChunk = innerIterator.next();
            if (currentChunk != innerChunk) {
               String innerParamString = paramsToString(innerChunk.getResult().get(0).getParams());
               if (paramString.equals(innerParamString)) {
                  LOG.debug("Removing " + innerParamString);
                  iterator.remove();
                  innerChunk.getResult().addAll(currentChunk.getResult());
                  break;
               }
            }
         }
      }
   }

   private static Set<File> convertFileNoData(final File child) throws JAXBException {
      JmhKoPeMeConverter converter = new JmhKoPeMeConverter(new MeasurementConfiguration(-1));
      Set<File> resultFiles = converter.convertToXMLData(child, child.getParentFile());

      String currentVersion = child.getName().substring(0, child.getName().length() - ".json".length()); // Remove .json
      createChunks(resultFiles, currentVersion);

      return resultFiles;
   }

   private static void createChunks(final Set<File> resultFiles, final String currentVersion) throws JAXBException {
      for (File resultFile : resultFiles) {
         LOG.info("Handling " + resultFile);
         Kopemedata data = XMLDataLoader.loadData(resultFile);
         Datacollector datacollector = data.getTestcases().getTestcase().get(0).getDatacollector().get(0);
         Set<String> allParams = getParams(datacollector);
         for (String params : allParams) {
            Chunk addedChunk = new Chunk();
            datacollector.getResult().forEach(result -> {
               if (params != null && paramsToString(result.getParams()).equals(params)) {
                  addedChunk.getResult().add(result);
                  result.setVersion(new Versioninfo());
                  result.getVersion().setGitversion(currentVersion);
               }
            });
            datacollector.getChunk().add(addedChunk);
         }
         datacollector.getResult().clear();
         XMLDataStorer.storeData(resultFile, data);
      }
   }

   private static Set<String> getParams(final Datacollector datacollector) {
      Set<String> allParams = new LinkedHashSet<>();
      for (Result result : datacollector.getResult()) {
         Params params = result.getParams();
         allParams.add(paramsToString(params));
      }
      LOG.info("Params: {}", allParams);
      return allParams;
   }

   public static String paramsToString(final Params params) {
      String result = "";
      if (params != null) {
         for (Param param : params.getParam()) {
            result += param.getKey() + "-" + param.getValue() + " ";
         }
      }
      return result;
   }
}
