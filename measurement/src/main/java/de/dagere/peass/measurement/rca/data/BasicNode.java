package de.dagere.peass.measurement.rca.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.dagere.nodeDiffDetector.data.MethodCall;
import de.dagere.peass.measurement.rca.serialization.MeasuredNode;

/**
 * Base class for node implementations:
 * <ul>
 * <li>{@link CallTreeNode} contains the measured data during measurement</li>
 * <li>{@link MeasuredNode} contains the measured data during serialization</li>
 * <li>{@link GraphNode} contains the measured data transformed for visualization</li>
 * </ul>
 * 
 * @author reichelt
 *
 */
public abstract class BasicNode {
   protected String call;
   protected String kiekerPattern;
   private String otherKiekerPattern;
   protected String module;

   public BasicNode(final String call, final String kiekerPattern, final String otherKiekerPattern) {
      int moduleSeparatorIndex = call.lastIndexOf(MethodCall.MODULE_SEPARATOR);
      if (moduleSeparatorIndex != -1) {
         this.call = call.substring(moduleSeparatorIndex + 1);
         this.module = call.substring(0, moduleSeparatorIndex);
         this.kiekerPattern = kiekerPattern != null ? kiekerPattern.replaceFirst(module + MethodCall.MODULE_SEPARATOR, "") : null;
         this.otherKiekerPattern = otherKiekerPattern != null ? otherKiekerPattern.replaceFirst(module + MethodCall.MODULE_SEPARATOR, "") : null;
      } else {
         this.call = call;
         this.kiekerPattern = kiekerPattern;
         this.otherKiekerPattern = otherKiekerPattern;
      }

      if (kiekerPattern != null && !CauseSearchData.ADDED.equals(kiekerPattern) && !kiekerPattern.contains(this.call.replace("#", "."))) {
         throw new RuntimeException("Pattern " + kiekerPattern + " must contain " + this.call.replace("#", "."));
      }
      if (kiekerPattern != null && kiekerPattern.contains("<init>") && !kiekerPattern.contains("new")) {
         throw new RuntimeException("Pattern " + kiekerPattern + " not legal - Constructor must contain new as return type!");
      }
      if (otherKiekerPattern != null && kiekerPattern.contains("<init>") && !kiekerPattern.contains("new")) {
         throw new RuntimeException("Pattern " + kiekerPattern + " not legal - Constructor must contain new as return type!");
      }
      if (kiekerPattern != null && kiekerPattern.contains("new new ")) {
         throw new RuntimeException("Illegal duplication of new identifier!");
      }
      if (otherKiekerPattern != null && otherKiekerPattern.contains("new new ")) {
         throw new RuntimeException("Illegal duplication of new identifier!");
      }
      if (kiekerPattern != null && !CauseSearchData.ADDED.equals(kiekerPattern) && (!kiekerPattern.contains("(") || !kiekerPattern.contains(")"))) {
         throw new RuntimeException("KiekerPattern " + kiekerPattern + " needs to contain parenthesis, at least () for empty parameters");
      }
      if (otherKiekerPattern != null && !CauseSearchData.ADDED.equals(otherKiekerPattern) && (!otherKiekerPattern.contains("(") || !otherKiekerPattern.contains(")"))) {
         throw new RuntimeException("KiekerPattern " + otherKiekerPattern + " needs to contain parenthesis, at least () for empty parameters");
      }
   }

   @JsonInclude(JsonInclude.Include.NON_NULL)
   public String getModule() {
      return module;
   }

   public void setModule(final String module) {
      this.module = module;
   }

   public String getCall() {
      return call;
   }

   public void setCall(String call) { this.call = call; }

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
