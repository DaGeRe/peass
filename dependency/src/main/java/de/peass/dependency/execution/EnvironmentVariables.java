package de.peass.dependency.execution;

import java.util.Map;
import java.util.TreeMap;

public class EnvironmentVariables {
   private final Map<String, String> environmentVariables = new TreeMap<>();
   
   public Map<String, String> getEnvironmentVariables() {
      return environmentVariables;
   }
}
