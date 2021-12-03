package de.dagere.peass.dependency.execution;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

public class EnvironmentVariables implements Serializable {

   private static final long serialVersionUID = 6287969989033845334L;

   private final Map<String, String> environmentVariables = new TreeMap<>();

   private final String properties;

   public EnvironmentVariables(final String properties) {
      this.properties = properties;
   }

   public EnvironmentVariables() {
      properties = "";
   }

   public Map<String, String> getEnvironmentVariables() {
      return environmentVariables;
   }

   public String getProperties() {
      return properties;
   }

   public String fetchMavenCall() {
      String mvnCall;
      if (environmentVariables.containsKey("MVN_CMD")) {
         mvnCall = environmentVariables.get("MVN_CMD");
      } else if (!System.getProperty("os.name").startsWith("Windows")) {
         mvnCall = "mvn";
      } else {
         mvnCall = "mvn.cmd";
      }
      return mvnCall;
   }
   
   public static String fetchMavenCallGeneric() {
      String mvnCall;
      if (!System.getProperty("os.name").startsWith("Windows")) {
         mvnCall = "mvn";
      } else {
         mvnCall = "mvn.cmd";
      }
      return mvnCall;
   }
}
