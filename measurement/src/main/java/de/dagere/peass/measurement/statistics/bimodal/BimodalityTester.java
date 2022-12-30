package de.dagere.peass.measurement.statistics.bimodal;

import org.apache.commons.math3.stat.inference.TTest;

import de.dagere.peass.measurement.statistics.Relation;

public class BimodalityTester {

   private final CompareData data;

   final IsBimodal dataPredecessor;
   final IsBimodal dataCurrent;

   private final boolean isBimodal;

   private final Relation relation;

   public BimodalityTester(final CompareData data) {
      this.data = data;

      dataPredecessor = new IsBimodal(data.getPredecessor(), data.getPredecessorStat());
      dataCurrent = new IsBimodal(data.getCurrent(), data.getCurrentStat());

      isBimodal = dataPredecessor.isBimodal()
            && dataCurrent.isBimodal();

      if (isBimodal) {
         final boolean firstSmaller = dataPredecessor.getStat1().getMean() < dataCurrent.getStat1().getMean();
         final boolean secondSmaller = dataPredecessor.getStat2().getMean() < dataCurrent.getStat2().getMean();
         if (firstSmaller && secondSmaller) {
            relation = Relation.LESS_THAN;
         } else if (!firstSmaller && !secondSmaller) {
            relation = Relation.GREATER_THAN;
         } else {
            relation = Relation.EQUAL;
         }
      } else {
         if (data.getAvgPredecessor() < data.getAvgCurrent()) {
            relation = Relation.LESS_THAN;
         } else {
            relation = Relation.GREATER_THAN;
         }
      }
   }
   
   public IsBimodal getDataPredecessor() {
      return dataPredecessor;
   }
   
   public IsBimodal getDataAfter() {
      return dataCurrent;
   }

   public boolean isTChange(final double significance) {
      if (isBimodal) {
         final boolean firstChanged = new TTest().homoscedasticTTest(dataPredecessor.getStat1(), dataCurrent.getStat1()) < significance;
         final boolean secondChanged = new TTest().homoscedasticTTest(dataPredecessor.getStat2(), dataCurrent.getStat2()) < significance;
         final boolean firstSmaller = dataPredecessor.getStat1().getMean() < dataCurrent.getStat1().getMean();
         final boolean secondSmaller = dataPredecessor.getStat2().getMean() < dataCurrent.getStat2().getMean();
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
