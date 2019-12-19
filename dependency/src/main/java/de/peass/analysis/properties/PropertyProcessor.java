package de.peass.analysis.properties;

public interface PropertyProcessor {
   public void process(String version, String testcase, ChangeProperty change, ChangeProperties changeProperties);
}
