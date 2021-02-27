package de.peass.dependencyprocessors;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.analysis.EarlyBreakDecider;
import de.peass.measurement.analysis.ResultLoader;
import de.peass.measurement.organize.FolderDeterminer;
import de.peass.testtransformation.JUnitTestTransformer;

public class AdaptiveTester extends DependencyTester {

   private static final Logger LOG = LogManager.getLogger(AdaptiveTester.class);

   private int finishedVMs = 0;

   public AdaptiveTester(final PeASSFolders folders, final MeasurementConfiguration measurementConfig)
         throws IOException {
      super(folders, measurementConfig);

   }

   public void evaluate(final TestCase testcase) throws IOException, InterruptedException, JAXBException, XmlPullParserException {
      LOG.info("Executing test " + testcase.getClazz() + " " + testcase.getMethod() + " in versions {} and {}", configuration.getVersionOld(), configuration.getVersion());

      new FolderDeterminer(folders).testResultFolders(configuration.getVersion(), configuration.getVersionOld(), testcase);

      final File logFolder = getLogFolder(configuration.getVersion(), testcase);

      currentChunkStart = System.currentTimeMillis();
      
      ProgressWriter writer = new ProgressWriter(new File(folders.getFullMeasurementFolder(), "progress.txt"), configuration.getVms());
      for (finishedVMs = 0; finishedVMs < configuration.getVms(); finishedVMs++) {
         long comparisonStart = System.currentTimeMillis();
         runOneComparison(logFolder, testcase, finishedVMs);

         final boolean savelyDecidable = checkIsDecidable(testcase, finishedVMs);

         if (savelyDecidable) {
            LOG.debug("Savely decidable - finishing testing");
            break;
         }

         final boolean shouldBreak = updateExecutions(testcase, finishedVMs);
         if (shouldBreak) {
            LOG.debug("Too less executions possible - finishing testing.");
            break;
         }
         long durationInSeconds = (System.currentTimeMillis() - comparisonStart)/1000;
         writer.write(durationInSeconds, finishedVMs);
      }
   }

   public int getFinishedVMs() {
      return finishedVMs;
   }

   public boolean checkIsDecidable(final TestCase testcase, final int vmid) throws JAXBException {
      final boolean savelyDecidable;
      if (configuration.isEarlyStop()) {
         final ResultLoader loader = new ResultLoader(configuration, folders.getFullMeasurementFolder(), testcase, currentChunkStart);
         loader.loadData();
         LOG.debug(loader.getStatisticsAfter());
         DescriptiveStatistics statisticsBefore = loader.getStatisticsBefore();
         DescriptiveStatistics statisticsAfter = loader.getStatisticsAfter();

         final EarlyBreakDecider decider = new EarlyBreakDecider(configuration, statisticsAfter, statisticsBefore);
         decider.setType1error(configuration.getType1error());
         decider.setType2error(configuration.getType2error());
         savelyDecidable = decider.isBreakPossible(vmid);
      } else {
         savelyDecidable = false;
      }
      return savelyDecidable;
   }
}
