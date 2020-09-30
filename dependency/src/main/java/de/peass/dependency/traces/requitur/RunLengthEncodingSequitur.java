package de.peass.dependency.traces.requitur;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.traces.requitur.content.Content;
import de.peass.dependency.traces.requitur.content.RuleContent;

public class RunLengthEncodingSequitur {

   private static final Logger LOG = LogManager.getLogger(RunLengthEncodingSequitur.class);

   private final Sequitur sequitur;

   public RunLengthEncodingSequitur(final Sequitur sequitur) {
      this.sequitur = sequitur;
   }

   public void reduce() {
      reduce(sequitur.getStartSymbol());

      removeSingleUsageRules(sequitur.getStartSymbol().getSuccessor());

      reduce(sequitur.getStartSymbol());

//      sequiturAgain();
   }

   private void sequiturAgain() {
      Map<Digram, Rule> currentRules = new HashMap<>();
      for (Rule rule : sequitur.getRules().values()) {
         Digram ruleDigram = new Digram(rule.getAnchor().getSuccessor(), rule.getAnchor().getSuccessor().getSuccessor());
         currentRules.put(ruleDigram, rule);
      }

      // Sequitur seq = new Sequitur();
      Symbol iterator = sequitur.getStartSymbol().getSuccessor();
      while (iterator != null && iterator.getValue() != null && iterator.getSuccessor() != null) {
         Symbol successor = iterator.getSuccessor();
         Digram digram = new Digram(iterator, successor);
         final Rule potentialRule = currentRules.get(digram);
         if (potentialRule != null) {
            potentialRule.use(digram);
         }
         
         iterator = successor;
         // sequitur.getRules()
      }
   }

   private void removeSingleUsageRules(Symbol iterator) {
      while (iterator != null && iterator.getValue() != null && iterator.getSuccessor() != null) {
         if (iterator.getValue() instanceof RuleContent) {
            RuleContent ruleName = (RuleContent) iterator.getValue();
            Rule rule = sequitur.getRules().get(ruleName.getValue());

            removeSingleUsageRules(rule.getAnchor().getSuccessor());

            if (iterator.getOccurences() == 1) {
               removeSingleOccurenceRule(iterator, rule);
            }

            iterator = iterator.getSuccessor();
         } else {
            iterator = iterator.getSuccessor();
         }
         // TraceStateTester.assureCorrectState(sequitur);
      }
   }

   private void removeSingleOccurenceRule(Symbol iterator, Rule rule) {
      Symbol currentPredecessor = iterator.getPredecessor();
      Symbol ruleIterator = rule.getAnchor().getSuccessor();
      while (ruleIterator.getSuccessor() != rule.getAnchor()) {
         Symbol copied = copySymbol(currentPredecessor, ruleIterator);

         currentPredecessor = copied;
         ruleIterator = ruleIterator.getSuccessor();
      }
      Symbol copied = copySymbol(currentPredecessor, ruleIterator);

      copied.setSuccessor(iterator.getSuccessor());
      iterator.getSuccessor().setPredecessor(copied);
   }

   private Symbol copySymbol(Symbol currentPredecessor, Symbol ruleIterator) {
      Symbol copied = new Symbol(sequitur, ruleIterator.getValue(), ruleIterator.getRule());
      copied.setOccurences(ruleIterator.getOccurences());
      currentPredecessor.setSuccessor(copied);
      copied.setPredecessor(currentPredecessor);
      return copied;
   }

   private void reduce(final Symbol start) {
      Symbol iterator = start.getSuccessor();
      reduceRule(iterator);
      while (iterator != null && iterator.getValue() != null && iterator.getSuccessor() != null && iterator.getSuccessor().getValue() != null) {
         final Symbol successor = iterator.getSuccessor();
         reduceRule(successor);
         if (iterator.valueEqual(successor)) {
            mergeOccurences(iterator, successor);
         } else {
            iterator = iterator.getSuccessor();
         }
      }
   }

