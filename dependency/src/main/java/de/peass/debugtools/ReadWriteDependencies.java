package de.peass.debugtools;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.utils.Constants;

/**
 * Read and write dependency, in order to get standardized sorting
 * 
 * @author reichelt
 *
 */
public class ReadWriteDependencies {
   public static void main(final String[] args) throws JsonGenerationException, JsonMappingException, IOException {
      final File input = new File(args[0]);
      if (input.getName().startsWith("dep")) {
         final Dependencies deps = Constants.OBJECTMAPPER.readValue(input, Dependencies.class);
         Constants.OBJECTMAPPER.writeValue(new File(input.getParentFile(), input.getName() + ".out"), deps);
      } else if (input.getName().startsWith("exec")) {
         final ExecutionData deps = Constants.OBJECTMAPPER.readValue(input, ExecutionData.class);
         Constants.OBJECTMAPPER.writeValue(new File(input.getParentFile(), input.getName() + ".out"), deps);
      }

   }
}
