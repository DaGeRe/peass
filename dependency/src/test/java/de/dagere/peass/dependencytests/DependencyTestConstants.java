package de.dagere.peass.dependencytests;

import java.io.File;

import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.folders.ResultsFolders;

public class DependencyTestConstants {

   public static final String VERSION_1 = "000001";
   public static final String VERSION_2 = "000002";
   public static final File VERSIONS_FOLDER = new File("../dependency/src/test/resources/dependencyIT");
   public static final File BASIC_STATE = new File(VERSIONS_FOLDER, "basic_state");
   public static final File NORMAL_CHANGE  = new File(VERSIONS_FOLDER, "normal_change");
   
   public static final File BASIC_STATE_PARAMETERS = new File(VERSIONS_FOLDER, "state_with_parameters");
   public static final File NORMAL_CHANGE_PARAMETERS  = new File(VERSIONS_FOLDER, "state_with_parameters_changed");
   
   public static final TestSelectionConfig DEFAULT_CONFIG_NO_VIEWS = new TestSelectionConfig(1, false, false, false);
   public static final TestSelectionConfig DEFAULT_CONFIG_WITH_VIEWS = new TestSelectionConfig(1, false, true, false);
   public static final TestSelectionConfig DEFAULT_CONFIG_WITH_COVERAGE = new TestSelectionConfig(1, false, true, true);
   
   public static final File CURRENT = new File(new File("target"), "current");
   
   public static final ResultsFolders NULL_RESULTS_FOLDERS = new ResultsFolders(new File("target/dummy"), "test");
   public static final ResultsFolders TARGET_RESULTS_FOLDERS = new ResultsFolders(new File("target/view_results"), "test");
   
   public static final File COVERAGE_VERSIONS_FOLDER = new File("../dependency/src/test/resources/coverageBasedSelection");
   public static final File COVERAGE_BASIC_STATE = new File(COVERAGE_VERSIONS_FOLDER, "basic_state");
   public static final File COVERAGE_NORMAL_CHANGE  = new File(COVERAGE_VERSIONS_FOLDER, "normal_change");

}
