package de.peass.kiekerInstrument;

import java.util.Set;

import de.peass.dependency.execution.AllowedKiekerRecord;

public class InstrumentationConfiguration {
   private final AllowedKiekerRecord usedRecord;
   private final boolean sample;
   private final boolean enableDeactivation;
   private final boolean createDefaultConstructor;
   private final boolean enableAdaptiveMonitoring;
   private final Set<String> includedPatterns;
   
   /**
    * Simple constructor, setting default values for everything except usedRecord, sample and includedPatterns
    */
   public InstrumentationConfiguration(final AllowedKiekerRecord usedRecord, final boolean sample, final Set<String> includedPatterns, final boolean enableAdaptiveMonitoring, final boolean enableDecativation) {
      this.usedRecord = usedRecord;
      this.sample = sample;
      this.includedPatterns = includedPatterns;
      this.enableAdaptiveMonitoring = enableAdaptiveMonitoring;
      this.createDefaultConstructor = true;
      this.enableDeactivation = enableDecativation;
   }

   public InstrumentationConfiguration(final AllowedKiekerRecord usedRecord, final boolean sample, final boolean createDefaultConstructor, final boolean enableAdaptiveMonitoring,
         final Set<String> includedPatterns, final boolean enableDecativation) {
      this.usedRecord = usedRecord;
      this.sample = sample;
      this.createDefaultConstructor = createDefaultConstructor;
      this.enableAdaptiveMonitoring = enableAdaptiveMonitoring;
      this.includedPatterns = includedPatterns;
      this.enableDeactivation = enableDecativation;
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
   
   public boolean isEnableDeactivation() {
      return enableDeactivation;
   }

}