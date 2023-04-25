package de.dagere.peass.measurement.statistics;

public enum Relation {
   EQUAL, UNKOWN, LESS_THAN, GREATER_THAN;
   
   public static boolean isUnequal(Relation relation) {
      return relation == Relation.LESS_THAN || relation == Relation.GREATER_THAN;
   }
}