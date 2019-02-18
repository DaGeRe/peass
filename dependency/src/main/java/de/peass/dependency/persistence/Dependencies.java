package de.peass.dependency.persistence;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Dependencies {
   private String url;
   private boolean isAndroid = false;
   private InitialVersion initialversion = new InitialVersion();
   private Map<String, Version> versions = new LinkedHashMap<>();

   public String getUrl() {
      return url;
   }

   public void setUrl(final String url) {
      this.url = url;
   }

   public InitialVersion getInitialversion() {
      return initialversion;
   }

   public void setInitialversion(final InitialVersion initialversion) {
      this.initialversion = initialversion;
   }

   public Map<String, Version> getVersions() {
      return versions;
   }

   public void setVersions(final Map<String, Version> versions) {
      this.versions = versions;
   }

   @JsonIgnore
   public String[] getVersionNames() {
      final String[] versionNames = versions.keySet().toArray(new String[0]);
      return versionNames;
   }

   @JsonIgnore
   public String getNewestVersion() {
      final String[] versions = getVersionNames();
      if (versions.length > 0) {
         return versions[versions.length - 1];
      } else if (initialversion != null) {
         return initialversion.getVersion();
      } else {
         return null;
      }

   }

   public boolean isAndroid() {
      return isAndroid;
   }

   public void setAndroid(final boolean isAndroid) {
      this.isAndroid = isAndroid;
   }

}
