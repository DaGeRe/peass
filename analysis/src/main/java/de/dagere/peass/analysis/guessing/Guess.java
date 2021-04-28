package de.dagere.peass.analysis.guessing;

import java.util.Map;
import java.util.TreeMap;

public class Guess {
   private Map<String, ExpectedDirection> directions = new TreeMap<>();

   public Map<String, ExpectedDirection> getDirections() {
      return directions;
   }

   public void setDirections(Map<String, ExpectedDirection> directions) {
      this.directions = directions;
   }

   public void add(Guesser guesser) {
      if (guesser.getDirection() == null) {
         throw new RuntimeException("Not possible");
      }
      directions.put(guesser.getName(), guesser.getDirection());
   }

   public void add(String type, ExpectedDirection direction) {
      if (direction == null) {
         throw new RuntimeException("Not possible");
      }
      directions.put(type, direction);
   }
}