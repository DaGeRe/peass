package de.peass.analysis.changes.processors;

import de.dagere.peass.analysis.changes.Change;

public interface ChangeProcessor {
   public void process(String version, String testclazz, Change change);
}
