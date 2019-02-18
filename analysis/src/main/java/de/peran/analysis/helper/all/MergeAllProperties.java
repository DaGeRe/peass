package de.peran.analysis.helper.all;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.changes.Change;
import de.peass.analysis.changes.Changes;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.analysis.properties.ChangeProperties;
import de.peass.analysis.properties.ChangeProperty;
import de.peass.analysis.properties.VersionChangeProperties;
import de.peran.FolderSearcher;

public class MergeAllProperties {
   private static final Logger LOG = LogManager.getLogger(MergeAllProperties.class);

   public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
      File propertiesFolder = new File("/home/reichelt/daten3/diss/properties/v2");
      File resultFolder = new File("/home/reichelt/daten/diss/ergebnisse/normaltest/v26_symbolicComplete", "results");

      for (String project : new String[] { "commons-compress", "commons-csv", "commons-dbcp", "commons-fileupload", "commons-imaging", "commons-io",
            "commons-text" }) {
         LOG.debug("Projekt: {}", project);
         File projectFile = new File(propertiesFolder, project);
         File propertyFile = new File(projectFile, "properties_alltests.json");
         VersionChangeProperties oldProperties = FolderSearcher.MAPPER.readValue(propertyFile, VersionChangeProperties.class);
         File changeFile = new File(resultFolder, project + "/clean.json");
         ProjectChanges knowledge = FolderSearcher.MAPPER.readValue(changeFile, ProjectChanges.class);
         VersionChangeProperties newProperties = new VersionChangeProperties();
         for (Map.Entry<String, Changes> entry : knowledge.getVersionChanges().entrySet()) {
            ChangeProperties oldVersionProperties = oldProperties.getVersions().get(entry.getKey());
            if (oldVersionProperties != null) {
               ChangeProperties changeProperties = new ChangeProperties();
               changeProperties.setCommitter(oldVersionProperties.getCommitter());
               changeProperties.setCommitText(oldVersionProperties.getCommitText());
               newProperties.getVersions().put(entry.getKey(), changeProperties);
               enrichVersion(changeProperties, entry, oldVersionProperties);
            }
         }
         File resultFile = new File(resultFolder, project + "/properties.json");
         FolderSearcher.MAPPER.writeValue(resultFile, newProperties);
      }
   }

   private static void enrichVersion(ChangeProperties changeProperties, Map.Entry<String, Changes> entry, ChangeProperties oldVersionProperties) {
      for (Map.Entry<String, List<Change>> changes : entry.getValue().getTestcaseChanges().entrySet()) {
         List<ChangeProperty> oldProperties = oldVersionProperties.getProperties().get(changes.getKey());
         if (oldProperties != null) {
            List<ChangeProperty> testProperties = new LinkedList<>();
            changeProperties.getProperties().put(changes.getKey(), testProperties);
            for (Change change : changes.getValue()) {
//               System.out.println("Suche: " + entry.getKey() + " " + changes.getKey() + " " + change.getMethod());
//               System.out.println(oldProperties);
               Optional<ChangeProperty> potentialProperty = oldProperties.stream().filter(prop -> prop.getMethod().equals(change.getMethod())).findFirst();
               if (potentialProperty.isPresent()) {
                  ChangeProperty oldProperty = potentialProperty.get();
                  oldProperty.setChangePercent(change.getChangePercent());
                  oldProperty.setOldTime(change.getOldTime());
                  testProperties.add(oldProperty);
                  if (oldProperty.getDiff() == null) {
                     oldProperty.setDiff(change.getDiff());
                  }
               }
            }
         }
      }
   }
}
