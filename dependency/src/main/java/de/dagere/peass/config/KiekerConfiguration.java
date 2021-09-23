package de.dagere.peass.config;

import java.io.Serializable;

import net.kieker.sourceinstrumentation.AllowedKiekerRecord;

public class KiekerConfiguration implements Serializable {
   
   private static final long serialVersionUID = 3129231099963995908L;

   private boolean useKieker = false;
   private boolean useSourceInstrumentation = true;
   private boolean useSelectiveInstrumentation = true;
   private boolean useSampling = false;
   private boolean useCircularQueue = true;
   private boolean enableAdaptiveMonitoring = false;
   private boolean adaptiveInstrumentation = false;
   private int kiekerAggregationInterval = 5000;
   private AllowedKiekerRecord record = AllowedKiekerRecord.OPERATIONEXECUTION;
   private boolean extractMethod = false;

   public KiekerConfiguration() {
   }

   public KiekerConfiguration(final boolean useKieker) {
      this.useKieker = useKieker;
   }

   public KiekerConfiguration(final KiekerConfiguration other) {
      this.useKieker = other.useKieker;
      this.useSourceInstrumentation = other.useSourceInstrumentation;
      this.useSelectiveInstrumentation = other.useSelectiveInstrumentation;
      this.useSampling = other.useSampling;
      this.useCircularQueue = other.useCircularQueue;
      this.enableAdaptiveMonitoring = other.enableAdaptiveMonitoring;
      this.adaptiveInstrumentation = other.adaptiveInstrumentation;
      this.kiekerAggregationInterval = other.kiekerAggregationInterval;
      this.record = other.record;
      this.extractMethod = other.extractMethod;
   }

   public boolean isUseKieker() {
      return useKieker;
   }

   public void setUseKieker(final boolean useKieker) {
      this.useKieker = useKieker;
   }

   public boolean isUseSourceInstrumentation() {
      return useSourceInstrumentation;
   }

   public void setUseSourceInstrumentation(final boolean useSourceInstrumentation) {
      this.useSourceInstrumentation = useSourceInstrumentation;
   }

   public boolean isUseSelectiveInstrumentation() {
      return useSelectiveInstrumentation;
   }

   public void setUseSelectiveInstrumentation(final boolean useSelectiveInstrumentation) {
      this.useSelectiveInstrumentation = useSelectiveInstrumentation;
   }

   public boolean isUseSampling() {
      return useSampling;
   }

   public void setUseSampling(final boolean useSampling) {
      this.useSampling = useSampling;
   }

   public boolean isUseCircularQueue() {
      return useCircularQueue;
   }

   public void setUseCircularQueue(final boolean useCircularQueue) {
      this.useCircularQueue = useCircularQueue;
   }

   public boolean isEnableAdaptiveMonitoring() {
      return enableAdaptiveMonitoring;
   }

   public void setEnableAdaptiveMonitoring(final boolean enableAdaptiveMonitoring) {
      this.enableAdaptiveMonitoring = enableAdaptiveMonitoring;
   }
   
   public boolean isAdaptiveInstrumentation() {
      return adaptiveInstrumentation;
   }

   public void setAdaptiveInstrumentation(final boolean adaptiveInstrumentation) {
      this.adaptiveInstrumentation = adaptiveInstrumentation;
   }

   public int getKiekerAggregationInterval() {
      return kiekerAggregationInterval;
   }

   public void setKiekerAggregationInterval(final int kiekerAggregationInterval) {
      this.kiekerAggregationInterval = kiekerAggregationInterval;
   }

   public AllowedKiekerRecord getRecord() {
      return record;
   }
   
   public void setRecord(final AllowedKiekerRecord record) {
      if (record == null) {
         this.record = AllowedKiekerRecord.OPERATIONEXECUTION;
      } else {
         this.record = record;
      }
   }

   public boolean isExtractMethod() {
      return extractMethod;
   }

   public void setExtractMethod(final boolean extractMethod) {
      this.extractMethod = extractMethod;
   }
   
   
}
