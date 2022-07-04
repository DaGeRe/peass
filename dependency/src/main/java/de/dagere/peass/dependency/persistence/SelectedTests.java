package de.dagere.peass.dependency.persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class SelectedTests {
   private String url;
   private boolean isAndroid = false;

   public String getUrl() {
      return url;
   }

   public void setUrl(final String url) {
      this.url = url;
   }

   public void setAndroid(final boolean isAndroid) {
      this.isAndroid = isAndroid;
   }

   public boolean isAndroid() {
      return isAndroid;
   }

   @JsonIgnore
   public String getName() {
      final String name;
      final int dotSeperator = url.lastIndexOf('.');
      final int lastSlashIndex = url.lastIndexOf('/') + 1;
      if (dotSeperator > lastSlashIndex) {
         name = url.substring(lastSlashIndex, dotSeperator);
      } else {
         name = url.substring(lastSlashIndex);
      }
      return name;
   }
   
   public abstract String[] getCommitNames();
}
