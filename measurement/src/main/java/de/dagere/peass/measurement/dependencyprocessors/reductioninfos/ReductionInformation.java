package de.dagere.peass.measurement.dependencyprocessors.reductioninfos;

import java.util.LinkedHashMap;
import java.util.Map;

public class ReductionInformation {
   private Map<Integer, IterationReduction> reductions = new LinkedHashMap<>();

   public Map<Integer, IterationReduction> getReductions() {
      return reductions;
   }

   public void setReductions(Map<Integer, IterationReduction> reductions) {
      this.reductions = reductions;
   }

   public void addReduction(int vmid, VMReductionInfo reductionOld, VMReductionInfo reductionCurrent) {
      IterationReduction reduction = new IterationReduction(reductionOld, reductionCurrent);
      reductions.put(vmid, reduction);
   }
}
