package de.peran.analysis.helper.all;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.statistics.DependencyStatisticAnalyzer;
import de.peass.utils.DivideVersions;
import de.peran.FolderSearcher;

public class GenerateAllExecutecommands {
   public static void main(final String[] args) throws JAXBException, JsonParseException, JsonMappingException, IOException {
      final File dependencyFolder = new File(args.length > 0 ? args[0] : "/home/reichelt/daten/diss/ergebnisse/views/v15_mitEnums/data");
      for (final String project : new String[] { "commons-io", "commons-dbcp", "commons-compress", "commons-csv", "commons-fileupload", "commons-imaging", "commons-text" }) {
         final File dependencyFile = new File(dependencyFolder, "deps_"+project+".xml");
         if (dependencyFile.exists()) {
            final Dependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
            VersionComparator.setDependencies(dependencies);
            final ExecutionData changedTests = FolderSearcher.MAPPER.readValue(new File(dependencyFolder, "views_"+project+File.separator + "execute-"+project+".json"), ExecutionData.class);
            DivideVersions.generateExecuteCommands(dependencies, changedTests, "generated", new File("../measurement/scripts/execute-" + project + ".sh"), System.out);
         }
      }
   }
}
