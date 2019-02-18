package de.peass.analysis.groups;

import java.util.Set;
import java.util.TreeSet;

public class TestcaseClass {
   
   private String direction;
   private Boolean isFunctionalChange = null;
   private Set<String> guessedTypes = new TreeSet<>();
   private Set<String> types = new TreeSet<>();

   public Set<String> getTypes() {
      return types;
   }

   public void setTypes(final Set<String> types) {
      this.types = types;
   }

   public Boolean isFunctionalChange() {
      return isFunctionalChange;
   }

   public void setFunctionalChange(final Boolean isFunctionalChange) {
      this.isFunctionalChange = isFunctionalChange;
   }

   public Set<String> getGuessedTypes() {
      return guessedTypes;
   }

   public void setGuessedTypes(final Set<String> guessedTypes) {
      this.guessedTypes = guessedTypes;
   }

   public void merge(final TestcaseClass value) {
      if (types != null && value.getTypes() != null) {
         types.addAll(value.getTypes());
      }else {
         types = value.getTypes();
      }
      if (value.isFunctionalChange() != null) {
         setFunctionalChange(value.isFunctionalChange());
      }
      if (value.getDirection() != null) {
         direction = value.getDirection();
      }
   }

   public String getDirection() {
      return direction;
   }

   public void setDirection(final String direction) {
      this.direction = direction;
   }

}
