package de.peass.analysis.groups;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.properties.VersionChangeProperties;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peran.FolderSearcher;

public class CreateClassificationData {

   public static Classification getOldData(final File folder, final String project) {
      final Classification merged = new Classification();

      for (final File candidate : folder.listFiles()) {
         if (!candidate.getName().endsWith(".swp") && candidate.getName().startsWith(project)) {
            try {
               final Classification foundData = FolderSearcher.MAPPER.readValue(candidate, Classification.class);
               merged.merge(foundData);

            } catch (final IOException e) {
               System.out.println("File unreadable: " + candidate);
            }
         }
      }

      return merged;
   }

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      final File propertyFile = new File(args[0]);
      final File goalFile = new File(args[1]);
      final String project = goalFile.getName().substring(0, goalFile.getName().indexOf('.'));
      System.out.println("Project: " + project);
      final Classification merged = getOldData(goalFile.getParentFile(), project);
      
      final File moved = moveToUnnamed(goalFile);

      final VersionChangeProperties properties = FolderSearcher.MAPPER.readValue(propertyFile, VersionChangeProperties.class);

      final Classification manualTemplate = new Classification();
      
      properties.executeProcessor((version, test, testcaseProperties, versionProperties) -> {
         if (version.equals("abb29f08629aa42ff44934c334de8cbc684a34ec")) {
            System.out.println("test");
         }
         final ChangedEntity testEntity = new ChangedEntity(test, "", testcaseProperties.getMethod());
         final Set<String> guessed = new TreeSet<>(testcaseProperties.getGuessedTypes());
         final String direction = (testcaseProperties.getChangePercent() > 0) ? "FASTER" : "SLOWER";
         manualTemplate.addChange(version, testEntity, guessed, direction);
      });
      FolderSearcher.MAPPER.writeValue(new File(goalFile.getParent(), "temp.json"), manualTemplate);
      
      manualTemplate.merge(merged);
      
      FolderSearcher.MAPPER.writeValue(goalFile, manualTemplate);
      
      if (moved != null) {
         if (FileUtils.contentEquals(moved, goalFile)) {
            moved.delete();
         }
      }
   }

   static File moveToUnnamed(final File goalFile) {
      File renamed = null;
      if (goalFile.exists()) {
         boolean moved = false;
         int index = 0;
         while (!moved) {
            final File move = new File(goalFile.getParentFile(), goalFile.getName() + "." + index);
            if (!move.exists()) {
               goalFile.renameTo(move);
               renamed = move;
               moved = true;
            }
            index++;
         }
      }
      return renamed;
   }
}
