package de.dagere.peass.measurement.dependencyprocessors.reductioninfos;

public class VMReductionInfo {
   private final ReductionReasons reason;
   private final int reductionToIterationCount;
   
   public VMReductionInfo(int reductionToIterationCount) {
      this.reductionToIterationCount = reductionToIterationCount;
      reason = ReductionReasons.NO_REDUCTION;
   }
   
   public VMReductionInfo(int reductionToIterationCount, ReductionReasons reason) {
      this.reductionToIterationCount = reductionToIterationCount;
      this.reason = reason;
   }

   public ReductionReasons getReason() {
      return reason;
   }
   
   public int getReductionToIterationCount() {
      return reductionToIterationCount;
   }
   
}