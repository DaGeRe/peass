package de.dagere.peass.dependency.jmh;

import java.io.File;
import java.io.FileFilter;

import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class JmhResultMover {
   
   private final PeASSFolders folders;
   
   public JmhResultMover(final PeASSFolders folders) {
      this.folders = folders;
   }

   public void moveToMethodFolder(final TestCase test, final File jsonResultFile) {
      final File[] files = folders.getTempMeasurementFolder().listFiles(new FileFilter() {

         @Override
         public boolean accept(final File pathname) {
            return pathname.getName().startsWith("kieker-");
         }
      });
      final File expectedResultFolder = files[0];

      final File clazzFolder = new File(folders.getTempMeasurementFolder(), test.getClazz());
      
      final File kiekerTimeFolder = new File(clazzFolder, Long.toString(System.currentTimeMillis()) + File.separator + test.getMethod());
      kiekerTimeFolder.mkdirs();

      final File kiekerSubfolder = new File(kiekerTimeFolder, expectedResultFolder.getName());
      expectedResultFolder.renameTo(kiekerSubfolder);
      
      final File expectedKoPeMeFile = new File(clazzFolder, test.getMethod() + ".xml");
      jsonResultFile.renameTo(expectedKoPeMeFile);
   }
}
