package de.dagere.peass.dependency.jmh;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.bind.JAXBException;

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
   public static void main(final String[] args) throws JAXBException {
      for (String input : args) {
         File inputFile = new File(input);
         if (inputFile.isDirectory()) {
            for (File child : inputFile.listFiles()) {
               if (child.getName().endsWith(".json")) {
                  convertFileNoData(child);
               }
            }
         } else {
            convertFileNoData(inputFile);
         }
      }
   }

   private static void convertFileNoData(final File child) throws JAXBException {
      JmhKoPeMeConverter converter = new JmhKoPeMeConverter(new MeasurementConfiguration(-1));
      Set<File> resultFiles = converter.convertToXMLData(child, child.getParentFile());

      String currentVersion = child.getName().substring(0, child.getName().length() - ".json".length()); // Remove .json
      createChunk(resultFiles, currentVersion);
   }

   private static void createChunk(final Set<File> resultFiles, final String currentVersion) throws JAXBException {
      for (File resultFile : resultFiles) {
         System.out.println("Hadnling " + resultFile);
         Kopemedata data = XMLDataLoader.loadData(resultFile);
         Datacollector datacollector = data.getTestcases().getTestcase().get(0).getDatacollector().get(0);
         Set<String> allParams = new LinkedHashSet<>();
         for (Result result : datacollector.getResult()) {
            Params params = result.getParams();
            allParams.add(paramsToString(params));
         }
         System.out.println(allParams);
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
