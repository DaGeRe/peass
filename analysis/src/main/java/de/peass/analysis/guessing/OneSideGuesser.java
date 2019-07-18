package de.peass.analysis.guessing;

import java.util.List;

import de.peass.analysis.guessing.GuessDecider.ConditionChecker;
import difflib.Delta;

class OneSideGuesser extends Guesser {

   OneSideGuesser(String name, ConditionChecker checker, ExpectedDirection direction) {
      super(name, checker, null, direction);
   }

   @Override
   boolean isGuessTrue(Delta<String> delta) {
      List<String> lines = delta.getRevised().getLines();
      List<String> lines2 = delta.getOriginal().getLines();
      if (lines2.size() < lines.size()) {
         return testOneSideCondition(checker, lines, lines2);
      } else if (lines2.size() > lines.size()) {
         return testOneSideCondition(checker, lines2, lines);
      }
      return false;
   }

   private boolean testOneSideCondition(ConditionChecker checker2, List<String> lines, List<String> lines2) {
      boolean conditionNotInBoth = false;
      for (int index = 0; index < lines.size(); index++) {
         String line = lines.get(index);
         if (checker2.check(line)) {
            if (lines2.size() == 0) {
               conditionNotInBoth = true;
            } else if (index < lines2.size()) {
               String lineOther = lines2.get(index);
               if (!checker2.check(lineOther)) {
                  boolean containsExactEqualLine = false;
                  for (String testLine : lines2) {
                     if (testLine.trim().equals(line.trim())) {
                        containsExactEqualLine = true;
                     }
                  }
                  if (!containsExactEqualLine) {
                     conditionNotInBoth = true;
                  }
               }
            }
         }
      }
      return conditionNotInBoth;
   }
}