package de.peran.analysis.helper.comparedata;

import java.util.LinkedHashMap;
import java.util.Map;

import de.peass.measurement.analysis.statistics.TestcaseStatistic;

public class DifferentMeasurements {
   Map<String, TestcaseStatistic> measurements = new LinkedHashMap<>();

   public Map<String, TestcaseStatistic> getMeasurements() {
      return measurements;
   }

   public void setMeasurements(final Map<String, TestcaseStatistic> measurements) {
      this.measurements = measurements;
   }
}