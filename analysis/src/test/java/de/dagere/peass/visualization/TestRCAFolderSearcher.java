package de.dagere.peass.visualization;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class TestRCAFolderSearcher {

   @Test
   public void testBasicSearching() throws IOException {
      RCAFolderSearcher searcher = new RCAFolderSearcher(new File("src/test/resources/visualization/project_3_peass"));
      List<File> files = searcher.searchRCAFiles();

      Assert.assertEquals(files.get(0).getName(), "testMe.json");
   }
   
   @Test
   public void testDirectJSON() throws IOException {
      RCAFolderSearcher searcher = new RCAFolderSearcher(new File("src/test/resources/visualization/project_3_peass/rca/treeMeasurementResults/9177678d505bfacb64a95c2271fb03b1e18475a8/MainTest/testMe.json"));
      List<File> files = searcher.searchRCAFiles();

      Assert.assertEquals(files.get(0).getName(), "testMe.json");
   }
   
   @Test
   public void testWrongFile() throws IOException {
      Assert.assertThrows(RuntimeException.class, () -> {
         RCAFolderSearcher searcher = new RCAFolderSearcher(new File("src/test/resources/visualization/project_3_peass/clearRCA.sh"));
         List<File> files = searcher.searchRCAFiles();
      });
   }
   
   @Test
   public void testCommitLimmitedSearching() throws IOException {
      RCAFolderSearcher searcher = new RCAFolderSearcher(new File("src/test/resources/visualization/project_3_peass"), "9177678d505bfacb64a95c2271fb03b1e18475a8");
      List<File> files = searcher.searchRCAFiles();

      Assert.assertEquals(files.get(0).getName(), "testMe.json");
   }
   
   @Test
   public void testNotExistingCommitSearching() throws IOException {
      RCAFolderSearcher searcher = new RCAFolderSearcher(new File("src/test/resources/visualization/project_3_peass"), "thisIsNotAValidCommit");
      List<File> files = searcher.searchRCAFiles();

      Assert.assertEquals(0, files.size());
   }
   
   
}
