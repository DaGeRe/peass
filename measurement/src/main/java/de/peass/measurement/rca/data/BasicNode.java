package de.peass.measurement.rca.data;

import java.util.List;

/**
 * Base class for node implementations:
 * - CallTreeNode contains the measured data during measurement
 * - MeasuredNode contains the measured data during serialization
 * - GraphNode contains the measured data transformed for visualization 
 * @author reichelt
 *
 */
public abstract class BasicNode {
   protected String call;
   protected String kiekerPattern;
   private String otherKiekerPattern;
   private String module;
   
   public BasicNode(final String call, final String kiekerPattern, final String otherKiekerPattern) {
      this.call = call;
      this.kiekerPattern = kiekerPattern;
      this.otherKiekerPattern = otherKiekerPattern;
      if (kiekerPattern != null && !kiekerPattern.contains(call.replace("#", "."))) {
         throw new RuntimeException("Pattern " + kiekerPattern + " must contain " + call);
      }
      if (kiekerPattern != null && kiekerPattern.contains("<init>") && !kiekerPattern.contains("new")) {
         throw new RuntimeException("Pattern " + kiekerPattern + " not legal - Constructor must contain new as return type!");
      }
      if (otherKiekerPattern != null && kiekerPattern.contains("<init>") && !kiekerPattern.contains("new")) {
         throw new RuntimeException("Pattern " + kiekerPattern + " not legal - Constructor must contain new as return type!");
      }
   }
   
   public String getModule() {
      return module;
   }
   
   public void setModule(String module) {
      this.module = module;
   }

   public String getCall() {
      return call;
   }
   
   public String getKiekerPattern() {
      return kiekerPattern;
   }
   
   public String getOtherKiekerPattern() {
      return otherKiekerPattern;
   }

   public void setOtherKiekerPattern(final String otherKiekerPattern) {
      this.otherKiekerPattern = otherKiekerPattern;
   }
   
   public abstract List<? extends BasicNode> getChildren();
}
