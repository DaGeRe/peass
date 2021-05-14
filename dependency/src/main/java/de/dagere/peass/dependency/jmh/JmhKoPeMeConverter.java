package de.dagere.peass.dependency.jmh;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.dagere.kopeme.datacollection.TimeDataCollectorNoGC;
import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Kopemedata.Testcases;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.Result.Fulldata.Value;
import de.dagere.kopeme.generated.Result.Params;
import de.dagere.kopeme.generated.Result.Params.Param;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.dagere.kopeme.generated.Versioninfo;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.utils.Constants;

/**
 * Converts JMH data into KoPeMe format to use change analysis afterwards
 * 
 * @author reichelt
 *
 */
public class JmhKoPeMeConverter {
   private static final int SECONDS_TO_NANOSECONDS = 1000000000;

   private final MeasurementConfiguration measurementConfig;

   public JmhKoPeMeConverter(final MeasurementConfiguration measurementConfig) {
      this.measurementConfig = measurementConfig;
   }

   public Set<File> convertToXMLData(final File sourceJsonResultFile, final File clazzResultFolder) {
      Set<File> results = new HashSet<>();
      try {
         JsonNode rootNode = Constants.OBJECTMAPPER.readTree(sourceJsonResultFile);
         if (rootNode != null && rootNode instanceof ArrayNode) {
            ArrayNode benchmarks = (ArrayNode) rootNode;
            for (JsonNode benchmark : benchmarks) {

               final String name = benchmark.get("benchmark").asText();
               String benchmarkMethodName = name.substring(name.lastIndexOf('.') + 1);

               JsonNode primaryMetric = benchmark.get("primaryMetric");
               String scoreUnit = primaryMetric.get("scoreUnit").asText();
               ArrayNode rawData = (ArrayNode) primaryMetric.get("rawData");

               File koPeMeFile = new File(clazzResultFolder, benchmarkMethodName + ".xml");
               results.add(koPeMeFile);
               Kopemedata transformed;
               Datacollector timeCollector;
               if (koPeMeFile.exists()) {
                  transformed = XMLDataLoader.loadData(koPeMeFile);
                  timeCollector = transformed.getTestcases().getTestcase().get(0).getDatacollector().get(0);
               } else {
                  transformed = new Kopemedata();
                  Testcases testcases = new Testcases();
                  testcases.setClazz(name.substring(0, name.lastIndexOf('.')));
                  transformed.setTestcases(testcases);
                  TestcaseType testclazz = new TestcaseType();
                  transformed.getTestcases().getTestcase().add(testclazz);
                  testclazz.setName(name.substring(name.lastIndexOf('.') + 1));
                  timeCollector = new Datacollector();
                  timeCollector.setName(TimeDataCollectorNoGC.class.getName());
                  testclazz.getDatacollector().add(timeCollector);
               }

               for (JsonNode vmExecution : rawData) {
                  Result result = buildResult(vmExecution, scoreUnit);
                  JsonNode params = benchmark.get("params");
                  if (params != null) {
                     setParamMap(result, params);
                  }
                  timeCollector.getResult().add(result);
               }

               XMLDataStorer.storeData(koPeMeFile, transformed);
            }
         }
      } catch (IOException | JAXBException e) {
         throw new RuntimeException(e);
      }
      return results;
   }

   private void setParamMap(final Result result, final JsonNode params) {
      result.setParams(new Params());
      for (Iterator<String> fieldIterator = params.fieldNames(); fieldIterator.hasNext();) {
         final String field = fieldIterator.next();
         final String value = params.get(field).asText();
         final Param param = new Param();
         param.setKey(field);
         param.setValue(value);
         result.getParams().getParam().add(param);
      }
   }

   private Result buildResult(final JsonNode vmExecution, final String scoreUnit) {
      Result result = new Result();
      result.setFulldata(new Fulldata());
      for (JsonNode iteration : vmExecution) {
         Value value = new Value();
         long iterationDuration;
         if (!scoreUnit.equals("ops/s")) {
            iterationDuration = (long) (iteration.asDouble() * SECONDS_TO_NANOSECONDS);
         } else {
            iterationDuration = iteration.asLong();
         }

         value.setValue(iterationDuration);
         result.getFulldata().getValue().add(value);
      }

      DescriptiveStatistics statistics = new DescriptiveStatistics();
      result.getFulldata().getValue().forEach(value -> statistics.addValue(value.getValue()));
      result.setValue(statistics.getMean());
      result.setDeviation(statistics.getStandardDeviation());
      result.setIterations(result.getFulldata().getValue().size());

      // Assume that warmup and repetitions took place as defined, since they are not recorded by jmh
      result.setWarmup(measurementConfig.getWarmup());
      result.setRepetitions(measurementConfig.getRepetitions());

      return result;
   }

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
         Kopemedata data = XMLDataLoader.loadData(resultFile);
         Chunk addedChunk = new Chunk();
         Datacollector datacollector = data.getTestcases().getTestcase().get(0).getDatacollector().get(0);
         addedChunk.getResult().addAll(datacollector.getResult());
         for (Result result : datacollector.getResult()) {
            result.setVersion(new Versioninfo());
            result.getVersion().setGitversion(currentVersion);
         }
         datacollector.getChunk().add(addedChunk);
         datacollector.getResult().clear();
         XMLDataStorer.storeData(resultFile, data);
      }
   }
}
