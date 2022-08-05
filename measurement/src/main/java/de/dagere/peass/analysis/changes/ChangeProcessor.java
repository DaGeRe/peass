package de.dagere.peass.analysis.changes;

public interface ChangeProcessor {
   public void process(String commit, String testclazz, Change change);
}
