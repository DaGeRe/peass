package de.dagere.peass.vcs;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;

public class TestGitCommitWriter {
   
   private static final File COMMIT_FILE = new File("target/test-classes/commits.json");
   
   @BeforeEach
   public void init() {
      COMMIT_FILE.delete();
   }
   
   @Test
   public void testRegularWriting() throws StreamReadException, DatabindException, StreamWriteException, IOException {
      PeassFolders folders = Mockito.mock(PeassFolders.class);
      Mockito.when(folders.getProjectFolder()).thenReturn(new File("."));
      List<String> firstPeassCommits = Arrays.asList("d11aa4558e9d16a98af572aea6d52e13af7cf974", "35310a36d782e67520fb03321ebec6b597dd7604");
      ResultsFolders resultsFolders = Mockito.mock(ResultsFolders.class);
      Mockito.when(resultsFolders.getCommitMetadataFile()).thenReturn(COMMIT_FILE);
      GitCommitWriter.writeCurrentCommits(folders, firstPeassCommits, resultsFolders);
      
      CommitList commits = Constants.OBJECTMAPPER.readValue(COMMIT_FILE, CommitList.class);
      Assert.assertEquals(2, commits.getCommits().size());
      
      List<String> nextPeassCommits = Arrays.asList("35310a36d782e67520fb03321ebec6b597dd7604", "3fc6202b956064415ae4ec328e52e491e3b65787", "4c193caead814bd77daaac52f58b219ce1532348");
      GitCommitWriter.writeCurrentCommits(folders, nextPeassCommits, resultsFolders);
      
      CommitList commitsAfterSecondWriting = Constants.OBJECTMAPPER.readValue(COMMIT_FILE, CommitList.class);
      Assert.assertEquals(4, commitsAfterSecondWriting.getCommits().size());
   }
}
