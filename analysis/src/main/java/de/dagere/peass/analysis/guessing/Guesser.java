package de.dagere.peass.analysis.guessing;

import java.util.LinkedList;
import java.util.List;

import com.github.difflib.patch.AbstractDelta;

import de.dagere.peass.analysis.guessing.GuessDecider.ConditionChecker;

public class Guesser {

   public static final List<Guesser> allGuessers = new LinkedList<>();

   static {
      allGuessers.add(new Guesser("INT_LONG",
            (line) -> {
               return line.contains("int");
            },
            (line) -> {
               return line.contains("long");
            }, ExpectedDirection.SLOWER));
      allGuessers.add(new Guesser("STREAM",
            (line) -> {
               return line.contains("FileInputStream") || line.contains("FileOutputStream");
            },
            (line) -> {
               return line.contains("BufferedInputStream") || line.contains("BufferedOutputStream");
            }, ExpectedDirection.FASTER));
      allGuessers.add(new Guesser("EXTENDED_FOR",
            (line) -> {
               return line.matches("\\s*for[ ]*\\([^;]*;[^;]*;[^;]*\\).*");
            },
            (line) -> {
               return line.matches("\\s*for[ ]*\\([^:]*:[^:]*\\).*");
            }, ExpectedDirection.BOTH));
      allGuessers.add(new Guesser("EFFICIENT_STRING",
            (line) -> {
               return line.contains("\"") && line.contains("+");
            },
            (line) -> {
               return line.contains(".append(");
            }, ExpectedDirection.FASTER));
      allGuessers.add(new OneSideGuesser("REUSE_BUFFER",
            (line) -> {
               return line.matches("\\s*\\w*\\s*\\w+[\\[\\]]*\\s+buffer = new byte\\[[^]]*\\].*");
            }, ExpectedDirection.FASTER));
      allGuessers.add(new OneSideGuesser("CONDITION_EXECUTION", (line) -> {
         return line.matches("\\s*if.*");
      }, ExpectedDirection.BOTH));
   }

   private String name;
   protected ConditionChecker checker;
   protected ConditionChecker checkerOld;
   protected ExpectedDirection direction;

   Guesser(String name, ConditionChecker checker, ConditionChecker checkerOld, ExpectedDirection direction) {
      this.name = name;
      this.checker = checker;
      this.checkerOld = checkerOld;
      this.direction = direction;
   }

   boolean isGuessTrue(AbstractDelta<String> delta) {
      boolean fullfills = false;
      if (check(delta.getSource().getLines(), checker) && check(delta.getTarget().getLines(), checkerOld)) {
         fullfills = true;
      }
      if (check(delta.getSource().getLines(), checkerOld) && check(delta.getTarget().getLines(), checker)) {
         fullfills = true;
      }
      return fullfills;
   }

   boolean check(List<String> lines, ConditionChecker checker) {
      boolean oldFullfils = false;
      for (String line : lines) {
         if (checker.check(line))
            oldFullfils = true;
      }
      return oldFullfils;
   }

   public String getName() {
      return name;
   }

   public ExpectedDirection getDirection() {
      return direction;
   }

}
