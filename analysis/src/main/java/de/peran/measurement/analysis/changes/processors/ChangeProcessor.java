package de.peran.measurement.analysis.changes.processors;

import de.peass.analysis.changes.Change;

public interface ChangeProcessor {
   public void process(String version, String testcase, Change change);
}
