package de.dagere.peass.dependency.reader;

import java.util.Map;

import de.dagere.nodeDiffGenerator.data.MethodCall;
import de.dagere.peass.dependency.changesreading.ClazzChangeData;

public class DependencyReadingInput {
   private final Map<MethodCall, ClazzChangeData> changes;
   private final String predecessor;

   public DependencyReadingInput(final Map<MethodCall, ClazzChangeData> changes, final String predecessor) {
      this.changes = changes;
      this.predecessor = predecessor;
   }

   public Map<MethodCall, ClazzChangeData> getChanges() {
      return changes;
   }

   public String getPredecessor() {
      return predecessor;
   }
}
