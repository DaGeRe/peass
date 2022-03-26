package de.dagere.peass.execution.utils;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

public class EnvironmentVariables implements Serializable {

   private static final long serialVersionUID = 6287969989033845334L;

   private final Map<String, String> environmentVariables = new TreeMap<>();

   private final String properties;

   public EnvironmentVariables(final String properties) {
      if (properties != null) {
         this.properties = properties;
      } else {
         this.properties = "";
      }
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
      } else if (!isWindows()) {
         mvnCall = "mvn";
      } else {
         mvnCall = "mvn.cmd";
      }
      return mvnCall;
   }

   public static String fetchMavenCallGeneric() {
      String mvnCall;
      if (!isWindows()) {
         mvnCall = "mvn";
      } else {
         mvnCall = "mvn.cmd";
      }
      return mvnCall;
   }

   public String fetchGradleCall() {
      String gradleCall;
      if (!isWindows()) {
         gradleCall = "./gradlew";
      } else {
         gradleCall = "gradlew.bat";
      }
      return gradleCall;
   }

   public static boolean isWindows() {
      return System.getProperty("os.name").startsWith("Windows");
   }

   public static boolean isLinux() {
      return !System.getProperty("os.name").startsWith("Windows") && !System.getProperty("os.name").startsWith("Mac");
   }
}
