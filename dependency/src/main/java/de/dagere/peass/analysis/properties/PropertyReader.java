package de.dagere.peass.analysis.properties;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.GitUtils;

public class PropertyReader {

   private static final Logger LOG = LogManager.getLogger(PropertyReader.class);

   private final ResultsFolders resultsFolders;
   private final File projectFolder;
   private final ExecutionData changedTests;
   private int count = 0;
   private final ExecutionConfig config;

   public PropertyReader(final ResultsFolders resultsFolders, final File projectFolder, final ExecutionData changedTests, final ExecutionConfig config) {
      this.resultsFolders = resultsFolders;
      this.projectFolder = projectFolder;
      this.changedTests = changedTests;
      this.config = config;
   }

   public void readAllTestsProperties() {
      try {
         final VersionChangeProperties versionProperties = new VersionChangeProperties();
         final File methodFolder = new File(resultsFolders.getPropertiesFolder(), "methods");
         methodFolder.mkdirs();
         for (final Map.Entry<String, TestSet> version : changedTests.getVersions().entrySet()) {
            readVersion(versionProperties, methodFolder, version);
            Constants.OBJECTMAPPER.writeValue(resultsFolders.getPropertiesFile(), versionProperties);
         }

         LOG.info("Analyzed properties: " + count);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private void readVersion(final VersionChangeProperties versionProperties, final File methodFolder, final Map.Entry<String, TestSet> version) throws IOException {
      // String prevVersion = VersionComparator.getPreviousVersion(version.getKey());
      LOG.debug("Reading {}", version.getKey());
      final ChangeProperties changeProperties = new ChangeProperties();
      changeProperties.setCommitText(GitUtils.getCommitText(projectFolder, version.getKey()));
      changeProperties.setCommitter(GitUtils.getCommitter(projectFolder, version.getKey()));
      versionProperties.getVersions().put(version.getKey(), changeProperties);
      for (final Entry<TestCase, Set<String>> testclazz : version.getValue().getTestcases().entrySet()) {
         final List<ChangeProperty> properties = new LinkedList<>();
         changeProperties.getProperties().put(testclazz.getKey().getClazz(), properties);
         for (final String testmethod : testclazz.getValue()) {
            readMethod(methodFolder, version, testclazz, properties, testmethod);
         }
      }
   }

   private void readMethod(final File methodSourceFolder, final Map.Entry<String, TestSet> version, final Entry<TestCase, Set<String>> testclazz,
         final List<ChangeProperty> properties, final String testmethod) throws IOException {
      final Change testcaseChange = new Change();
      testcaseChange.setMethod(testmethod);

      final ChangedEntity entity = new ChangedEntity(testclazz.getKey().getClazz(), testclazz.getKey().getModule());
      // TODO eventually, we need to set change the version of the config here to  version.getKey(), version.getValue().getPredecessor(),
      ExecutionConfig copyConfig = new ExecutionConfig(config);
      copyConfig.setCommit(version.getKey());
      copyConfig.setCommitOld(version.getValue().getPredecessor());
      final PropertyReadHelper reader = new PropertyReadHelper(copyConfig,
            entity, testcaseChange,
            projectFolder,
            resultsFolders.getViewFolder(), methodSourceFolder, changedTests);
      final ChangeProperty currentProperty = reader.read();
      if (currentProperty != null) {
         properties.add(currentProperty);
      }

      count++;
   }

}
