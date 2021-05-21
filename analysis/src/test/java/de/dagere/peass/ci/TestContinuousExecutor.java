package de.dagere.peass.ci;

import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import de.dagere.peass.TestUtil;
import de.dagere.peass.config.DependencyConfig;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependencytests.DependencyTestConstants;
import de.dagere.peass.vcs.ProjectBuilderHelper;

public class TestContinuousExecutor {
   
   @Test
   public void testChangeIdentification() throws InterruptedException, IOException {
      if (!DependencyTestConstants.CURRENT.exists()) {
         DependencyTestConstants.CURRENT.mkdirs();
      }
      TestUtil.deleteContents(DependencyTestConstants.CURRENT);
      ProjectBuilderHelper.init(DependencyTestConstants.CURRENT);
      FileUtils.copyDirectory(DependencyTestConstants.BASIC_STATE, DependencyTestConstants.CURRENT);
      ProjectBuilderHelper.commit(DependencyTestConstants.CURRENT, "Version 0");
      FileUtils.copyDirectory(DependencyTestConstants.BASIC_STATE, DependencyTestConstants.CURRENT);
      ProjectBuilderHelper.commit(DependencyTestConstants.CURRENT, "Version 0");
      
      DependencyConfig dependencyConfig = new DependencyConfig(1, false);
      MeasurementConfiguration measurementConfig = new MeasurementConfiguration(2);
      ContinuousExecutor executor = new ContinuousExecutor(DependencyTestConstants.CURRENT, measurementConfig, dependencyConfig, new EnvironmentVariables());
   }
}
