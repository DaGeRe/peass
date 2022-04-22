package de.dagere.peass.jmh;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.io.FileMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.MeasurementStrategy;
import de.dagere.peass.config.WorkloadType;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.jmh.JmhTestExecutor;
import de.dagere.peass.dependency.jmh.JmhTestTransformer;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;

public class JmhParallelExecutionTest {

   private static final File BASE_FOLDER = new File("target/jmh/");
   private static final TestCase test = new TestCase("de.dagere.peass#ExampleBenchmark");

   @BeforeEach
   public void clean() throws IOException {
      if (BASE_FOLDER.exists()) {
         FileUtils.cleanDirectory(BASE_FOLDER);
      }
      BASE_FOLDER.mkdirs();
   }

   @Test
   public void testParallelExecution() throws IOException, InterruptedException, XmlPullParserException {
      File[] testFolders = new File[2];
      JmhTestExecutor[] executors = new JmhTestExecutor[2];
      Thread[] threads = new Thread[2];

      MeasurementConfig measurementConfig = new MeasurementConfig(2);
      measurementConfig.setIterations(10);
      measurementConfig.setRepetitions(2);
      measurementConfig.setMeasurementStrategy(MeasurementStrategy.PARALLEL);
      measurementConfig.getExecutionConfig().setTestExecutor(WorkloadType.JMH.getTestExecutor());

      prepareThreads(testFolders, executors, threads, measurementConfig);

      startThreads(threads);

      waitForResult(threads);

      for (int i = 0; i < 2; i++) {
         File expectedFile = new File(BASE_FOLDER, "jmh-" + i + "_peass/measurementsTemp/de.dagere.peass/example/de.dagere.peass/testMethod.json");
         MatcherAssert.assertThat(expectedFile, FileMatchers.anExistingFile());
      }
   }

   private void waitForResult(final Thread[] threads) throws InterruptedException {
      for (int i = 0; i < 2; i++) {
         threads[i].join();
      }
   }

   private void startThreads(final Thread[] threads) {
      for (int i = 0; i < 2; i++) {
         threads[i].start();
      }
   }

   private void prepareThreads(final File[] testFolders, final JmhTestExecutor[] executors, final Thread[] threads, final MeasurementConfig measurementConfig)
         throws IOException, InterruptedException, XmlPullParserException {
      for (int i = 0; i < 2; i++) {
         testFolders[i] = new File(BASE_FOLDER, "jmh-" + i);
         FileUtils.copyDirectory(JmhTestConstants.BASIC_VERSION, testFolders[i]);
         PeassFolders folders = new PeassFolders(testFolders[i]);

         JmhTestTransformer transformer = new JmhTestTransformer(testFolders[i], measurementConfig);
         executors[i] = new JmhTestExecutor(folders, transformer, new EnvironmentVariables());

         executors[i].prepareKoPeMeExecution(new File(folders.getMeasureLogFolder(), "clean.txt"));
         final int j = i;
         threads[i] = new Thread(new Runnable() {

            @Override
            public void run() {
               File logFolder = new File(BASE_FOLDER, "" + j);
               logFolder.mkdirs();
               executors[j].executeTest(test, logFolder, 100);
            }
         });
      }
   }
}
