package de.dagere.peass.dependencytests;

import java.io.File;

import de.dagere.peass.config.DependencyConfig;
import de.dagere.peass.dependency.ResultsFolders;

public class DependencyTestConstants {

   public static final String VERSION_1 = "000001";
   public static final String VERSION_2 = "000002";
   public static final File VERSIONS_FOLDER = new File("src/test/resources/dependencyIT");
   public static final File BASIC_STATE = new File(VERSIONS_FOLDER, "basic_state");
   
   public static final DependencyConfig DEFAULT_CONFIG = new DependencyConfig(1, false);
   
   public static final File CURRENT = new File(new File("target"), "current");
   
   public static final ResultsFolders NULL_RESULTS_FOLDERS = new ResultsFolders(new File("/dev/null"), "test");

}
