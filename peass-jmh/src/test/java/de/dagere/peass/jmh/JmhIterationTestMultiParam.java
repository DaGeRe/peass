package de.dagere.peass.jmh;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;



import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsIterableContaining;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.javaparser.ParseException;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.TestMethod;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.TestConstants;
import de.dagere.peass.TestUtil;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.ExecutorCreator;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.jmh.JmhTestTransformer;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;

public class JmhIterationTestMultiParam {

   @BeforeEach
   public void clearCurrent() throws IOException {
      TestUtil.deleteContents(TestConstants.CURRENT_FOLDER);
   }

   @Test
   public void testVersionReading() throws IOException, InterruptedException, XmlPullParserException, ParseException, ViewNotFoundException, ClassNotFoundException,
         InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
      FileUtils.copyDirectory(JmhTestConstants.MULTIPARAM_VERSION, TestConstants.CURRENT_FOLDER);

      MeasurementConfig measurementConfig = new MeasurementConfig(3);
      measurementConfig.setIterations(4);
      measurementConfig.setWarmup(2);
      JmhTestTransformer transformer = new JmhTestTransformer(TestConstants.CURRENT_FOLDER, measurementConfig);
      PeassFolders folders = new PeassFolders(TestConstants.CURRENT_FOLDER);
      TestExecutor executor = ExecutorCreator.createExecutor(folders, transformer, new EnvironmentVariables());

      // File logFile = new File(folders.getLogFolder(), "test.txt");
      TestCase testcase = new TestCase("de.dagere.peass.ExampleBenchmark#testMethod");
      executor.prepareKoPeMeExecution(new File(folders.getMeasureLogFolder(), "compile.txt"));
      executor.executeTest(testcase, folders.getMeasureLogFolder(), 100);

      File clazzFolder = folders.findTempClazzFolder(testcase).get(0);

      Kopemedata data = JSONDataLoader.loadData(new File(clazzFolder, testcase.getMethod() + ".json"));

      TestMethod testcaseData = data.getFirstMethodResult();
      Assert.assertEquals("de.dagere.peass.ExampleBenchmark", data.getClazz());
      Assert.assertEquals("testMethod", testcaseData.getMethod());

      Assert.assertEquals(6, testcaseData.getDatacollectorResults().get(0).getResults().size());
      List<VMResult> results = testcaseData.getDatacollectorResults().get(0).getResults();
      Assert.assertEquals(4, results.get(0).getFulldata().getValues().size());

      List<String> params = results
            .stream()
            .map(result -> result.getParameters()
                  .entrySet()
                  .stream()
                  .map(param -> param.getKey() + "-" + param.getValue())
                  .collect(Collectors.joining(" ")))
            .collect(Collectors.toList());
      MatcherAssert.assertThat(params, IsIterableContaining.hasItems("parameter-val1", "parameter-val2"));

      for (VMResult result : results) {
         DescriptiveStatistics statistics = new DescriptiveStatistics();
         result.getFulldata().getValues().forEach(value -> statistics.addValue(value.getValue()));

         Assert.assertEquals(statistics.getMean(), result.getValue(), 0.01);
         Assert.assertEquals(statistics.getStandardDeviation(), result.getDeviation(), 0.01);
      }

   }

}
