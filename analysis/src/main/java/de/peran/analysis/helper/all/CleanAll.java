package de.peran.analysis.helper.all;

import java.io.File;

import javax.xml.bind.JAXBException;

import de.peass.dependency.persistence.Dependencies;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.measurement.analysis.Cleaner;
import de.peass.statistics.DependencyStatisticAnalyzer;

public class CleanAll {
   public static final String[] allProjects = new String[] { "commons-compress", "commons-csv", "commons-dbcp", "commons-fileupload", "commons-imaging", "commons-io",
         "commons-numbers", "commons-text", "k-9", "commons-pool", "commons-jcs" };

   public static final String defaultDependencyFolder = "/home/reichelt/daten3/diss/repos/dependencies-final";
   public static final String defaultDataFolder = "/home/reichelt/daten3/diss/repos/measurementdata";

   public static void main(final String[] args) throws JAXBException {
      final File dependencyFolder = new File(defaultDependencyFolder);
      final File dataFolder = new File(args.length > 0 ? args[0] : defaultDataFolder);
      for (final String project : allProjects) {
         // for (String project : new String[] {"commons-dbcp"}){
         final File dependencyFile = new File(dependencyFolder, "deps_" + project + ".xml");
         if (dependencyFile.exists()) {
            final Dependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
            VersionComparator.setDependencies(dependencies);
            final File projectFolder = new File(dataFolder, project);
            final File cleanFolder = new File(projectFolder, "clean" + File.separator + "measurementsFull");
            if (projectFolder.exists()) {
               final Cleaner transformer = new Cleaner(cleanFolder);
               for (final File measurementFolder : projectFolder.listFiles()) {
                  if (measurementFolder.isDirectory() && !measurementFolder.getName().equals("clean")) {
                     transformer.processDataFolder(measurementFolder);
                  }
               }
            }
         } else {
            System.out.println("No dependency file: " + dependencyFile.getAbsolutePath());
         }
      }
   }
}
