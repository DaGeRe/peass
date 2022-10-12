package de.dagere.peass.measurement.dependencyprocessors.reductioninfos;

public class IterationReduction {
   private VMReductionInfo reductionOld;
   private VMReductionInfo reductionCurrent;

   public IterationReduction(VMReductionInfo reductionOld, VMReductionInfo reductionCurrent) {
      this.reductionOld = reductionOld;
      this.reductionCurrent = reductionCurrent;
   }

   public VMReductionInfo getReductionOld() {
      return reductionOld;
   }

   public void setReductionOld(VMReductionInfo reductionOld) {
      this.reductionOld = reductionOld;
   }

   public VMReductionInfo getReductionCurrent() {
      return reductionCurrent;
   }

   public void setReductionCurrent(VMReductionInfo reductionCurrent) {
      this.reductionCurrent = reductionCurrent;
   }

}