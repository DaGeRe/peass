package de.peass.measurement.searchcause.serialization;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MeasuredValues {
   private Map<Integer, List<Double>> values = new TreeMap<>();

   public Map<Integer, List<Double>> getValues() {
      return values;
   }

   public void setValues(final Map<Integer, List<Double>> values) {
      this.values = values;
   }

}