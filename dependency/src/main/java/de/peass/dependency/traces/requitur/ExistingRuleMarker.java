package de.peass.dependency.traces.requitur;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExistingRuleMarker {
   
   private static final Logger LOG = LogManager.getLogger(ExistingRuleMarker.class);
   
   private final Sequitur sequitur;
   private final Map<Digram, Rule> currentRules = new HashMap<>();
   private final Map<Digram, Digram> replacedDigrams = new HashMap<>();
   
   public ExistingRuleMarker(Sequitur sequitur) {
      this.sequitur = sequitur;
   }


   public void mark() {
      detectDigrams();
      
      sequitur.digrams = replacedDigrams; 

      Symbol iterator = sequitur.getStartSymbol().getSuccessor();
      while (iterator != null && iterator.getValue() != null && iterator.getSuccessor() != null) {
         Symbol successor = iterator.getSuccessor();
         Digram digram = new Digram(iterator, successor);
         final Rule potentialRule = currentRules.get(digram);
         if (potentialRule != null) {
            markRule(digram, potentialRule);
            // It is not clean to build markRule, it would be better to adjust the digram handling and use potentialRule.use
//            potentialRule.use(digram);
         }
         
         iterator = successor;
      }
   }


   private void markRule(Digram digram, final Rule potentialRule) {
      LOG.trace("Reusing: " + potentialRule.getName() + " " + potentialRule.getElements());
      
      final Symbol ruleSymbol = new Symbol(sequitur, potentialRule);
      
      digram.getStart().getPredecessor().setSuccessor(ruleSymbol);
      ruleSymbol.setPredecessor(digram.getStart().getPredecessor());

      if (digram.getEnd().getSuccessor() != null) {
         digram.getEnd().getSuccessor().setPredecessor(ruleSymbol);
         ruleSymbol.setSuccessor(digram.getEnd().getSuccessor());
      } else {
         this.sequitur.lastSymbol = ruleSymbol;
      }
   }


   private void detectDigrams() {
      for (Rule rule : sequitur.getRules().values()) {
         if (rule.getElements().size() == 2) {
            Digram ruleDigram = new Digram(rule.getAnchor().getSuccessor(), rule.getAnchor().getSuccessor().getSuccessor());
            ruleDigram.setRule(rule);
            currentRules.put(ruleDigram, rule);
            replacedDigrams.put(ruleDigram, ruleDigram);
            
            LOG.trace("Digram: " + ruleDigram);
         }
      }
   }
}
