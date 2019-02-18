package de.peran.measurement.analysis.changes.processing;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.peass.analysis.changes.Change;
import de.peass.analysis.changes.Changes;
import de.peass.analysis.changes.ProjectChanges;
import de.peran.AnalyseOneTest;

public class MergeChangeData {

   private final static ObjectMapper MAPPER = new ObjectMapper();

   public static void main(final String[] args) throws JsonGenerationException, JsonMappingException, IOException {
      final ProjectChanges merged = new ProjectChanges();
      for (final File json : AnalyseOneTest.RESULTFOLDER.listFiles()) {
         if (json.getName().endsWith(".json")) {
            try {
               final ProjectChanges knowledge = MAPPER.readValue(json, ProjectChanges.class);
               for (final Map.Entry<String, Changes> entry : knowledge.getVersionChanges().entrySet()) {
                  if (!merged.getVersionChanges().containsKey(entry.getKey())) {
                     merged.getVersionChanges().put(entry.getKey(), entry.getValue());
                  } else {
                     final Changes current = entry.getValue();
                     final Changes mergedVersion = merged.getVersion(entry.getKey());
                     for (final Map.Entry<String, List<Change>> changes : current.getTestcaseChanges().entrySet()) {
                        if (mergedVersion.getTestcaseChanges().containsKey(changes.getKey())) {
                           final List<Change> mergedChanges = mergedVersion.getTestcaseChanges().get(changes.getKey());
                           for (final Change change : changes.getValue()) {
                              boolean found = false;
                              for (final Change mergedChange : mergedChanges) {
                                 if (mergedChange.getDiff().equals(change.getDiff())) {
                                    found = true;
//                                    if (mergedChange.getCorrectness() == null && change.getCorrectness() != null) {
//                                       mergedChange.setCorrectness(change.getCorrectness());
//                                       mergedChange.setType(change.getType());
//                                    }
                                 }
                              }
                              if (!found) {
                                 mergedChanges.add(change);
                              }
                           }
                        } else {
                           mergedVersion.getTestcaseChanges().put(changes.getKey(), changes.getValue());
                        }
                     }
                  }
               }
            } catch (final IOException e) {
               e.printStackTrace();
            }
         }
      }

      merged.getVersionChanges().forEach((version, changes) -> {
         changes.getTestcaseChanges().forEach((test, changeList) -> {
//            changeList.forEach(change -> {
//               if (change.getCorrectness() != null && change.getCorrectness().equals("Y")) {
//                  change.setCorrectness("CORRECT");
//               }
//            });
         });
      });

      System.out.println("Writing..");
      final File mergedFile = new File(AnalyseOneTest.RESULTFOLDER, "merged.json");

      MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
      MAPPER.writeValue(mergedFile, merged);
   }
}
