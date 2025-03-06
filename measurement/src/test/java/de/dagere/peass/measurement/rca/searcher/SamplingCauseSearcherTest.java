package de.dagere.peass.measurement.rca.searcher;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.MeasurementStrategy;
import de.dagere.peass.config.parameters.ExecutionConfigMixin;
import de.dagere.peass.config.parameters.KiekerConfigMixin;
import de.dagere.peass.config.parameters.MeasurementConfigurationMixin;
import de.dagere.peass.config.parameters.StatisticsConfigMixin;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.CauseSearcherConfigMixin;
import de.dagere.peass.measurement.rca.RCAStrategy;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import io.github.terahidro2003.cct.SamplerResultsProcessor;
import io.github.terahidro2003.measurement.data.MeasurementIdentifier;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;

public class SamplingCauseSearcherTest {

    private SamplingCauseSearcher getSearcher(boolean disableMeasurements, int vms, int iterations, int repetitions,
                                              int warmup, int interval, boolean iterativeSampling, String sjswUUID,
                                              String commit, String commitOld, File projectFolder, String testcase) {
        MeasurementConfigurationMixin measurementConfigurationMixin = new MeasurementConfigurationMixin();
        measurementConfigurationMixin.setDisableMeasurements(disableMeasurements);
        measurementConfigurationMixin.setVms(vms);
        measurementConfigurationMixin.setIterations(iterations);
        measurementConfigurationMixin.setRepetitions(repetitions);
        measurementConfigurationMixin.setWarmup(warmup);
        measurementConfigurationMixin.setInterval(interval);
        measurementConfigurationMixin.setMeasurementStrategy(MeasurementStrategy.SEQUENTIAL);
        measurementConfigurationMixin.setUseIterativeSampling(iterativeSampling);
        measurementConfigurationMixin.setSamplingResultUUID(sjswUUID);

        ExecutionConfigMixin executionConfigMixin = new ExecutionConfigMixin();
        StatisticsConfigMixin statisticsConfigMixin = new StatisticsConfigMixin();
        KiekerConfigMixin kiekerConfigMixin = new KiekerConfigMixin();
        MeasurementConfig measurementConfig = new MeasurementConfig(measurementConfigurationMixin, executionConfigMixin, statisticsConfigMixin, kiekerConfigMixin);
        measurementConfig.getFixedCommitConfig().setCommit(commit);
        measurementConfig.getFixedCommitConfig().setCommitOld(commitOld);

        final CauseSearchFolders alternateFolders = new CauseSearchFolders(projectFolder);
        EnvironmentVariables environmentVariables = new EnvironmentVariables();
        CauseSearcherConfigMixin causeSearcherConfigMixin = new CauseSearcherConfigMixin();
        causeSearcherConfigMixin.setStrategy(RCAStrategy.SAMPLING);
        TestMethodCall test = TestMethodCall.createFromString(testcase);
        CauseSearcherConfig config = new CauseSearcherConfig(test, causeSearcherConfigMixin);

        return new SamplingCauseSearcher(test, measurementConfig, alternateFolders, environmentVariables, config);
    }

    private void cleanup(File peassRcaResultFolder) {
        if(peassRcaResultFolder.exists()) {
            peassRcaResultFolder.listFiles()[0].delete();
        }
        peassRcaResultFolder.delete();
    }

    @Test
    void testTreeMeasurements() {
        String projectName = "project_2";
        String testcase = "de.dagere.peass.MainTest#testMe";
        int vms = 2;
        int iterations = 5;
        int repetitions = 10000;
        int warmup = 0;
        int interval = 10;
        boolean iterativeSampling = true;
        String sjswUUID = "899bf630-06e6-456c-81ee-a0803a942923";
        String commit = "453d8deb392581e5f047370ecf660ad7c44dcce6";
        String commitOld = "a99d8b731939747ea1da77dc5c085f2cb7cfefa1";
        File projectFolder = new File("src/test/resources/samplingTestData/" + projectName);

        // cleanup
        File peassRcaResultFolder = new File(new File(projectFolder + "_peass", "rca"), "treeMeasurementResults");
        cleanup(peassRcaResultFolder);

        peassRcaResultFolder.delete();

        SamplingCauseSearcher searcher = getSearcher(true, vms, iterations, repetitions, warmup, interval, iterativeSampling, sjswUUID, commit, commitOld, projectFolder, testcase);

        MeasurementIdentifier measurementIdentifier = new MeasurementIdentifier(UUID.fromString(sjswUUID));
        CallTreeNode root = searcher.generateTree(new SamplerResultsProcessor(), measurementIdentifier, 2);

        root.getData().get(commit).getResults().forEach(value -> {
            value.getValues().forEach(v -> {
                System.out.println(v);
            });
        });

        int countOfValues = root.getData().get(commit).getResults().get(0).getValues().size();
        MatcherAssert.assertThat(countOfValues, Matchers.greaterThan(1));
    }
}
