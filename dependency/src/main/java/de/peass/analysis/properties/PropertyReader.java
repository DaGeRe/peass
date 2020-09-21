package de.peass.analysis.properties;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.peass.analysis.changes.Change;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.utils.Constants;
import de.peass.vcs.GitUtils;

public class PropertyReader {
   
   private final File outFolder, propertyFile;
   private final File projectFolder, viewFolder;
   int count = 0;
   
   public PropertyReader(final File outFolder, final File projectFolder, final File viewFolder) {
      super();
      this.outFolder = outFolder;
      this.projectFolder = projectFolder;
      this.viewFolder = viewFolder;
      this.propertyFile = new File(outFolder, "properties.json");
   }

   public void readAllTestsProperties(final ExecutionData changedTests) throws IOException {
      final VersionChangeProperties versionProperties = new VersionChangeProperties();
      
      final File methodFolder = new File(outFolder, "methods");
      methodFolder.mkdirs();
      for (final Map.Entry<String, TestSet> version : changedTests.getVersions().entrySet()) {
         readVersion(versionProperties, methodFolder, version);
         Constants.OBJECTMAPPER.writeValue(propertyFile, versionProperties);
      }

      System.out.println("Analyzed: " + count);
   }

   private void readVersion(final VersionChangeProperties versionProperties, final File methodFolder, final Map.Entry<String, TestSet> version) throws IOException {
      // String prevVersion = VersionComparator.getPreviousVersion(version.getKey());
      final ChangeProperties changeProperties = new ChangeProperties();
      changeProperties.setCommitText(GitUtils.getCommitText(projectFolder, version.getKey()));
      changeProperties.setCommitter(GitUtils.getCommitter(projectFolder, version.getKey()));
      versionProperties.getVersions().put(version.getKey(), changeProperties);
      for (final Entry<ChangedEntity, Set<String>> testclazz : version.getValue().getTestcases().entrySet()) {
         final List<ChangeProperty> properties = new LinkedList<>();
         changeProperties.getProperties().put(testclazz.getKey().getJavaClazzName(), properties);
         for (final String testmethod : testclazz.getValue()) {
            readMethod(methodFolder, version, testclazz, properties, testmethod);
         }
      }
   }

   private void readMethod(final File methodFolder, final Map.Entry<String, TestSet> version, final Entry<ChangedEntity, Set<String>> testclazz,
         final List<ChangeProperty> properties, final String testmethod) throws IOException {
      final Change testcaseChange = new Change();
      testcaseChange.setMethod(testmethod);
      
      final ChangedEntity entity = new ChangedEntity(testclazz.getKey().getClazz(), testclazz.getKey().getModule());
      final PropertyReadHelper reader = new PropertyReadHelper(version.getKey(), version.getValue().getPredecessor(), 
            entity, testcaseChange, 
            projectFolder,
            viewFolder, methodFolder);
      final ChangeProperty currentProperty = reader.read();
      if (currentProperty != null) {
         properties.add(currentProperty);
      }

      count++;
   }

}
