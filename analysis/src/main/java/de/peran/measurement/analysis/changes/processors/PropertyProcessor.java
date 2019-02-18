package de.peran.measurement.analysis.changes.processors;

import de.peass.analysis.properties.ChangeProperties;
import de.peass.analysis.properties.ChangeProperty;

public interface PropertyProcessor {
   public void process(String version, String testcase, ChangeProperty change, ChangeProperties changeProperties);
}
