package de.peran.analysis.helper.comparedata;

import java.util.LinkedHashMap;
import java.util.Map;

import de.peran.measurement.analysis.Statistic;

public class DifferentMeasurements {
   Map<String, Statistic> measurements = new LinkedHashMap<>();

   public Map<String, Statistic> getMeasurements() {
      return measurements;
   }

   public void setMeasurements(final Map<String, Statistic> measurements) {
      this.measurements = measurements;
   }
}