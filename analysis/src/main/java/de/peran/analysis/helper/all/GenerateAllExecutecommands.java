package de.peran.analysis.helper.all;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.analysis.all.RepoFolders;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.utils.DivideVersions;
import de.peran.FolderSearcher;

public class GenerateAllExecutecommands {
   public static void main(final String[] args) throws JAXBException, JsonParseException, JsonMappingException, IOException {
      final File dependencyFolder = args.length > 0 ? new File(args[0]) : new RepoFolders().getDependencyFolder();
      for (final String project : new String[] { "commons-io", "commons-dbcp", "commons-compress", "commons-csv", "commons-fileupload", "commons-imaging", "commons-text" }) {
         final File dependencyFile = new File(dependencyFolder, "deps_" + project + ".json");
         if (dependencyFile.exists()) {
            final Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
            VersionComparator.setDependencies(dependencies);
            final File executeFile = new File(dependencyFolder, "execute_" + project + ".json");
            final ExecutionData changedTests = FolderSearcher.MAPPER.readValue(executeFile, ExecutionData.class);
            DivideVersions.generateExecuteCommands(dependencies, changedTests, "generated", System.out);
         } else {
            System.out.println("Not existing: " + dependencyFile.getAbsolutePath());
         }
      }
   }
}
