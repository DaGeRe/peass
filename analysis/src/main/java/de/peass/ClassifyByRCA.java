package de.peass;

import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.Callable;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class ClassifyByRCA implements Callable<Void> {

   @Option(names = { "-datafolder", "--datafolder" }, description = "Folder which should be used for analysis", required = true)
   private File datafolder;

   public static void main(String[] args) {
      final CommandLine commandLine = new CommandLine(new ClassifyByRCA());
      commandLine.execute(args);
   }

   @Override
   public Void call() throws Exception {

      for (File job : datafolder.listFiles()) {
         File commitsFolders = new File(job, "peass/rca/tree/");
         for (File commit : commitsFolders.listFiles()) {
            for (File test : commit.listFiles()) {
               for (File json : test.listFiles((FileFilter) new WildcardFileFilter("*.json"))) {
                  CauseSearchData csd = Constants.OBJECTMAPPER.readValue(json, CauseSearchData.class);
                  
               }
            }
         }
      }
      return null;
   }
}
