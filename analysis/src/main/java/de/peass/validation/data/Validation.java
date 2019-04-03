package de.peass.validation.data;

import java.util.LinkedHashMap;
import java.util.Map;

public class Validation{
   private Map<String, ProjectValidation> projects = new LinkedHashMap<>();

   public Map<String, ProjectValidation> getProjects() {
      return projects;
   }

   public void setProjects(final Map<String, ProjectValidation> projects) {
      this.projects = projects;
   }
}