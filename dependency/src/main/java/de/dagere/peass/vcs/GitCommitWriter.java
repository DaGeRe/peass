package de.dagere.peass.vcs;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;

public class GitCommitWriter {
   public static void writeCurrentCommits(PeassFolders folders, List<String> current, ResultsFolders resultsFolders)
         throws IOException, StreamReadException, DatabindException, StreamWriteException {
      List<GitCommit> commitMetadata = GitUtils.getCommitMetadata(folders.getProjectFolder(), current);
      if (resultsFolders.getCommitMetadataFile().exists()) {
         try {
            CommitList old = Constants.OBJECTMAPPER.readValue(resultsFolders.getCommitMetadataFile(), CommitList.class);
            old.addCommits(commitMetadata);
            Constants.OBJECTMAPPER.writeValue(resultsFolders.getCommitMetadataFile(), old);
         } catch (JsonMappingException e) {
            e.printStackTrace();
            serializeOnlyCurrent(resultsFolders, commitMetadata);
         }
      } else {
         serializeOnlyCurrent(resultsFolders, commitMetadata);
      }

   }

   private static void serializeOnlyCurrent(ResultsFolders resultsFolders, List<GitCommit> commitMetadata) throws IOException, StreamWriteException, DatabindException {
      CommitList value = new CommitList();
      value.getCommits().addAll(commitMetadata);
      Constants.OBJECTMAPPER.writeValue(resultsFolders.getCommitMetadataFile(), value);
   }
}
