package de.peass.dependency.persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SelectedTests {
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
      String name = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'));
      return name;
   }
}
