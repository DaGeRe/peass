package de.peass.validation.data;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProjectValidation {
   private Map<String, ValidationChange> changes = new LinkedHashMap<>();

   public Map<String, ValidationChange> getChanges() {
      return changes;
   }

   public void setChanges(final Map<String, ValidationChange> changes) {
      this.changes = changes;
   }
}