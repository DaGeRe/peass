package de.peass.measurement.rca;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.measurement.rca.CausePersistenceManager;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.CauseSearchFolders;
import de.peass.measurement.rca.helper.TestConstants;
import de.peass.measurement.rca.helper.TreeBuilder;
import de.peass.utils.Constants;

public class TestCausePersistenceManager {
   @Test
   public void testCallTreeToMeasuredNode() throws Exception {
      final CauseSearchFolders folders = new CauseSearchFolders(TestConstants.getCurrentFolder());

      testConfig(folders, TestConstants.SIMPLE_MEASUREMENT_CONFIG);

      TestConstants.getCurrentFolder();
      final MeasurementConfiguration config = new MeasurementConfiguration(3, "000001", "000001~1");
      config.setIterations(5);
      testConfig(folders, config);
   }
   
   @Test
   public void testDetailWriting() throws JsonGenerationException, JsonMappingException, JsonParseException, IOException {
      final MeasurementConfiguration config = new MeasurementConfiguration(3, "000001", "000001~1");
      final CauseSearchFolders folders = new CauseSearchFolders(TestConstants.getCurrentFolder());
      
      writeData(folders, config, false);
      final File expectedResultFile = new File(folders.getRcaTreeFolder(), "000001" + File.separator + 
            "Test" + File.separator + 
            "details" + File.separator + "test.json");
      Assert.assertTrue(expectedResultFile.exists());
      
      final CauseSearchData data = Constants.OBJECTMAPPER.readValue(expectedResultFile, CauseSearchData.class);
      
      Assert.assertEquals(3, data.getNodes().getValues().getValues().size());
   }

   private void testConfig(final CauseSearchFolders folders, final MeasurementConfiguration config)
         throws IOException, JsonGenerationException, JsonMappingException, JsonParseException {
      writeData(folders, config, true);

      final File expectedResultFile = new File(folders.getRcaTreeFolder(), "000001" + File.separator + "Test" + File.separator + "test.json");
      Assert.assertTrue(expectedResultFile.exists());

      final CauseSearchData data = Constants.OBJECTMAPPER.readValue(expectedResultFile, CauseSearchData.class);

      Assert.assertEquals(config.getVms(), data.getNodes().getStatistic().getVMs());
      Assert.assertEquals(config.getVms() * config.getIterations(), data.getNodes().getStatistic().getCalls());
      Assert.assertEquals(config.getVms() * config.getIterations(), data.getNodes().getStatistic().getCallsOld());
   }

   private void writeData(final CauseSearchFolders folders, final MeasurementConfiguration config, final boolean useFullLogAPI) throws IOException, JsonGenerationException, JsonMappingException {
      final CausePersistenceManager manager = new CausePersistenceManager(TestConstants.SIMPLE_CAUSE_CONFIG, config, folders);

      final TreeBuilder builder = new TreeBuilder(config, useFullLogAPI);
      builder.buildMeasurements(builder.getRoot());
      manager.addMeasurement(builder.getRoot());

      manager.writeTreeState();
   }
}
