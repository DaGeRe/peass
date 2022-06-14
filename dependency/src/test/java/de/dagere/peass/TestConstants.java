package de.dagere.peass;

import java.io.File;

import de.dagere.peass.testtransformation.JUnitVersions;

public class TestConstants {

   public static final File TEST_RESOURCES = new File("src/test/resources");

   public static final File CURRENT_FOLDER = new File(new File("target"), "current");
   public static final File CURRENT_PEASS = new File("target/current_peass/");
   
   public static final JUnitVersions TEST_JUNIT_VERSIONS = new JUnitVersions();
   static {
      TEST_JUNIT_VERSIONS.setJunit4(true);
   }

}
