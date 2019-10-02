package de.peass.breaksearch.treeanalysis;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

class CorrelatedPair {
   List<Double> val1 = new LinkedList<>();
   List<Double> val2 = new LinkedList<>();

   void addValue(final double v1, final double v2) {
      val1.add(v1);
      val2.add(v2);
   }

   double[] getVal1() {
      return ArrayUtils.toPrimitive(val1.toArray(new Double[0]));
   }

   double[] getVal2() {
      return ArrayUtils.toPrimitive(val2.toArray(new Double[0]));
   }

   double getPearsonCorrelation() {
      return new PearsonsCorrelation().correlation(getVal1(), getVal2());
   }
}