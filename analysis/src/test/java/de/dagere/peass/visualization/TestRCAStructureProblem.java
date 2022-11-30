package de.dagere.peass.visualization;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.utils.Constants;

public class TestRCAStructureProblem {
   @BeforeEach
   public void init() throws IOException {
      if (TestRCAGenerator.RESULT_FOLDER.exists()) {
         FileUtils.cleanDirectory(TestRCAGenerator.RESULT_FOLDER);
      }
   }

   @Test
   public void testBeforeProblem() throws IOException {
      File folder = new File(TestRCAGenerator.VISUALIZATION_FOLDER, "singleTreeBeforeProblem/demo-project_peass");

      CauseSearchFolders folders = new CauseSearchFolders(folder);
      File sourceFile = new File(folder, "rca/treeMeasurementResults/" + "6ce9d6a3154c4ce8f617c357cf466fab222d27ef" + "/ExampleTest/details/test.json");

      RCAGenerator generator = new RCAGenerator(sourceFile, TestRCAGenerator.RESULT_FOLDER, folders);

      generator.setPropertyFolder(new File(TestRCAGenerator.VISUALIZATION_FOLDER, "singleTreeBeforeProblem/properties_demo-project"));

      createTreeStructureView("6ce9d6a3154c4ce8f617c357cf466fab222d27ef", folders, generator);
   }
   
   private void createTreeStructureView(String commit, CauseSearchFolders folders, RCAGenerator generator) throws IOException, StreamReadException, DatabindException {
      File treeFolder = folders.getExistingTreeCacheFolder(commit, new TestMethodCall("de.dagere.peass.ExampleTest", "test"));
      if (treeFolder.exists() && commit.equals("6ce9d6a3154c4ce8f617c357cf466fab222d27ef")) {
         final File potentialCacheFileOld = new File(treeFolder, "dc90ca044d1aa98688ef2a7142f9019560df7a24");
         final File potentialCacheFile = new File(treeFolder, "6ce9d6a3154c4ce8f617c357cf466fab222d27ef");

         final CallTreeNode rootPredecessor = Constants.OBJECTMAPPER.readValue(potentialCacheFileOld, CallTreeNode.class);
         final CallTreeNode rootCurrent = Constants.OBJECTMAPPER.readValue(potentialCacheFile, CallTreeNode.class);

         generator.createSingleVisualization(commit, rootCurrent);
         generator.createSingleVisualization("dc90ca044d1aa98688ef2a7142f9019560df7a24", rootPredecessor);
      }
   }
}
