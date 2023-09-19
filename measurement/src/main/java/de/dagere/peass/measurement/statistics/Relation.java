package de.dagere.peass.measurement.statistics;

/**
 * The relation *viewed from the current version*, i.e., if the old version has significantly lower values than the current one, 
 * it would be LESS_THAN, if the older version has significantly higher values than the current one, it would be GREATER_THAN.
 */
public enum Relation {
   EQUAL, UNKOWN, LESS_THAN, GREATER_THAN;
   
   public static boolean isUnequal(Relation relation) {
      return relation == Relation.LESS_THAN || relation == Relation.GREATER_THAN;
   }
}