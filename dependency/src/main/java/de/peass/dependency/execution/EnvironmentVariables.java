package de.peass.dependency.execution;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

public class EnvironmentVariables implements Serializable{
   private final Map<String, String> environmentVariables = new TreeMap<>();
   
   public Map<String, String> getEnvironmentVariables() {
      return environmentVariables;
   }
}
