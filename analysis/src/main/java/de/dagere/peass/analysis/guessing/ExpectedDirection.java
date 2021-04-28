package de.dagere.peass.analysis.guessing;

public enum ExpectedDirection {
   FASTER, SLOWER, BOTH;

   public static ExpectedDirection getDirection(String direction) {
      if (direction.toLowerCase().equals("faster")) {
         return FASTER;
      } else if (direction.toLowerCase().equals("slower")) {
         return SLOWER;
      } else {
         throw new RuntimeException("Unexpected Direction: " + direction);
      }
   }
}