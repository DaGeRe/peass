package de.dagere.peass.measurement.dependencyprocessors.helper;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.statistics.Relation;
import de.dagere.peass.measurement.statistics.StatisticUtil;


public class EarlyBreakDecider {

   private static final Logger LOG = LogManager.getLogger(EarlyBreakDecider.class);

   private final StatisticalSummary statisticsPredecessor;
   private final StatisticalSummary statisticsCurrent;

   private final double type1error;
   private final double type2error;

   public EarlyBreakDecider(final MeasurementConfig config, final StatisticalSummary statisticsPredecessor, final StatisticalSummary statisticsCurrent)  {
      this.type1error = config.getStatisticsConfig().getType1error();
      this.type2error = config.getStatisticsConfig().getType2error();
      this.statisticsPredecessor = statisticsPredecessor;
      this.statisticsCurrent = statisticsCurrent;
   }

   public boolean isBreakPossible(final int vmid) {
      boolean savelyDecidable = false;
      if (vmid > 3) {
         LOG.debug("T: {} {}", statisticsPredecessor.getN(), statisticsCurrent.getN());
         if ((statisticsPredecessor.getN() > 3 && statisticsCurrent.getN() > 3)) {
            savelyDecidable = isSavelyDecidableBothHypothesis(vmid);
         } else if (vmid > 10) {
            LOG.debug("More than 10 executions and only {} / {} measurements - aborting", statisticsPredecessor.getN(), statisticsCurrent.getN());
            return true;
         }
         // T statistic can not be determined if less than 2 values (produces exception..)
      }
      return savelyDecidable;
   }

   public boolean isSavelyDecidableBothHypothesis(final int vmid) {
      boolean savelyDecidable = false;
      if (statisticsPredecessor.getN() > 30 && statisticsCurrent.getN() > 30) {
         final Relation relation = StatisticUtil.agnosticTTest(statisticsPredecessor, statisticsCurrent, type1error, type2error);
         if (relation == Relation.EQUAL || relation == Relation.UNEQUAL) {
            LOG.info("Can savely decide: {}", relation);
            savelyDecidable = true;
         }
      }
      return savelyDecidable;
   }


}
