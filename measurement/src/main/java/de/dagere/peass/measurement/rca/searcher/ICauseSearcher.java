package de.dagere.peass.measurement.rca.searcher;

import java.util.Set;

import de.dagere.nodeDiffDetector.data.MethodCall;

public interface ICauseSearcher {
   public Set<MethodCall> search();
}
