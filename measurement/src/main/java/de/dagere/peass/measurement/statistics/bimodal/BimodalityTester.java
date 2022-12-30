package de.dagere.peass.measurement.statistics.bimodal;

import org.apache.commons.math3.stat.inference.TTest;

import de.dagere.peass.measurement.statistics.Relation;

public class BimodalityTester {

   private final CompareData data;

   final IsBimodal dataBefore;
   final IsBimodal dataAfter;

   private final boolean isBimodal;

   private final Relation relation;

   public BimodalityTester(final CompareData data) {
      this.data = data;

      dataBefore = new IsBimodal(data.getPredecessor(), data.getPredecessorStat());
      dataAfter = new IsBimodal(data.getCurrent(), data.getCurrentStat());

      isBimodal = dataBefore.isBimodal()
            && dataAfter.isBimodal();

      if (isBimodal) {
         final boolean firstSmaller = dataBefore.getStat1().getMean() < dataAfter.getStat1().getMean();
         final boolean secondSmaller = dataBefore.getStat2().getMean() < dataAfter.getStat2().getMean();
         if (firstSmaller && secondSmaller) {
            relation = Relation.LESS_THAN;
         } else if (!firstSmaller && !secondSmaller) {
            relation = Relation.GREATER_THAN;
         } else {
            relation = Relation.EQUAL;
         }
      } else {
         if (data.getAvgBefore() < data.getAvgAfter()) {
            relation = Relation.LESS_THAN;
         } else {
            relation = Relation.GREATER_THAN;
         }
      }
   }
   
   public IsBimodal getDataBefore() {
      return dataBefore;
   }
   
   public IsBimodal getDataAfter() {
      return dataAfter;
   }

   public boolean isTChange(final double significance) {
      if (isBimodal) {
         final boolean firstChanged = new TTest().homoscedasticTTest(dataBefore.getStat1(), dataAfter.getStat1()) < significance;
         final boolean secondChanged = new TTest().homoscedasticTTest(dataBefore.getStat2(), dataAfter.getStat2()) < significance;
         final boolean firstSmaller = dataBefore.getStat1().getMean() < dataAfter.getStat1().getMean();
         final boolean secondSmaller = dataBefore.getStat2().getMean() < dataAfter.getStat2().getMean();
         final boolean sameDirection = (firstSmaller == secondSmaller);
         boolean isChange = firstChanged && secondChanged && sameDirection;
         return isChange;
      } else {
         return new TTest().homoscedasticTTest(data.getPredecessor(), data.getCurrent(), significance);
      }
   }
   
   public Relation getRelation() {
      return relation;
   }

   public boolean isBimodal() {
      return isBimodal;
   }
}
