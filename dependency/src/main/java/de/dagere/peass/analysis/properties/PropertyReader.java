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

import de.dagere.nodeDiffGenerator.data.MethodCall;
import de.dagere.nodeDiffGenerator.data.TestClazzCall;
import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.FixedCommitConfig;
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
         final CommitChangeProperties commitProperties = new CommitChangeProperties();
         final File methodFolder = new File(resultsFolders.getPropertiesFolder(), "methods");
         methodFolder.mkdirs();
         for (final Map.Entry<String, TestSet> commit : changedTests.getCommits().entrySet()) {
            readCommit(commitProperties, methodFolder, commit);
            Constants.OBJECTMAPPER.writeValue(resultsFolders.getPropertiesFile(), commitProperties);
         }

         LOG.info("Analyzed properties: " + count);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private void readCommit(final CommitChangeProperties commitProperties, final File methodFolder, final Map.Entry<String, TestSet> commit) throws IOException {
      LOG.debug("Reading {}", commit.getKey());
      final ChangeProperties changeProperties = new ChangeProperties();
      changeProperties.setCommitText(GitUtils.getCommitText(projectFolder, commit.getKey()));
      changeProperties.setCommitter(GitUtils.getCommitter(projectFolder, commit.getKey()));
      commitProperties.getVersions().put(commit.getKey(), changeProperties);
      for (final Entry<TestClazzCall, Set<String>> testclazz : commit.getValue().getTestcases().entrySet()) {
         final List<ChangeProperty> properties = new LinkedList<>();
         changeProperties.getProperties().put(testclazz.getKey().getClazz(), properties);
         for (final String testmethod : testclazz.getValue()) {
            readMethod(methodFolder, commit, testclazz, properties, testmethod);
         }
      }
   }

   private void readMethod(final File methodSourceFolder, final Map.Entry<String, TestSet> commit, final Entry<TestClazzCall, Set<String>> testclazz,
         final List<ChangeProperty> properties, final String testmethod) throws IOException {
      final Change testcaseChange = new Change();
      testcaseChange.setMethod(testmethod);

      final MethodCall entity = new MethodCall(testclazz.getKey().getClazz(), testclazz.getKey().getModule());
      // TODO eventually, we need to set change the version of the config here to  version.getKey(), version.getValue().getPredecessor(),
      FixedCommitConfig copyConfig = new FixedCommitConfig();
      copyConfig.setCommit(commit.getKey());
      copyConfig.setCommitOld(commit.getValue().getPredecessor());
      final PropertyReadHelper reader = new PropertyReadHelper(config, copyConfig,
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
