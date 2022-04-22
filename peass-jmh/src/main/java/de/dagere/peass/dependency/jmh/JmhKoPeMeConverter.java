package de.dagere.peass.dependency.jmh;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.dagere.kopeme.datastorage.JSONDataStorer;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.utils.Constants;

/**
 * Converts JMH data into KoPeMe format to use change analysis afterwards
 * 
 * @author reichelt
 *
 */
public class JmhKoPeMeConverter {

   private final MeasurementConfig measurementConfig;

   public JmhKoPeMeConverter(final MeasurementConfig measurementConfig) {
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
               String clazzName = name.substring(0, name.lastIndexOf('.'));
               String benchmarkMethodName = name.substring(name.lastIndexOf('.') + 1);

               TestCase testcase = new TestCase(clazzName, benchmarkMethodName);

               JsonNode primaryMetric = benchmark.get("primaryMetric");
               String scoreUnit = primaryMetric.get("scoreUnit").asText();
               ArrayNode rawData = (ArrayNode) primaryMetric.get("rawData");

               JmhBenchmarkConverter converter = new JmhBenchmarkConverter(testcase, clazzResultFolder, measurementConfig);
               converter.convertData(rawData, benchmark, scoreUnit);

               JSONDataStorer.storeData(converter.getKoPeMeFile(), converter.getTransformed());
               results.add(converter.getKoPeMeFile());
            }
         }
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
      return results;
   }

   
}
