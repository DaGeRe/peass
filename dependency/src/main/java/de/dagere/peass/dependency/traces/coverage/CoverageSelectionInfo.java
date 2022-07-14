package de.dagere.peass.dependency.traces.coverage;

import java.util.LinkedHashMap;
import java.util.Map;

public class CoverageSelectionInfo {
   private Map<String, CoverageSelectionCommit> versions = new LinkedHashMap<>();

   public Map<String, CoverageSelectionCommit> getVersions() {
      return versions;
   }

   public void setVersions(final Map<String, CoverageSelectionCommit> versions) {
      this.versions = versions;
   }
}
