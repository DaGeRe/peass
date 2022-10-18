package de.dagere.peass.dependency.traces.coverage;

import java.util.LinkedHashMap;
import java.util.Map;

public class CoverageSelectionInfo {
   private Map<String, CoverageSelectionCommit> commits = new LinkedHashMap<>();

   public Map<String, CoverageSelectionCommit> getCommits() {
      return commits;
   }

   public void setCommits(final Map<String, CoverageSelectionCommit> commits) {
      this.commits = commits;
   }
}
