package de.peass.measurement.rca.searcher;

import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.analyzer.TreeAnalyzer;
import de.peass.measurement.rca.kieker.BothTreeReader;

public interface TreeAnalyzerCreator {
   public TreeAnalyzer getAnalyzer(BothTreeReader reader, CauseSearcherConfig config);
}
