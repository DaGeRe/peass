package de.dagere.peass.dependency.jmh;

import java.io.File;
import java.io.FileFilter;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.traces.KiekerFolderUtil;

public class JmhResultMover {

   private final PeassFolders folders;
   private final MeasurementConfiguration measurementConfig;
   private final File[] sourceResultFolders;

   public JmhResultMover(final PeassFolders folders, final MeasurementConfiguration measurementConfig) {
      this.folders = folders;
      this.measurementConfig = measurementConfig;
      final File[] files = folders.getTempMeasurementFolder().listFiles(new FileFilter() {

         @Override
         public boolean accept(final File pathname) {
            return pathname.getName().startsWith("kieker-");
         }
      });
      if (files.length > 0) {
         sourceResultFolders = files;
      } else {
         sourceResultFolders = null;
      }
   }

   public void moveToMethodFolder(final TestCase testcase, final File sourceJsonResultFile) {
      final File moduleResultsFolder = KiekerFolderUtil.getModuleResultFolder(folders, testcase);
      final File clazzResultFolder = new File(moduleResultsFolder, testcase.getClazz());

      final File kiekerTimeFolder = new File(clazzResultFolder, Long.toString(System.currentTimeMillis()) + File.separator + testcase.getMethod());
      kiekerTimeFolder.mkdirs();

      // final File expectedKoPeMeFile = new File(clazzResultFolder, testcase.getMethod() + ".xml");
      new JmhKoPeMeConverter(measurementConfig).convertToXMLData(sourceJsonResultFile, clazzResultFolder);
      sourceJsonResultFile.delete();

      if (sourceResultFolders != null) {
         for (File sourceResultFolder : sourceResultFolders) {
            final File kiekerSubfolder = new File(kiekerTimeFolder, sourceResultFolder.getName());
            sourceResultFolder.renameTo(kiekerSubfolder);
         }
      }
   }
}
