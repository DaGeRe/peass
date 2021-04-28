package de.dagere.peass.measurement.rca.data;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

public interface OneVMResult {
   double getAverage();
   
   long getCalls();

   List<StatisticalSummary> getValues();
}