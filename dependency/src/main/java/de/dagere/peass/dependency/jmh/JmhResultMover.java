package de.dagere.peass.dependency.jmh;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.dagere.kopeme.datacollection.TimeDataCollectorNoGC;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Kopemedata.Testcases;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.Result.Fulldata.Value;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.traces.KiekerFolderUtil;
import de.dagere.peass.utils.Constants;

public class JmhResultMover {

   private static final int SECONDS_TO_NANOSECONDS = 1000000000;
   private final PeASSFolders folders;
   private final MeasurementConfiguration measurementConfig;
   private final File[] sourceResultFolders;

   public JmhResultMover(final PeASSFolders folders, final MeasurementConfiguration measurementConfig) {
      this.folders = folders;
      this.measurementConfig = measurementConfig;
      final File[] files = folders.getTempMeasurementFolder().listFiles(new FileFilter() {

         @Override
         public boolean accept(final File pathname) {
            return pathname.getName().startsWith("kieker-");
         }
      });
      if (files.length > 0) {
         sourceResultFolders = files;
      } else {
         sourceResultFolders = null;
      }
   }

   public void moveToMethodFolder(final TestCase testcase, final File sourceJsonResultFile) {
      final File moduleResultsFolder = KiekerFolderUtil.getModuleResultFolder(folders, testcase);
      final File clazzResultFolder = new File(moduleResultsFolder, testcase.getClazz());

      final File kiekerTimeFolder = new File(clazzResultFolder, Long.toString(System.currentTimeMillis()) + File.separator + testcase.getMethod());
      kiekerTimeFolder.mkdirs();

      final File expectedKoPeMeFile = new File(clazzResultFolder, testcase.getMethod() + ".xml");
      convertToXMLData(sourceJsonResultFile, expectedKoPeMeFile);

      if (sourceResultFolders != null) {
         for (File sourceResultFolder : sourceResultFolders) {
            final File kiekerSubfolder = new File(kiekerTimeFolder, sourceResultFolder.getName());
            sourceResultFolder.renameTo(kiekerSubfolder);
         }
      }
   }

   private void convertToXMLData(final File sourceJsonResultFile, final File expectedKoPeMeFile) {
      try {
         ArrayNode benchmarks = (ArrayNode) Constants.OBJECTMAPPER.readTree(sourceJsonResultFile);
         for (JsonNode benchmark : benchmarks) {
            final String name = getBenchmarkName(benchmark);

            JsonNode primaryMetric = benchmark.get("primaryMetric");
            ArrayNode rawData = (ArrayNode) primaryMetric.get("rawData");

            Kopemedata transformed = new Kopemedata();
            Testcases testcases = new Testcases();
            testcases.setClazz(name.substring(0, name.lastIndexOf('.')));
            transformed.setTestcases(testcases);
            TestcaseType testclazz = new TestcaseType();
            transformed.getTestcases().getTestcase().add(testclazz);
            testclazz.setName(name.substring(name.lastIndexOf('.') + 1));

            Datacollector timeCollector = new Datacollector();
            timeCollector.setName(TimeDataCollectorNoGC.class.getName());
            testclazz.getDatacollector().add(timeCollector);

            for (JsonNode vmExecution : rawData) {
               Result result = buildResult(vmExecution);
               timeCollector.getResult().add(result);
            }

            // timeCollector.getResult().add(new Result())

            XMLDataStorer.storeData(expectedKoPeMeFile, transformed);
         }

      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private String getBenchmarkName(final JsonNode benchmark) {
      String paramString = getParameterString(benchmark);
      
      final String name;
      if (!paramString.isEmpty()) {
         name = benchmark.get("benchmark").asText() + "-" + paramString;
      } else {
         name = benchmark.get("benchmark").asText();
      }
      return name;
   }

   private String getParameterString(final JsonNode benchmark) {
      String parameterString = "";
      JsonNode params = benchmark.get("params");
      if (params != null) {
         for (Iterator<String> fieldIterator = params.fieldNames(); fieldIterator.hasNext();) {
            String field = fieldIterator.next();
            parameterString += params.get(field).asText();
            if (fieldIterator.hasNext()) {
               parameterString += "-";
            }
         }
      }
      return parameterString;
   }

   private Result buildResult(final JsonNode vmExecution) {
      Result result = new Result();
      result.setFulldata(new Fulldata());
      for (JsonNode iteration : vmExecution) {
         Value value = new Value();
         long iterationDuration = (long) (iteration.asDouble() * SECONDS_TO_NANOSECONDS);
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
}
