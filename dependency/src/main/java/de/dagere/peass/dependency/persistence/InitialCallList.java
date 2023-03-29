package de.dagere.peass.dependency.persistence;

import java.util.LinkedList;
import java.util.List;

import de.dagere.nodeDiffDetector.data.MethodCall;

public class InitialCallList {
   private List<MethodCall> entities = new LinkedList<>();

   public List<MethodCall> getEntities() {
      return entities;
   }

   public void setEntities(List<MethodCall> entities) {
      this.entities = entities;
   }
}