package de.dagere.peass.jmh;

import java.io.File;

public class JmhTestConstants {

   public static File JMH_EXAMPLE_FOLDER = new File("src/test/resources/jmh-it");
   public static File BASIC_VERSION = new File(JmhTestConstants.JMH_EXAMPLE_FOLDER, "basic_version");
   public static File SLOWER_VERSION = new File(JmhTestConstants.JMH_EXAMPLE_FOLDER, "slower_version");
   
   public static File MULTIMODULE_VERSION = new File(JmhTestConstants.JMH_EXAMPLE_FOLDER, "multimodule");
   
   public static File MULTIPARAM_VERSION = new File(JmhTestConstants.JMH_EXAMPLE_FOLDER, "multi-param-version");
}
