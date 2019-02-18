package de.peran.measurement.analysis.changes.processing;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.peass.analysis.changes.Change;
import de.peass.analysis.changes.ProjectChanges;
import de.peran.measurement.analysis.changes.processors.ChangeProcessor;

public class CreateRunScript {
   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      final File input = new File(args[0]);
      final ProjectChanges changes = new ObjectMapper().readValue(input, ProjectChanges.class);
      
      changes.executeProcessor(new ChangeProcessor() {
         
         @Override
         public void process(final String version, final String testcase, final Change change) {
//            if (change.getCorrectness() == null || !change.getCorrectness().equals("CORRECT")) {
//               System.out.print("java -cp target/measurement-0.1-SNAPSHOT.jar de.peran.AdaptiveTestStarter "
//                     + "-test " + testcase + "#" + change.getMethod() + " "
//                     + "-warmup 0 "
//                     + "-iterations 1000 "
//                     + "-repetitions 100 "
//                     + "-vms 100 "
//                     + "-timeout 300 "
//                     + "-startversion " + version + " "
//                     + "-endversion " + version + " "
//                     + "-executionfile ../execute-commons-io.json "
//                     + "-folder ../../projekte/commons-io/ "
//                     + "-dependencyfile ../dependency/deps_commons-io.xml &> measurement_" + version.substring(0, 6) + "_" + change.getMethod() + ".txt\n");
//            }
            
         }
      });
   }
}
