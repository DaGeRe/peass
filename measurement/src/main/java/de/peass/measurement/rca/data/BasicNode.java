package de.peass.measurement.rca.data;

import java.util.List;

public abstract class BasicNode {
   protected String call;
   protected String kiekerPattern;
   
   public BasicNode(final String call, final String kiekerPattern) {
      this.call = call;
      this.kiekerPattern = kiekerPattern;
   }

   public String getCall() {
      return call;
   }
   
   public String getKiekerPattern() {
      return kiekerPattern;
   }
   
   public abstract List<? extends BasicNode> getChildren();
}
