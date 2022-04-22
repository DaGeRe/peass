package de.dagere.peass.dependency.jmh;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.dagere.kopeme.datacollection.TimeDataCollectorNoGC;
import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.kopeme.kopemedata.Fulldata;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.MeasuredValue;
import de.dagere.kopeme.kopemedata.TestMethod;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class JmhBenchmarkConverter {

   private static final int SECONDS_TO_NANOSECONDS = 1000000000;

   private final File koPeMeFile;
   private final Kopemedata transformed;
   private final DatacollectorResult timeCollector;
   private final MeasurementConfig measurementConfig;

   public JmhBenchmarkConverter(final TestCase testcase, final File clazzResultFolder, final MeasurementConfig measurementConfig) {
      this.measurementConfig = measurementConfig;
      File koPeMeFileTry = new File(clazzResultFolder, testcase.getMethod() + ".json");

      if (koPeMeFileTry.exists()) {
         Kopemedata transformedTry = JSONDataLoader.loadData(koPeMeFileTry);
         if (transformedTry.getClazz().equals(testcase.getClazz()) &&
               transformedTry.getFirstMethodResult().getMethod().equals(testcase.getMethod())) {
            transformed = transformedTry;
            koPeMeFile = koPeMeFileTry;
         } else {
            koPeMeFile = new File(clazzResultFolder, testcase.getShortClazz() + "_" + testcase.getMethod() + ".json");
            transformed = JSONDataLoader.loadData(koPeMeFileTry);
         }
         timeCollector = transformed.getFirstTimeDataCollector();
      } else {
         koPeMeFile = koPeMeFileTry;
         transformed = new Kopemedata(testcase.getClazz());
         TestMethod testclazz = new TestMethod(testcase.getMethod());
         transformed.getMethods().add(testclazz);
         timeCollector = new DatacollectorResult(TimeDataCollectorNoGC.class.getName());
         testclazz.getDatacollectorResults().add(timeCollector);
      }
   }

   public void convertData(final ArrayNode rawData, final JsonNode benchmark, final String scoreUnit) {
      JsonNode params = benchmark.get("params");
      for (JsonNode vmExecution : rawData) {
         VMResult result = buildResult(vmExecution, scoreUnit);
         if (params != null) {
            setParamMap(result, params);
         }
         timeCollector.getResults().add(result);
      }
   }

   private void setParamMap(final VMResult result, final JsonNode params) {
      result.setParameters(new LinkedHashMap<>());
      for (Iterator<String> fieldIterator = params.fieldNames(); fieldIterator.hasNext();) {
         final String field = fieldIterator.next();
         final String value = params.get(field).asText();
         result.getParameters().put(field, value);
      }
   }

   private VMResult buildResult(final JsonNode vmExecution, final String scoreUnit) {
      VMResult result = new VMResult();
      Fulldata fulldata = buildFulldata(vmExecution, scoreUnit);
      result.setFulldata(fulldata);

      DescriptiveStatistics statistics = new DescriptiveStatistics();
      result.getFulldata().getValues().forEach(value -> statistics.addValue(value.getValue()));
      result.setValue(statistics.getMean());
      result.setDeviation(statistics.getStandardDeviation());
      result.setIterations(result.getFulldata().getValues().size());

      // Peass always executes the iterations "normally" and discards warmup afterwards
      result.setWarmup(0);
      result.setRepetitions(measurementConfig.getRepetitions());

      return result;
   }

   private Fulldata buildFulldata(final JsonNode vmExecution, final String scoreUnit) {
      Fulldata fulldata = new Fulldata();
      for (JsonNode iteration : vmExecution) {
         MeasuredValue value = new MeasuredValue();
         long iterationDuration;
         if (scoreUnit.equals("s/op")) {
            iterationDuration = (long) (iteration.asDouble() * SECONDS_TO_NANOSECONDS);
         } else if (scoreUnit.equals("ops/s")) {
            double valueInNanoseconds = (1 / iteration.asDouble()) * SECONDS_TO_NANOSECONDS;
            iterationDuration = (long) valueInNanoseconds;
         } else {
            throw new RuntimeException("Unexpected unit: " + scoreUnit);
         }

         value.setValue(iterationDuration);
         fulldata.getValues().add(value);
      }
      return fulldata;
   }

   public File getKoPeMeFile() {
      return koPeMeFile;
   }

   public Kopemedata getTransformed() {
      return transformed;
   }
}
