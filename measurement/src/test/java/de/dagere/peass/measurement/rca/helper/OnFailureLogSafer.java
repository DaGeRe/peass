package de.dagere.peass.measurement.rca.helper;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class OnFailureLogSafer extends TestWatcher {

   private final File[] folderForSaving;

   public OnFailureLogSafer(final File... folderForSaving) {
      this.folderForSaving = folderForSaving;
   }

   @Override
   protected void failed(final Throwable e, final Description description) {
      File goal1 = getUniqueResultFolder();
      for (File saveFolder : folderForSaving) {
         try {
            FileUtils.copyDirectory(saveFolder, new File(goal1, saveFolder.getName()));
         } catch (IOException e1) {
            e1.printStackTrace();
         }
      }
      super.failed(e, description);
   }

   private File getUniqueResultFolder() {
      int index = 0;
      File parent = new File("target/surefire-reports/peass/");
      parent.mkdirs();
      File goal1 = new File(parent, "" + index);
      while (goal1.exists()) {
         index++;
         goal1 = new File(parent, "" + index);
      }
      return goal1;
   }
}