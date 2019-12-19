package de.peass.analysis.properties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChangeProperties {
   private Map<String, List<ChangeProperty>> properties = new LinkedHashMap<>();
   
   private String commitText;
   private String committer;

   public String getCommitter() {
      return committer;
   }

   public void setCommitter(final String comitter) {
      this.committer = comitter;
   }

   public String getCommitText() {
      return commitText;
   }

   public void setCommitText(final String commitText) {
      this.commitText = commitText;
   }
   
   public Map<String, List<ChangeProperty>> getProperties() {
      return properties;
   }

   public void setProperties(final Map<String, List<ChangeProperty>> properties) {
      this.properties = properties;
   }
}