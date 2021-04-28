package de.dagere.peass.measurement.rca.serialization;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.dagere.peass.statistics.StatisticUtil;

public class MeasuredValues {
   
   private static final Logger LOG = LogManager.getLogger(MeasuredValues.class);
   
   private Map<Integer, List<StatisticalSummary>> values = new TreeMap<>();

   // @JsonSerialize(contentUsing = SummaryStatisticsSerializer.class)
   // @JsonDeserialize(contentUsing = SummaryStatisticsDeserializer.class)
   public Map<Integer, List<StatisticalSummary>> getValues() {
      return values;
   }

   public void setValues(final Map<Integer, List<StatisticalSummary>> values) {
      this.values = values;
   }

   @JsonIgnore
   public double[] getValuesArray() {
      double[] aggregated = new double[values.size()];
      for (int i = 0; i < values.size(); i++) {
         double mean = StatisticUtil.getMean(values.get(i));
         aggregated[i] = mean;
         LOG.debug("Mean " + i + " " + mean);
      }
      return aggregated;
   }

}