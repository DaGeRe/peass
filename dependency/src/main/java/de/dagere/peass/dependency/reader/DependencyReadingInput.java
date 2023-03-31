package de.dagere.peass.dependency.reader;

import java.util.Map;

import de.dagere.nodeDiffDetector.data.Type;
import de.dagere.peass.dependency.changesreading.ClazzChangeData;

public class DependencyReadingInput {
   private final Map<Type, ClazzChangeData> changes;
   private final String predecessor;

   public DependencyReadingInput(final Map<Type, ClazzChangeData> changes, final String predecessor) {
      this.changes = changes;
      this.predecessor = predecessor;
   }

   public Map<Type, ClazzChangeData> getChanges() {
      return changes;
   }

   public String getPredecessor() {
      return predecessor;
   }
}
