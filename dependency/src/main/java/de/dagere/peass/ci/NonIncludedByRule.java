package de.dagere.peass.ci;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class NonIncludedByRule {
   public static boolean isTestIncluded(TestCase test, ExecutionConfig config) {
      return false;
   }
}
