package de.dagere.peass.jmh;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;



import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
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
import de.dagere.peass.config.WorkloadType;
import de.dagere.peass.dependency.ExecutorCreator;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.jmh.JmhTestTransformer;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;

public class JmhIterationTest {
   
   private static final int VMS = 3;
   private static final int WARMUP = 2;
   private static final int ITERATIONS = 4;

   @BeforeEach
   public void clearCurrent() throws IOException {
      TestUtil.deleteContents(TestConstants.CURRENT_FOLDER);
      TestUtil.deleteContents(TestConstants.CURRENT_PEASS);
   }
   
   @Test
   public void testVersionReading() throws IOException, InterruptedException, XmlPullParserException, ParseException, ViewNotFoundException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
      FileUtils.copyDirectory(JmhTestConstants.BASIC_VERSION, TestConstants.CURRENT_FOLDER);
      
      MeasurementConfig measurementConfig = new MeasurementConfig(VMS);
      measurementConfig.setIterations(ITERATIONS);
      measurementConfig.setWarmup(WARMUP);
      measurementConfig.getExecutionConfig().setTestExecutor(WorkloadType.JMH.getTestExecutor());
      JmhTestTransformer transformer = new JmhTestTransformer(TestConstants.CURRENT_FOLDER, measurementConfig);
      PeassFolders folders = new PeassFolders(TestConstants.CURRENT_FOLDER);
      TestExecutor executor = ExecutorCreator.createExecutor(folders, transformer, new EnvironmentVariables());
    
//      File logFile = new File(folders.getLogFolder(), "test.txt");
      TestCase testcase = new TestCase("de.dagere.peass.ExampleBenchmark#testMethod");
      executor.prepareKoPeMeExecution(new File(folders.getMeasureLogFolder(), "compile.txt"));
      for (int i = 0; i < 3; i++) {
         executor.executeTest(testcase, folders.getMeasureLogFolder(), 100);
      }
      
      File clazzFolder = folders.findTempClazzFolder(testcase).get(0);
      Kopemedata data = JSONDataLoader.loadData(new File(clazzFolder, testcase.getMethod() + ".json"));
      
      TestMethod testcaseData = data.getMethods().get(0);
      Assert.assertEquals("de.dagere.peass.ExampleBenchmark", data.getClazz());
      Assert.assertEquals("testMethod", testcaseData.getMethod());
      
      Assert.assertEquals(VMS, testcaseData.getDatacollectorResults().get(0).getResults().size());
      List<VMResult> results = testcaseData.getDatacollectorResults().get(0).getResults();
      Assert.assertEquals(ITERATIONS + WARMUP, results.get(0).getFulldata().getValues().size());
      
      for (VMResult result : results) {
         DescriptiveStatistics statistics = new DescriptiveStatistics();
         result.getFulldata().getValues().forEach(value -> statistics.addValue(value.getValue()));
         
         Assert.assertEquals(statistics.getMean(), result.getValue(), 0.01);
         Assert.assertEquals(statistics.getStandardDeviation(), result.getDeviation(), 0.01);
         
      }
   }

}
