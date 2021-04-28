package de.dagere.peass.measurement.rca.searcher;

import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.analyzer.TreeAnalyzer;
import de.dagere.peass.measurement.rca.kieker.BothTreeReader;

public interface TreeAnalyzerCreator {
   public TreeAnalyzer getAnalyzer(BothTreeReader reader, CauseSearcherConfig config);
}
