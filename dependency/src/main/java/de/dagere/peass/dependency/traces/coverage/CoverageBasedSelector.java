package de.dagere.peass.dependency.traces.coverage;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class CoverageBasedSelector {
   
   private static final Logger LOG = LogManager.getLogger(CoverageBasedSelector.class);
   
   public static List<TestCase> selectBasedOnCoverage(final List<TraceCallSummary> summaries, final Set<ChangedEntity> changes) {
      List<TraceCallSummary> copiedSummaries = new LinkedList<>(summaries);
      Set<ChangedEntity> copiedChanges = new HashSet<>(changes);
      boolean changed = true;
      List<TestCase> resultTests = new LinkedList<>();
      while (copiedSummaries.size() > 0 && copiedChanges.size() > 0 && changed) {
         changed = false;

         TraceCallSummary selected = selectMaximumCalled(copiedChanges, copiedSummaries);

         if (selected != null) {
            resultTests.add(selected.getTestcase());
            copiedSummaries.remove(selected);
            LOG.debug("Selected: {} with summary {}", selected.getTestcase(), selected);
            changed = removeUnneededChanges(copiedChanges, changed, selected);
         }
         
      }

      return resultTests;
   }

   private static boolean removeUnneededChanges(final Set<ChangedEntity> changes, boolean changed, final TraceCallSummary selected) {
      for (Iterator<ChangedEntity> changeIterator = changes.iterator(); changeIterator.hasNext();) {
         ChangedEntity change = changeIterator.next();
         String currentChangeSignature = change.toString();
         if (selected.getCallCounts().containsKey(currentChangeSignature) && selected.getCallCounts().get(currentChangeSignature) > 0) {
            changeIterator.remove();
            changed = true;
         }
      }
      return changed;
   }

   private static TraceCallSummary selectMaximumCalled(final Set<ChangedEntity> changes, final List<TraceCallSummary> copiedSummaries) {
      TraceCallSummary selected = copiedSummaries.get(0);
      int selectedCallSum = getCallSum(changes, selected);
      for (TraceCallSummary current : copiedSummaries) {
         int currentCallSum = getCallSum(changes, current);
         if (currentCallSum > selectedCallSum) {
            selectedCallSum = currentCallSum;
            selected = current;
         }
      }
      if (selectedCallSum > 0) {
         return selected;
      } else {
         return null;
      }
   }

   private static int getCallSum(final Set<ChangedEntity> changes, final TraceCallSummary summary) {
      int currentCallSum = 0;
      for (ChangedEntity change : changes) {
         String parameters = change.getParametersPrintable().length() > 0 ? "(" + change.getParametersPrintable() + ")" : "";
         String changeSignature = change.toString() + parameters;
         if (summary.getCallCounts().containsKey(changeSignature)) {
            currentCallSum += summary.getCallCounts().get(changeSignature);
         }
      }
      return currentCallSum;
   }
}
