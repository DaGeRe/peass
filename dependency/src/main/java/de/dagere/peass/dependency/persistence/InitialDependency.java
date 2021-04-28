package de.dagere.peass.dependency.persistence;

import java.util.LinkedList;
import java.util.List;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;

public class InitialDependency {
   private List<ChangedEntity> entities = new LinkedList<>();

   public List<ChangedEntity> getEntities() {
      return entities;
   }

   public void setEntities(List<ChangedEntity> entities) {
      this.entities = entities;
   }
}