package de.peass.measurement.rca.serialization;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

public class MeasuredValues {
   private Map<Integer, List<StatisticalSummary>> values = new TreeMap<>();

//   @JsonSerialize(contentUsing = SummaryStatisticsSerializer.class)
//   @JsonDeserialize(contentUsing = SummaryStatisticsDeserializer.class)
   public Map<Integer, List<StatisticalSummary>> getValues() {
      return values;
   }

   public void setValues(final Map<Integer, List<StatisticalSummary>> values) {
      this.values = values;
   }

}