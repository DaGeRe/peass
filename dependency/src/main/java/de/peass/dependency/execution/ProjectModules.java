package de.peass.dependency.execution;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class ProjectModules {
   private final List<File> modules;

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
}
