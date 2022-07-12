package de.dagere.peass.dependency;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.TestTransformer;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;

public class RTSTestTransformerBuilder {
   public static TestTransformer createTestTransformer(PeassFolders folders, final ExecutionConfig executionConfig, final KiekerConfig kiekerConfig) {
      ExecutionConfig fakeExecutionConfig = new ExecutionConfig(executionConfig);
      KiekerConfig fakeKiekerConfig = new KiekerConfig(kiekerConfig);
      MeasurementConfig fakeConfig = new MeasurementConfig(1, fakeExecutionConfig, fakeKiekerConfig);
      fakeConfig.setIterations(1);
      fakeConfig.setWarmup(0);
      // Structure discovery runs never need adaptive monitoring
      fakeConfig.getKiekerConfig().setEnableAdaptiveMonitoring(false);
      fakeConfig.getKiekerConfig().setUseAggregation(false);
      fakeConfig.getExecutionConfig().setRedirectToNull(false);
      fakeConfig.getKiekerConfig().setRecord(AllowedKiekerRecord.OPERATIONEXECUTION);
      fakeConfig.getExecutionConfig().setShowStart(true);

      TestTransformer testTransformer = ExecutorCreator.createTestTransformer(folders, executionConfig, fakeConfig);
      return testTransformer;
   }
}
