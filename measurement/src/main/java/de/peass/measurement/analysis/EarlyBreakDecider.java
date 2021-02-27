package de.peass.measurement.analysis;

import javax.xml.bind.JAXBException;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.config.MeasurementConfiguration;

public class EarlyBreakDecider {

   private static final Logger LOG = LogManager.getLogger(EarlyBreakDecider.class);

   private final StatisticalSummary statisticsBefore;
   private final StatisticalSummary statisticsAfter;

   private double type1error = 0.01;
   private double type2error = 0.01;

   public EarlyBreakDecider(final MeasurementConfiguration config, final StatisticalSummary statisticsBefore, final StatisticalSummary statisticsAfter) throws JAXBException {
      this.type1error = config.getType1error();
      this.type2error = config.getType2error();
      this.statisticsBefore = statisticsBefore;
      this.statisticsAfter = statisticsAfter;
   }

   public boolean isBreakPossible(final int vmid) {
      boolean savelyDecidable = false;
      if (vmid > 3) {
         LOG.debug("T: {} {}", statisticsBefore.getN(), statisticsAfter.getN());
         if ((statisticsBefore.getN() > 3 && statisticsAfter.getN() > 3)) {
            savelyDecidable = isSavelyDecidableBothHypothesis(vmid);
         } else if (vmid > 10) {
            LOG.debug("More than 10 executions and only {} / {} measurements - aborting", statisticsBefore.getN(), statisticsAfter.getN());
            return true;
         }
         // T statistic can not be determined if less than 2 values (produces exception..)
      }
      return savelyDecidable;
   }

   public boolean isSavelyDecidableBothHypothesis(final int vmid) {
      boolean savelyDecidable = false;
      if (statisticsBefore.getN() > 30 && statisticsAfter.getN() > 30) {
         final Relation relation = StatisticUtil.agnosticTTest(statisticsBefore, statisticsAfter, type1error, type2error);
         if (relation == Relation.EQUAL || relation == Relation.UNEQUAL) {
            LOG.info("Can savely decide: {}", relation);
            savelyDecidable = true;
         }
      }
      return savelyDecidable;
   }

   public double getType1error() {
      return type1error;
   }

   public void setType1error(final double type1error) {
      this.type1error = type1error;
   }

   public double getType2error() {
      return type2error;
   }

   public void setType2error(final double type2error) {
      this.type2error = type2error;
   }

}
