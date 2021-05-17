package de.dagere.peass.dependency.jmh;

import java.io.File;
import java.util.Iterator;

import javax.xml.bind.JAXBException;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.dagere.kopeme.datacollection.TimeDataCollectorNoGC;
import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Kopemedata.Testcases;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.Result.Fulldata.Value;
import de.dagere.kopeme.generated.Result.Params;
import de.dagere.kopeme.generated.Result.Params.Param;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class JmhBenchmarkConverter {
   
   private static final int SECONDS_TO_NANOSECONDS = 1000000000;
   
   private final File koPeMeFile;
   private final Kopemedata transformed;
   private final Datacollector timeCollector;
   private final MeasurementConfiguration measurementConfig;
   
   public JmhBenchmarkConverter(final TestCase testcase, final File clazzResultFolder, final MeasurementConfiguration measurementConfig) throws JAXBException {
      this.measurementConfig = measurementConfig;
      File koPeMeFileTry = new File(clazzResultFolder, testcase.getMethod() + ".xml");
      
      if (koPeMeFileTry.exists()) {
         Kopemedata transformedTry = XMLDataLoader.loadData(koPeMeFileTry);
         if (transformedTry.getTestcases().getClazz().equals(testcase.getClazz()) && 
               transformedTry.getTestcases().getTestcase().get(0).getName().equals(testcase.getMethod())) {
            transformed = transformedTry;
            koPeMeFile = koPeMeFileTry;
         } else {
            koPeMeFile = new File(clazzResultFolder, testcase.getShortClazz() + "_" + testcase.getMethod() + ".xml");
            transformed = XMLDataLoader.loadData(koPeMeFileTry);
         }
         timeCollector = transformed.getTestcases().getTestcase().get(0).getDatacollector().get(0);
      } else {
         koPeMeFile = koPeMeFileTry;
         transformed = new Kopemedata();
         Testcases testcases = new Testcases();
         testcases.setClazz(testcase.getClazz());
         transformed.setTestcases(testcases);
         TestcaseType testclazz = new TestcaseType();
         transformed.getTestcases().getTestcase().add(testclazz);
         testclazz.setName(testcase.getMethod());
         timeCollector = new Datacollector();
         timeCollector.setName(TimeDataCollectorNoGC.class.getName());
         testclazz.getDatacollector().add(timeCollector);
      }
   }
   
   public void convertData(final ArrayNode rawData, final JsonNode benchmark, final String scoreUnit) {
      JsonNode params = benchmark.get("params");
      for (JsonNode vmExecution : rawData) {
         Result result = buildResult(vmExecution, scoreUnit);
         if (params != null) {
            setParamMap(result, params);
         }
         timeCollector.getResult().add(result);
      }
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
      Fulldata fulldata = buildFulldata(vmExecution, scoreUnit);
      result.setFulldata(fulldata);

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
   
   private Fulldata buildFulldata(final JsonNode vmExecution, final String scoreUnit) {
      Fulldata fulldata = new Fulldata();
      for (JsonNode iteration : vmExecution) {
         Value value = new Value();
         long iterationDuration;
         if (!scoreUnit.equals("ops/s")) {
            iterationDuration = (long) (iteration.asDouble() * SECONDS_TO_NANOSECONDS);
         } else {
            iterationDuration = iteration.asLong();
         }

         value.setValue(iterationDuration);
         fulldata.getValue().add(value);
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
