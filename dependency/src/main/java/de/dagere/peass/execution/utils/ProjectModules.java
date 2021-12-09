package de.dagere.peass.execution.utils;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ProjectModules {
   private final List<File> modules;
   private final Map<File, String> artifactIds = new HashMap<>();

   public ProjectModules(final List<File> modules) {
      this.modules = modules;
   }
   
   public ProjectModules(final File current) {
      modules = new LinkedList<File>();
      modules.add(current);
   }

   public List<File> getModules() {
      return modules;
   }
   
   public Map<File, String> getArtifactIds() {
      return artifactIds;
   }

   public List<File> getParents(final File moduleFile) {
      final List<File> parents = new LinkedList<File>();
      for (File potentialParent : modules) {
         String potentialParentPath = potentialParent.getAbsolutePath();
         String moduleAbsolutePath = moduleFile.getAbsolutePath();
         if (moduleAbsolutePath.contains(potentialParentPath) && !potentialParent.equals(moduleFile)) {
            parents.add(potentialParent);
         }
      }
      return parents;
   }
}
