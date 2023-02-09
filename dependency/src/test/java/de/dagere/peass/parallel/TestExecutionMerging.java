package de.dagere.peass.parallel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.TestUtil;
import de.dagere.peass.debugtools.OnlyMergeTestSelection;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.parallel.PartialSelectionResultsMerger;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.CommitUtil;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionControlSystem;

public class TestExecutionMerging {

   private static final List<String> COMMIT_NAMES = Arrays.asList(new String[] {
         "v01", "v02", "v03", "asd12", "asd13", "asd14", "xyz12", "xyz13", "xyz14"
   });
   
   @Test
   public void testOnlyMergingStarter() throws Exception {
      File basicFolder = new File("target/basic-results/");
      File mergedFolder = new File("target/merged-results/");

      basicFolder.mkdirs();
      TestUtil.deleteContents(basicFolder);
      TestUtil.deleteContents(mergedFolder);

      File projectFolder = new File("target/project").getAbsoluteFile();

      for (int i = 0; i < 3; i++) {
         File resultFolder = new File(basicFolder, "folder_" + i);
         resultFolder.mkdirs();
         ExecutionData ex1 = createExecutionData(i);
         Constants.OBJECTMAPPER.writeValue(new File(resultFolder, "traceTestSelection_project.json"), ex1);
         Constants.OBJECTMAPPER.writeValue(new File(resultFolder, "staticTestSelection_project.json"), new StaticTestSelection());
      }

      try (MockedStatic<CommitUtil> mockedStatic = Mockito.mockStatic(CommitUtil.class)) {
         
         mockedStatic.when(() -> CommitUtil.getGitCommits(null, null, projectFolder, true))
               .thenReturn(COMMIT_NAMES);

         OnlyMergeTestSelection.main(new String[] { "-baseFolder", basicFolder.getAbsolutePath(), "-mergedFolder", mergedFolder.getAbsolutePath(),
               "-folder", projectFolder.getAbsolutePath() });
      }
      
      File expectedResultFile = new File(mergedFolder, "traceTestSelection_project.json");
      ExecutionData merged = Constants.OBJECTMAPPER.readValue(expectedResultFile, ExecutionData.class);
      Assert.assertEquals("v01", merged.getCommitNames()[0]);
      Assert.assertEquals("asd12", merged.getCommitNames()[3]);
      Assert.assertEquals("xyz12", merged.getCommitNames()[6]);
   }

   @Test
   public void mergeExecutions() {
      ExecutionData ex1 = createExecutionData(0);
      ExecutionData ex2 = createExecutionData(1);
      ExecutionData ex3 = createExecutionData(2);

      ExecutionData merged = PartialSelectionResultsMerger.mergeExecutiondata(Arrays.asList(new ExecutionData[] { ex1, ex2, ex3 }), 
            new CommitComparatorInstance(COMMIT_NAMES));

      checkMergedResult(merged);
   }

   @Test
   public void mergeWrittenExecutions() throws JsonGenerationException, JsonMappingException, IOException {
      ResultsFolders folders[] = new ResultsFolders[3];

      for (int i = 0; i < 3; i++) {
         ExecutionData ex1 = createExecutionData(i);
         Constants.OBJECTMAPPER.writeValue(new File("target/traceTestSelection_" + i + ".json"), ex1);
         folders[i] = Mockito.mock(ResultsFolders.class);
         Mockito.when(folders[i].getTraceTestSelectionFile()).thenReturn(new File("target/traceTestSelection_" + i + ".json"));
      }

      ResultsFolders out = new ResultsFolders(new File("target"), "mytest");
      ExecutionData merged = PartialSelectionResultsMerger.mergeExecutions(out, folders, new CommitComparatorInstance(COMMIT_NAMES));

      checkMergedResult(merged);
   }

   private void checkMergedResult(final ExecutionData merged) {
      List<String> keys = new ArrayList<>(merged.getCommits().keySet());
      Assert.assertEquals("v01", keys.get(0));
      Assert.assertEquals("v02", keys.get(1));
      Assert.assertEquals("asd12", keys.get(3));
      Assert.assertEquals("xyz14", keys.get(8));
   }

   private ExecutionData createExecutionData(final int index) {
      switch (index) {
      case 0:
         ExecutionData ex1 = new ExecutionData();
         ex1.addCall("v01", new TestSet("TestA"));
         ex1.addCall("v02", new TestSet("TestB"));
         ex1.addCall("v03", new TestSet("TestC"));
         return ex1;
      case 1:
         ExecutionData ex2 = new ExecutionData();
         ex2.addCall("asd12", new TestSet("TestA"));
         ex2.addCall("asd13", new TestSet("TestB"));
         ex2.addCall("asd14", new TestSet("TestC"));
         return ex2;
      case 2:
         ExecutionData ex3 = new ExecutionData();
         ex3.addCall("xyz12", new TestSet("TestA"));
         ex3.addCall("xyz13", new TestSet("TestB"));
         ex3.addCall("xyz14", new TestSet("TestC"));
         return ex3;
      }
      return null;

   }
}
