package de.peass.analysis.changes;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.utils.Constants;

public class GetChangeClasses {
   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      final File f = new File("results/merged.json");
      final ProjectChanges knowledge = Constants.OBJECTMAPPER.readValue(f, ProjectChanges.class);
      final Map<String, DescriptiveStatistics> clazzes = new HashMap<>();
      for (final Changes changes : knowledge.getVersionChanges().values()) {
         for (final List<Change> change : changes.getTestcaseChanges().values()) {
            for (final Change c : change) {
//               if (c.getCorrectness() != null && c.getCorrectness().equals("CORRECT")) {
//                  for (String type : c.getType()) {
//                     if (type.equals("FUNCTION") || type.equals("BUGFIX") || type.equals("FEATURE")) {
//                        type = "FUNCTIONALITY";
//                     }
//                     if (type.equals("JUNIT")) {
//                        type = "LIB";
//                     }
//                     DescriptiveStatistics clazz = clazzes.get(type);
//                     if (clazz == null) {
//                        clazz = new DescriptiveStatistics();
//                        clazzes.put(type, clazz);
//                     }
//                     clazz.addValue(c.getChangePercent());
//                  }
//
//               }
            }
         }
      }
      for (final Map.Entry<String, DescriptiveStatistics> clazz : clazzes.entrySet()) {
         System.out.println(clazz.getKey() + " " + clazz.getValue().getMean());
      }
   }
}
