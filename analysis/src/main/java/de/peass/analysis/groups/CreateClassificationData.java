package de.peass.analysis.groups;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.changes.Change;
import de.peass.analysis.changes.ProjectChanges;
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
               merged.mergeAll(foundData);

            } catch (final IOException e) {
               System.out.println("File unreadable: " + candidate);
            }
         }
      }

      return merged;
   }

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      boolean isProperty = false;
      final File goalFile = new File(args[1]);
      final String project = goalFile.getName().substring(0, goalFile.getName().indexOf('.'));
      if (isProperty) {
         final File propertyFile = new File(args[0]);
         final VersionChangeProperties properties = FolderSearcher.MAPPER.readValue(propertyFile, VersionChangeProperties.class);
         createClassificationData(properties, goalFile, project);
      }else {
         final File changeFile = new File(args[0]);
         final ProjectChanges changes = FolderSearcher.MAPPER.readValue(changeFile, ProjectChanges.class);
         createClassificationData(changes, goalFile, project);
      }
      
   }
   
   public static void createClassificationData(final ProjectChanges changes, final File goalFile, final String project)
         throws IOException, JsonParseException, JsonMappingException, JsonGenerationException {
      final Classification manualTemplate = readCurrentData(changes, goalFile);
      mergeOldData(goalFile, project, manualTemplate);
   }
   
   public static void createClassificationData(final VersionChangeProperties properties, final File goalFile, final String project)
         throws IOException, JsonParseException, JsonMappingException, JsonGenerationException {
      final Classification manualTemplate = readChangesFromProperties(properties);
      FolderSearcher.MAPPER.writeValue(new File(goalFile.getParent(), "temp.json"), manualTemplate);
      mergeOldData(goalFile, project, manualTemplate);
   }

   public static void mergeOldData(final File goalFile, final String project, final Classification manualTemplate)
         throws IOException, JsonGenerationException, JsonMappingException {
      System.out.println("Project: " + project);
      final Classification oldMergedData = getOldData(goalFile.getParentFile(), project);
      
      final File lastOldFile = moveToUnnamed(goalFile);
      manualTemplate.merge(oldMergedData);
      
      FolderSearcher.MAPPER.writeValue(goalFile, manualTemplate);
      
      if (lastOldFile != null) {
         if (FileUtils.contentEquals(lastOldFile, goalFile)) {
            lastOldFile.delete();
         }
      }
   }

   public static Classification readCurrentData(final ProjectChanges changes, final File goalFile) throws IOException, JsonGenerationException, JsonMappingException {
      final Classification manualTemplate = new Classification();
      
      changes.executeProcessor((final String version, final String testcase, final Change change) -> {
         final ChangedEntity testEntity = new ChangedEntity(testcase, "", change.getMethod());
         final String direction = (change.getChangePercent() > 0) ? "FASTER" : "SLOWER";
         manualTemplate.addChange(version, testEntity, new HashSet<>(), direction);
      });
      
      FolderSearcher.MAPPER.writeValue(new File(goalFile.getParent(), "temp.json"), manualTemplate);
      return manualTemplate;
   }

   

   public static Classification readChangesFromProperties(final VersionChangeProperties properties) throws IOException, JsonParseException, JsonMappingException {
      final Classification manualTemplate = new Classification();
      
      properties.executeProcessor((version, test, testcaseProperties, versionProperties) -> {
         final ChangedEntity testEntity = new ChangedEntity(test, "", testcaseProperties.getMethod());
         final Set<String> guessed = new TreeSet<>(testcaseProperties.getGuessedTypes());
         final String direction = (testcaseProperties.getChangePercent() > 0) ? "FASTER" : "SLOWER";
         final TestcaseClass classification = manualTemplate.addChange(version, testEntity, guessed, direction);
         if (!testcaseProperties.isAffectsSource()) {
            classification.setFunctionalChange(false);
         }
      });
      return manualTemplate;
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
