package de.peran.analysis.helper.all;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.persistence.Dependencies;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.measurement.analysis.Cleaner;
import de.peass.utils.Constants;

public class CleanSync {
   public static void main(String[] args) throws JAXBException, JsonParseException, JsonMappingException, IOException {
      File dependencyFolder = new File(CleanAll.defaultDependencyFolder);
      File dataFolder = new File("../measurement/scripts/versions/sync/");
      for (String project : new String[] { "commons-io", "commons-dbcp", "commons-csv", "commons-fileupload", "commons-compress" }) {
         // for (String project : new String[] {"commons-io"}){
         File dependencyFile = new File(dependencyFolder, "deps_" + project + ".xml");
         final Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
         VersionComparator.setDependencies(dependencies);
         File cleanFolder = new File(dataFolder, "projects" + File.separator  + project + File.separator + "measurementsFull");
         cleanFolder.mkdirs();
         for (File measurementFolder : dataFolder.listFiles()) {
            File projectFolder = new File(measurementFolder, project + "_peass");
            if (projectFolder.isDirectory()) {
               Cleaner transformer = new Cleaner(cleanFolder);
               transformer.processDataFolder(projectFolder);
            }
         }
      }
   }
}
