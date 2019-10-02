package de.peass.measurement.searchcause.helper;

import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.measurement.searchcause.CauseSearcherConfig;

public class TestConstants {
   public final static CauseSearcherConfig SIMPLE_CAUSE_CONFIG = new CauseSearcherConfig(new TestCase("Test#test"), true, false, 5.0, false, 0.1, false);
   public final static CauseSearcherConfig SIMPLE_CAUSE_CONFIG_TESTME = new CauseSearcherConfig(new TestCase("defaultpackage.TestMe", "testMe"), true, false, 5.0, false, 0.1,
         false);
   public final static MeasurementConfiguration SIMPLE_MEASUREMENT_CONFIG = new MeasurementConfiguration(2, "000001", "000001~1");
   public final static MeasurementConfiguration SIMPLE_MEASUREMENT_CONFIG_KIEKER = new MeasurementConfiguration(2, "000001", "000001~1");
   static {
      SIMPLE_MEASUREMENT_CONFIG_KIEKER.setUseKieker(true);
   }
}
