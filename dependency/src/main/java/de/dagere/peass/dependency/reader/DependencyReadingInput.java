package de.dagere.peass.dependency.reader;

import java.util.Map;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.changesreading.ClazzChangeData;

public class DependencyReadingInput {
   private final Map<ChangedEntity, ClazzChangeData> changes;
   private final String predecessor;

   public DependencyReadingInput(final Map<ChangedEntity, ClazzChangeData> changes, final String predecessor) {
      this.changes = changes;
      this.predecessor = predecessor;
   }

   public Map<ChangedEntity, ClazzChangeData> getChanges() {
      return changes;
   }

   public String getPredecessor() {
      return predecessor;
   }
}
