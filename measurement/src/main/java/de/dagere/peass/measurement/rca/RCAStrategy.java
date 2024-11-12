package de.dagere.peass.measurement.rca;

import de.dagere.peass.measurement.rca.analyzer.CompleteTreeAnalyzer;
import de.dagere.peass.measurement.rca.analyzer.SourceChangeTreeAnalyzer;
import de.dagere.peass.measurement.rca.searcher.LevelCauseSearcher;

/**
 * Contains all supported node selection strategies for root cause analysis 
 *
 */
public enum RCAStrategy {
   /**
    * Measures each level individually, implemented by {@link LevelCauseSearcher}
    */
   LEVELWISE, 
   /**
    * Measures all nodes at once, node selection implemented by {@link CompleteTreeAnalyzer}
    */
   COMPLETE, UNTIL_STRUCTURE_CHANGE, 
   /**
    * Searches for root causes by analyzing all nodes on the path to a changed source code, implemented by {@link SourceChangeTreeAnalyzer}
    */
   UNTIL_SOURCE_CHANGE,
   /**
    * Read the whole tree using sampling
    */
   SAMPLING
}
