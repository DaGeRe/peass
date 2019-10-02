package de.peass.breaksearch.treeanalysis;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.measurement.searchcause.data.CauseSearchData;
import de.peass.utils.Constants;

public class TreeAnalysis {

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      final File treesFolder = new File(args[0]);

      final TestVersionPairManager manager = new TestVersionPairManager();

      for (final File resultFolder : treesFolder.listFiles()) {
         final File treeFolder = new File(resultFolder, "peass" + File.separator + "rca" + File.separator + "tree");
         if (treeFolder.isDirectory()) {
            final File versionFolder = treeFolder.listFiles()[0];
            if (versionFolder.getName().startsWith("4ed")) {
               final File testcaseFolder = versionFolder.listFiles()[0];
               for (final File treeJson : testcaseFolder.listFiles((FileFilter) new WildcardFileFilter("*.json"))) {
                  if (treeJson.getName().contains("testFoldedHeaders")) {
                     System.out.println("Folder: " + treeJson.getAbsolutePath());
                     final CauseSearchData data = Constants.OBJECTMAPPER.readValue(treeJson, CauseSearchData.class);
                     manager.addData(data);
                  }
               }
            }
         } else {
            System.out.println("Not analyzable: " + treeFolder.getAbsolutePath());
         }

      }

      manager.printAll();
   }
}
