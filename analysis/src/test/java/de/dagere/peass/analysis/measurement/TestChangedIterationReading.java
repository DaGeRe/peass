package de.dagere.peass.analysis.measurement;

import java.io.File;
import java.util.Arrays;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.StatisticsConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependencyprocessors.CommitByNameComparator;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;

public class TestChangedIterationReading {

   private final static File DATA_FOLDER = new File(TestAnalyseFullData.DATA_READING_FOLDER, "changedIterations");

   @Test
   public void testChangedIterationReading() throws InterruptedException {
      CommitComparatorInstance comparator = new CommitComparatorInstance(Arrays.asList(
            "a23e385264c31def8dcda86c3cf64faa698c62d8", "33ce17c04b5218c25c40137d4d09f40fbb3e4f0f"));

      ProjectStatistics statistics = new ProjectStatistics(CommitByNameComparator.INSTANCE);
      AnalyseFullData afd = new AnalyseFullData(new File("target/changes.json"), statistics, null, new StatisticsConfig(), comparator);
      afd.analyseFolder(new File(DATA_FOLDER, "measurement_a23e385264c31def8dcda86c3cf64faa698c62d8_33ce17c04b5218c25c40137d4d09f40fbb3e4f0f/measurements"));

      TestcaseStatistic testcaseStatistic = statistics.getStatistics().get("a23e385264c31def8dcda86c3cf64faa698c62d8")
            .get(new TestMethodCall("de.test.CalleeTest", "onlyCallMethod2"));
      System.out.println(testcaseStatistic);

      System.out.println(testcaseStatistic.getDeviationCurrent());
      System.out.println(testcaseStatistic.getDeviationOld());

      MatcherAssert.assertThat(testcaseStatistic.getDeviationCurrent(), Matchers.lessThan(800000d));
      MatcherAssert.assertThat(testcaseStatistic.getDeviationOld(), Matchers.lessThan(800000d));
   }
}
