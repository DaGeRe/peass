package de.dagere.peass.dependency.jmh;

import java.io.File;
import java.io.FileFilter;

import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.traces.KiekerFolderUtil;

public class JmhResultMover {
   
   private final PeASSFolders folders;
   private final File sourceResultFolder;
   
   public JmhResultMover(final PeASSFolders folders) {
      this.folders = folders;
      final File[] files = folders.getTempMeasurementFolder().listFiles(new FileFilter() {

         @Override
         public boolean accept(final File pathname) {
            return pathname.getName().startsWith("kieker-");
         }
      });
      sourceResultFolder = files[0];
   }

   public void moveToMethodFolder(final TestCase testcase, final File sourceJsonResultFile) {
      final File moduleResultsFolder = KiekerFolderUtil.getModuleResultFolder(folders, testcase);
      final File clazzResultFolder = new File(moduleResultsFolder, testcase.getClazz());
      
      final File kiekerTimeFolder = new File(clazzResultFolder, Long.toString(System.currentTimeMillis()) + File.separator + testcase.getMethod());
      kiekerTimeFolder.mkdirs();

      final File kiekerSubfolder = new File(kiekerTimeFolder, sourceResultFolder.getName());
      sourceResultFolder.renameTo(kiekerSubfolder);
      
      final File expectedKoPeMeFile = new File(clazzResultFolder, testcase.getMethod() + ".xml");
      sourceJsonResultFile.renameTo(expectedKoPeMeFile);
   }
}
