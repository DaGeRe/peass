package de.peass.debugtools;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.utils.Constants;

/**
 * Read and write dependency, in order to get standardized sorting (just local debug code - no public use intended)
 * 
 * @author reichelt
 *
 */
public class ReadWriteDependencies {
   public static void main(final String[] args) throws JsonGenerationException, JsonMappingException, IOException {
      if (args.length > 0) {
         final File input = new File(args[0]);
         readWrite(input);
      }

      File folder = new File("/home/reichelt/daten3/diss/repos/dependencies-final");
      for (File file : folder.listFiles((FilenameFilter) new WildcardFileFilter("execute_*.json"))) {
         readWrite(file);
      }

   }

   public static void readWrite(final File input) throws IOException, JsonParseException, JsonMappingException, JsonGenerationException {
      if (input.getName().startsWith("dep")) {
         final Dependencies deps = Constants.OBJECTMAPPER.readValue(input, Dependencies.class);
         Constants.OBJECTMAPPER.writeValue(new File(input.getParentFile(), input.getName()), deps);
      } else if (input.getName().startsWith("exec")) {
         final ExecutionData deps = Constants.OBJECTMAPPER.readValue(input, ExecutionData.class);
         Constants.OBJECTMAPPER.writeValue(new File(input.getParentFile(), input.getName()), deps);
      }
   }
}
