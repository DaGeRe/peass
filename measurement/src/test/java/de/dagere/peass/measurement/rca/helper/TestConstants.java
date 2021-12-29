package de.dagere.peass.measurement.rca.helper;

import java.io.File;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.RCAStrategy;

public class TestConstants {
   
   public static final String V1 = "000001~1";
   public static final String V2 = "000001";
   
   public final static CauseSearcherConfig SIMPLE_CAUSE_CONFIG = new CauseSearcherConfig(new TestCase("Test#test"), 
         false, 0.1, 
         false, true, RCAStrategy.COMPLETE, 1);
   public final static CauseSearcherConfig SIMPLE_CAUSE_CONFIG_TESTME = new CauseSearcherConfig(new TestCase("defaultpackage.TestMe", "testMe"), 
         false, 0.1,
         false, true, RCAStrategy.COMPLETE, 1);
   public final static MeasurementConfig SIMPLE_MEASUREMENT_CONFIG = new MeasurementConfig(2, V2, V1);
   public final static MeasurementConfig SIMPLE_MEASUREMENT_CONFIG_KIEKER = new MeasurementConfig(2, V2, V1);
  
   static {
      SIMPLE_MEASUREMENT_CONFIG_KIEKER.setUseKieker(true);
   }
   
   public static final File CURRENT_FOLDER = new File(new File("target"), "current");
   public static final File CURRENT_PEASS = new File("target/current_peass/");
}
