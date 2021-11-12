package de.dagere.peass.breaksearch;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.utils.OptionConstants;

public class DependencyLoader {
   
   private static final Logger LOG = LogManager.getLogger(FindLowestVMCount.class);
   
   public static void loadDependencies(final CommandLine line) throws JsonParseException, JsonMappingException, IOException {
      if (line.hasOption(OptionConstants.DEPENDENCYFILE.getName())) {
         final File dependencyFile = new File(line.getOptionValue(OptionConstants.DEPENDENCYFILE.getName()));
         final Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
         VersionComparator.setDependencies(dependencies);
      } else {
         LOG.error("No dependencyfile information passed.");
         throw new RuntimeException("No dependencyfile information passed.");
      }
   }
}
