package de.dagere.peass.dependency.jmh;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;



import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.datastorage.JSONDataStorer;
import de.dagere.kopeme.datastorage.ParamNameHelper;
import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.kopeme.kopemedata.VMResultChunk;
import de.dagere.peass.config.MeasurementConfig;

public class ExternalJmhDataConverter {

   private static final Logger LOG = LogManager.getLogger(ExternalJmhDataConverter.class);

   public static void main(final String[] args)  {
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
         Kopemedata data = JSONDataLoader.loadData(resultFile);
         mergeDoubleChunks(data);
         JSONDataStorer.storeData(resultFile, data);
      }
   }

   private static void mergeDoubleChunks(final Kopemedata data) {
      DatacollectorResult datacollector = data.getFirstMethodResult().getDatacollectorResults().get(0);
      for (Iterator<VMResultChunk> iterator = datacollector.getChunks().iterator(); iterator.hasNext();) {
         VMResultChunk currentChunk = iterator.next();
         String paramString = ParamNameHelper.paramsToString(currentChunk.getResults().get(0).getParameters());
         for (Iterator<VMResultChunk> innerIterator = datacollector.getChunks().iterator(); innerIterator.hasNext();) {
            VMResultChunk innerChunk = innerIterator.next();
            if (currentChunk != innerChunk) {
               String innerParamString = ParamNameHelper.paramsToString(innerChunk.getResults().get(0).getParameters());
               if (paramString.equals(innerParamString)) {
                  LOG.debug("Removing " + innerParamString);
                  iterator.remove();
                  innerChunk.getResults().addAll(currentChunk.getResults());
                  break;
               }
            }
         }
      }
   }

   private static Set<File> convertFileNoData(final File child)  {
      JmhKoPeMeConverter converter = new JmhKoPeMeConverter(new MeasurementConfig(-1));
      Set<File> resultFiles = converter.convertToXMLData(child, child.getParentFile());

      String currentVersion = child.getName().substring(0, child.getName().length() - ".json".length()); // Remove .json
      createChunks(resultFiles, currentVersion);

      return resultFiles;
   }

   private static void createChunks(final Set<File> resultFiles, final String currentVersion)  {
      for (File resultFile : resultFiles) {
         LOG.info("Handling " + resultFile);
         Kopemedata data = JSONDataLoader.loadData(resultFile);
         DatacollectorResult datacollector = data.getFirstMethodResult().getDatacollectorResults().get(0);
         Set<String> allParams = getParams(datacollector);
         for (String params : allParams) {
            VMResultChunk addedChunk = new VMResultChunk();
            datacollector.getResults().forEach(result -> {
               if (params != null && ParamNameHelper.paramsToString(result.getParameters()).equals(params)) {
                  addedChunk.getResults().add(result);
                  result.setCommit(currentVersion);
               }
            });
            datacollector.getChunks().add(addedChunk);
         }
         datacollector.getResults().clear();
         JSONDataStorer.storeData(resultFile, data);
      }
   }

   private static Set<String> getParams(final DatacollectorResult datacollector) {
      Set<String> allParams = new LinkedHashSet<>();
      for (VMResult result : datacollector.getResults()) {
         LinkedHashMap<String, String> params = result.getParameters();
         allParams.add(ParamNameHelper.paramsToString(params));
      }
      LOG.info("Params: {}", allParams);
      return allParams;
   }
   
   
}