   private void mergeOccurences(Symbol iterator, final Symbol successor) {
      if (successor.getSuccessor() != null) {
         iterator.setSuccessor(successor.getSuccessor());
         successor.getSuccessor().setPredecessor(iterator);
      } else {
         iterator.setSuccessor(null);
      }
      iterator.setOccurences(iterator.getOccurences() + successor.getOccurences());
   }

   private void reduceRule(final Symbol containingSymbol) {
      if (containingSymbol.isRule()) {
         LOG.trace("Reduce: {}", containingSymbol);
         final Rule rule = containingSymbol.getRule();
         final Symbol ruleAnchor = rule.getAnchor();
         reduce(ruleAnchor);
         final Symbol firstSymbolOfRule = ruleAnchor.getSuccessor();
         LOG.trace("Reduced: {}", rule.getName());
         LOG.trace("Rule-Length: {}", rule.getElements().size() + " " + (firstSymbolOfRule.getSuccessor() == ruleAnchor));
         if (firstSymbolOfRule.getSuccessor() == ruleAnchor) { 
            removeRuleUsage(containingSymbol, rule, firstSymbolOfRule);
         }
         // TraceStateTester.testTrace(sequitur);
      }
   }

   private void removeRuleUsage(final Symbol containingSymbol, final Rule rule, final Symbol firstSymbolOfRule) {
      containingSymbol.setValue(firstSymbolOfRule.getValue());
      containingSymbol.setOccurences(containingSymbol.getOccurences() * firstSymbolOfRule.getOccurences());
      containingSymbol.decrementUsage(rule);
      if (firstSymbolOfRule.getRule() != null) {
         containingSymbol.setRule(firstSymbolOfRule.getRule());
      } else {
         firstSymbolOfRule.setRule(null);
      }
   }

   public List<ReducedTraceElement> getReadableRLETrace() {
      Symbol iterator = sequitur.getStartSymbol().getSuccessor();
      final List<ReducedTraceElement> trace = new LinkedList<>();
      while (iterator != null) {
         addReadableElement(iterator, trace);
         iterator = iterator.getSuccessor();
      }
      return trace;
   }

   public List<ReducedTraceElement> getTopLevelTrace() {
      Symbol iterator = sequitur.getStartSymbol().getSuccessor();
      final List<ReducedTraceElement> trace = new LinkedList<>();
      while (iterator != null) {
         final ReducedTraceElement newElement = new ReducedTraceElement(iterator.getValue(), iterator.getOccurences());
         trace.add(newElement);
         iterator = iterator.getSuccessor();
      }
      return trace;
   }

   /**
    * Adds the symbols in the current iterator to the given list
    * 
    * @param iterator
    * @param trace
    * @return The count of elements that where added
    */
   private int addReadableElement(final Symbol iterator, final List<ReducedTraceElement> trace) {
      final Content content = iterator.getValue();
      LOG.trace("Add: {} {}", content, content.getClass());
      final ReducedTraceElement newElement = new ReducedTraceElement(content, iterator.getOccurences());
      if (content instanceof RuleContent) {
         return addRuleContent(iterator, trace, content, newElement);
      } else {
         trace.add(newElement);
         return 1;
      }
   }

   private int addRuleContent(final Symbol iterator, final List<ReducedTraceElement> trace, final Content content, final ReducedTraceElement newElement) {
      final RuleContent currentContent = (RuleContent) content;
      // if (newElement.getOccurences() > 1)
      trace.add(newElement);
      final Rule rule = iterator.getRule();
      final Symbol anchor = rule.getAnchor();
      Symbol ruleIterator = anchor.getSuccessor();
      int subElements = 1;
      while (ruleIterator != anchor) {
         subElements += addReadableElement(ruleIterator, trace);
         ruleIterator = ruleIterator.getSuccessor();
      }
      currentContent.setCount(subElements - 1);
      return subElements;
   }

}
