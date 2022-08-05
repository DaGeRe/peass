package de.dagere.peass.dependency.traces.coverage;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;

public class CoverageBasedSelector {

   private static final Logger LOG = LogManager.getLogger(CoverageBasedSelector.class);

   public static CoverageSelectionCommit selectBasedOnCoverage(final List<TraceCallSummary> summaries, final Set<ChangedEntity> changes) {
      List<TraceCallSummary> copiedSummaries = new LinkedList<>(summaries);
      Set<ChangedEntity> copiedChanges = new HashSet<>(changes);
      boolean changed = true;

      CoverageSelectionCommit resultingInfo = new CoverageSelectionCommit();

      LOG.debug("Searching CBS");
      while (copiedSummaries.size() > 0 && copiedChanges.size() > 0 && changed) {
         changed = false;

         TraceCallSummary selected = selectMaximumCalled(copiedChanges, copiedSummaries);

         LOG.debug("Selected: {}", selected);
         if (selected != null) {
            selected.setSelected(true);
            resultingInfo.getTestcases().put(selected.getTestcase(), selected);

            copiedSummaries.remove(selected);
            for (Iterator<TraceCallSummary> iterator = copiedSummaries.iterator(); iterator.hasNext();) {
               TraceCallSummary current = iterator.next();
               if (current.getTestcase().equals(selected.getTestcase())) {
                  iterator.remove();
               }
            }

            LOG.debug("Selected: {} with score {}", selected.getTestcase(), selected.getOverallScore());
            changed = removeUnneededChanges(copiedChanges, changed, selected);
         }
      }
      LOG.debug("Remaining changes: {}", copiedChanges);

      setRemainingCallSums(copiedChanges, copiedSummaries);
      addNotSelectedSummaryInfos(copiedSummaries, resultingInfo);

      return resultingInfo;
   }

   private static void setRemainingCallSums(final Set<ChangedEntity> changes, final List<TraceCallSummary> copiedSummaries) {
      for (TraceCallSummary summary : copiedSummaries) {
         int callSum = getCallSum(changes, summary);
         summary.setOverallScore(callSum);
      }
   }

   private static void addNotSelectedSummaryInfos(final List<TraceCallSummary> copiedSummaries, final CoverageSelectionCommit resultingInfo) {
      for (TraceCallSummary leftSummary : copiedSummaries) {
         LOG.debug("Adding unselected test: {} score: {}", leftSummary.getTestcase(), leftSummary.getOverallScore());
         leftSummary.setSelected(false);
         resultingInfo.getTestcases().put(leftSummary.getTestcase(), leftSummary);
      }
   }

   private static boolean removeUnneededChanges(final Set<ChangedEntity> changes, boolean changed, final TraceCallSummary selected) {
      for (Iterator<ChangedEntity> changeIterator = changes.iterator(); changeIterator.hasNext();) {
         ChangedEntity change = changeIterator.next();
         String currentChangeSignature = change.toString();
         if (change.getMethod() != null) {
            if (selected.getCallCounts().containsKey(currentChangeSignature) && selected.getCallCounts().get(currentChangeSignature) > 0) {
               changeIterator.remove();
               changed = true;
            }
         } else {
            boolean used = false;
            for (Map.Entry<String, Integer> callCount : selected.getCallCounts().entrySet()) {
               // The prefix needs to be used since otherwise inner classes are falsely selected (e.g. ChangedEntity de.Example would select de.Example$InnerClass#methodA)
               String signaturePrefix = change.toString() + ChangedEntity.METHOD_SEPARATOR;
               LOG.trace("Testing: {} vs {}" , signaturePrefix , callCount.getKey());
               if (callCount.getKey().startsWith(signaturePrefix)) {
                  used = true;
               }
            }
            if (used) {
               changeIterator.remove();
               changed = true;
            }
         }

      }
      return changed;
   }

   private static TraceCallSummary selectMaximumCalled(final Set<ChangedEntity> changes, final List<TraceCallSummary> copiedSummaries) {
      TraceCallSummary selected = copiedSummaries.get(0);
      int selectedCallSum = getCallSum(changes, selected);
      selected.setOverallScore(selectedCallSum);
      LOG.debug("Searching in {} summaries", copiedSummaries.size());
      for (TraceCallSummary current : copiedSummaries) {
         int currentCallSum = getCallSum(changes, current);
         if (currentCallSum > selectedCallSum) {
            selectedCallSum = currentCallSum;
            selected = current;
            selected.setOverallScore(currentCallSum);
         }
      }
      if (selectedCallSum > 0) {
         return selected;
      } else {
         return null;
      }
   }

   private static int getCallSum(final Set<ChangedEntity> changes, final TraceCallSummary summary) {
      summary.getSelectedChanges().clear();
      int currentCallSum = 0;
      LOG.debug("Changes: {} Test: {}", changes.size(), summary.getTestcase());
      LOG.trace("Trace Callcounts: {}", summary.getCallCounts().keySet());
      for (ChangedEntity change : changes) {
         String changeSignature = change.toString();
         LOG.trace("Change signature: {}", changeSignature);
         if (change.getMethod() != null) {
            currentCallSum = addExactCallCount(summary, currentCallSum, changeSignature);
         } else {
            currentCallSum = addClassbasedCallCount(summary, currentCallSum, changeSignature);
         }
      }
      LOG.debug("Sum: " + currentCallSum);
      return currentCallSum;
   }

   private static int addClassbasedCallCount(final TraceCallSummary summary, int currentCallSum, final String changeSignature) {
      LOG.trace("Call counts: {}", summary.getCallCounts().size());
      for (Map.Entry<String, Integer> callCount : summary.getCallCounts().entrySet()) {
         // The prefix needs to be used since otherwise inner classes are falsely selected (e.g. ChangedEntity de.Example would select de.Example$InnerClass#methodA)
         String signaturePrefix = changeSignature + ChangedEntity.METHOD_SEPARATOR;
         LOG.trace("Testing: {} vs {}", signaturePrefix, callCount.getKey());
         if (callCount.getKey().startsWith(signaturePrefix)) {
            currentCallSum += callCount.getValue();
            summary.getSelectedChanges().add(callCount.getKey());
         }
      }
      return currentCallSum;
   }

   private static int addExactCallCount(final TraceCallSummary summary, int currentCallSum, final String changeSignature) {
      if (summary.getCallCounts().containsKey(changeSignature)) {
         currentCallSum += summary.getCallCounts().get(changeSignature);
         summary.getSelectedChanges().add(changeSignature);
      }
      return currentCallSum;
   }
}
