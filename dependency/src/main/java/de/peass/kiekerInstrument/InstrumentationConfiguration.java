package de.peass.kiekerInstrument;

import java.util.Set;

import de.peass.dependency.execution.AllowedKiekerRecord;

public class InstrumentationConfiguration {
   private final AllowedKiekerRecord usedRecord;
   private final boolean sample;
   private final boolean createDefaultConstructor;
   private final boolean enableAdaptiveMonitoring;
   private final Set<String> includedPatterns;
   
   /**
    * Simple constructor, setting default values for everything except usedRecord, sample and includedPatterns
    */
   public InstrumentationConfiguration(AllowedKiekerRecord usedRecord, boolean sample, Set<String> includedPatterns) {
      this.usedRecord = usedRecord;
      this.sample = sample;
      this.includedPatterns = includedPatterns;
      this.enableAdaptiveMonitoring = false;
      this.createDefaultConstructor = true;
   }

   public InstrumentationConfiguration(AllowedKiekerRecord usedRecord, boolean sample, boolean createDefaultConstructor, boolean enableAdaptiveMonitoring,
         Set<String> includedPatterns) {
      this.usedRecord = usedRecord;
      this.sample = sample;
      this.createDefaultConstructor = createDefaultConstructor;
      this.enableAdaptiveMonitoring = enableAdaptiveMonitoring;
      this.includedPatterns = includedPatterns;
   }

   public AllowedKiekerRecord getUsedRecord() {
      return usedRecord;
   }

   public boolean isSample() {
      return sample;
   }

   public boolean isCreateDefaultConstructor() {
      return createDefaultConstructor;
   }

   public Set<String> getIncludedPatterns() {
      return includedPatterns;
   }
   
   public boolean isEnableAdaptiveMonitoring() {
      return enableAdaptiveMonitoring;
   }

}