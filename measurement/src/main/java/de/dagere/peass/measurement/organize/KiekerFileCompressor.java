package de.dagere.peass.measurement.organize;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KiekerFileCompressor {
   
   private static final Logger LOG = LogManager.getLogger(KiekerFileCompressor.class);
   
   private int thresholdForZippingInMB = 5;
   private int thresholdForDeletingInMB = 1000;
   
   public void moveOrCompressFile(final File destFolder, final File kiekerFolder) throws IOException {
      final long size = FileUtils.sizeOf(kiekerFolder);
      final long sizeInMb = size / (1024 * 1024);
      LOG.debug("Kieker folder size: {} MB ({})", sizeInMb, size);
      if (sizeInMb > thresholdForDeletingInMB) {
         LOG.info("Result folder {} too big ({} MB) for saving; deleting completely", destFolder, sizeInMb);
         FileUtils.deleteDirectory(kiekerFolder);
      } else if (sizeInMb > thresholdForZippingInMB) {
         compressKiekerFolder(destFolder, kiekerFolder);
         FileUtils.deleteDirectory(kiekerFolder);
      } else {
         final File dest = new File(destFolder, kiekerFolder.getName());
         FileUtils.moveDirectory(kiekerFolder, dest);
      }
   }

   private void compressKiekerFolder(final File destFolder, final File kiekerFolder) throws IOException {
      final File dest = new File(destFolder, kiekerFolder.getName() + ".tar");
      final ProcessBuilder processBuilder = new ProcessBuilder("tar", "-czf", dest.getAbsolutePath(), kiekerFolder.getAbsolutePath());
      processBuilder.environment().put("GZIP", "-9");
      final Process process = processBuilder.start();
      try {
         process.waitFor();
      } catch (final InterruptedException e) {
         e.printStackTrace();
      }
   }
   
   public int getThresholdForDeletingInMB() {
      return thresholdForDeletingInMB;
   }
   
   public void setThresholdForDeletingInMB(final int thresholdForDeletingInMB) {
      this.thresholdForDeletingInMB = thresholdForDeletingInMB;
   }

   public int getThresholdForZippingInMB() {
      return thresholdForZippingInMB;
   }

   public void setThresholdForZippingInMB(final int thresholdForZippingInMB) {
      this.thresholdForZippingInMB = thresholdForZippingInMB;
   }
}
