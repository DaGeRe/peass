package de.peass.breaksearch.treeanalysis;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.config.MeasurementConfiguration;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.utils.Constants;

public class TreeAnalysis {
   public static final MeasurementConfiguration config = new MeasurementConfiguration(2);
   static {
      config.setType1error(0.1);
      config.setType2error(0.1);
   }

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      final File treesFolder = new File(args[0]);
      

      final TestVersionPairManager manager = new TestVersionPairManager();
      if (args.length > 1) {
         final File treesFolderCorrect = new File(args[1]);
         getAllTreeData(treesFolderCorrect, manager);
      }
      getAllTreeData(treesFolder, manager);

      manager.printChanged();
      // manager.printAll();
   }

   private static void getAllTreeData(final File treesFolder, final TestVersionPairManager manager) throws IOException, JsonParseException, JsonMappingException {
      for (final File resultFolder : treesFolder.listFiles()) {
         File treeFolder = new File(resultFolder, "peass" + File.separator + "rca" + File.separator + "tree");
         final File treeFolder2 = new File(resultFolder, File.separator + "rca" + File.separator + "tree");
         if (treeFolder2.isDirectory()) {
            treeFolder = treeFolder2;
         }
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
   }
}
