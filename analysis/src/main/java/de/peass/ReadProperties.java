package de.peass;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.all.ReadAllProperties;
import de.peass.analysis.all.RepoFolders;
import de.peass.analysis.changes.Change;
import de.peass.analysis.changes.Changes;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.analysis.properties.ChangeProperties;
import de.peass.analysis.properties.ChangeProperty;
import de.peass.analysis.properties.PropertyReadHelper;
import de.peass.analysis.properties.VersionChangeProperties;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.vcs.GitUtils;
import de.peran.FolderSearcher;
import de.peran.analysis.helper.AnalysisUtil;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Reads a changes.json which contains all changes that happened in a project and determines properties of the changes.
 * 
 * @author reichelt
 *
 */
@Command(description = "Reads the properties of given changes", name = "readproperties")
public class ReadProperties implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(ReadProperties.class);

   @Option(names = { "-dependencyfile", "--dependencyfile" }, description = "Path to the dependencyfile")
   protected File dependencyFile;

   @Option(names = { "-executionfile", "--executionfile" }, description = "Path to the executionfile")
   protected File executionFile;

   @Option(names = { "-folder", "--folder" }, description = "Folder of the project that should be analyzed", required = true)
   protected File projectFolder;

   @Option(names = { "-out", "--out" }, description = "Path for saving the propertyfile")
   protected File out;

   public ReadProperties() {

   }

   public ReadProperties(final File resultFile) {
      out = resultFile;
   }

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException, JAXBException {
      final CommandLine commandLine = new CommandLine(new ReadProperties());
      commandLine.execute(args);
   }

   @Override
   public Void call() throws Exception {
      final RepoFolders folders = new RepoFolders();
      final String projectName = projectFolder.getName();
      GetChanges.getVersionOrder(dependencyFile, executionFile, folders.getDependencyFile(projectName));
      
      if (!projectFolder.exists()) {
         GitUtils.downloadProject(VersionComparator.getDependencies().getUrl(), projectFolder);
      }

      final File viewFolder = folders.getViewFolder(projectName);
      final ExecutionData changedTests = folders.getExecutionData(projectName);
      if (ReadAllProperties.readAll) {
         final File resultFile = new File("results" + File.separator + projectName + File.separator + "properties_alltests.json");
         out = resultFile;
         readAllTestsProperties(projectFolder, viewFolder, changedTests);
      } else {
         final File changefile = folders.getChangeFile(projectName);
         // final File changefile = new File(commandLine.getOptionValue(OptionConstants.CHANGEFILE.getName()));

         if (out == null) {
            out = folders.getProjectPropertyFile(projectName);
            if (!out.getParentFile().exists()) {
               out.getParentFile().mkdirs();
            }
         }

         if (!changefile.exists()) {
            LOG.error("Changefile {} needs to exist.", changefile);
            System.exit(1);
         }

         if (!viewFolder.exists()) {
            LOG.error("ViewFolder {} needs to exist.", viewFolder);
            System.exit(1);
         }

         readChangeProperties(changefile, projectFolder, viewFolder, changedTests);
      }
      return null;
   }

   public void readAllTestsProperties(final File projectFolder, final File viewFolder, final ExecutionData changedTests) throws IOException {
      final VersionChangeProperties versionProperties = new VersionChangeProperties();
      int count = 0;
      final File methodFolder = new File(out.getParentFile(), "methods");
      methodFolder.mkdirs();
      for (final Map.Entry<String, TestSet> version : changedTests.getVersions().entrySet()) {
         // String prevVersion = VersionComparator.getPreviousVersion(version.getKey());
         final ChangeProperties changeProperties = new ChangeProperties();
         changeProperties.setCommitText(GitUtils.getCommitText(projectFolder, version.getKey()));
         changeProperties.setCommitter(GitUtils.getCommitter(projectFolder, version.getKey()));
         versionProperties.getVersions().put(version.getKey(), changeProperties);
         for (final Entry<ChangedEntity, Set<String>> testclazz : version.getValue().getTestcases().entrySet()) {
            final List<ChangeProperty> properties = new LinkedList<>();
            changeProperties.getProperties().put(testclazz.getKey().getJavaClazzName(), properties);
            for (final String testmethod : testclazz.getValue()) {
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
         FolderSearcher.MAPPER.writeValue(out, versionProperties);
      }

      System.out.println("Analyzed: " + count);
   }

   public void readChangeProperties(final File changefile, final File projectFolder, final File viewFolder, final ExecutionData changedTests)
         throws IOException, JsonParseException, JsonMappingException, JsonGenerationException {
      final File resultCSV = new File(out.getParentFile(), projectFolder.getName() + ".csv");

      try (BufferedWriter csvWriter = new BufferedWriter(new FileWriter(resultCSV))) {
         writeCSVHeadline(csvWriter);
         final VersionChangeProperties versionProperties = new VersionChangeProperties();

         final ProjectChanges changes = FolderSearcher.MAPPER.readValue(changefile, ProjectChanges.class);

         int versionCount = 0, testcaseCount = 0;
         for (final Entry<String, Changes> versionChanges : changes.getVersionChanges().entrySet()) {
            final String version = versionChanges.getKey();
            final TestSet tests = changedTests.getVersions().get(version);
            //
            final String predecessor = tests != null ? tests.getPredecessor() : version + "~1";
            testcaseCount += detectVersionProperty(projectFolder, viewFolder, csvWriter, versionProperties, versionChanges, predecessor);
            if (tests == null) {
               LOG.error("Version not contained in runfile: " + version);
            }
            versionCount++;
         }
         // writeOnlySource(versionProperties, ProjectChanges.getOldChanges());
         System.out.println("Analyzed: " + testcaseCount + " testcases in " + versionCount + " versions");
      }
   }

   private int detectVersionProperty(final File projectFolder, final File viewFolder, final BufferedWriter csvWriter,
         final VersionChangeProperties versionProperties,
         final Entry<String, Changes> versionChanges, final String predecessor) throws IOException, JsonGenerationException, JsonMappingException {
      final File methodFolder = new File(out.getParentFile(), "methods");
      methodFolder.mkdirs();
      final String version = versionChanges.getKey();
      final ChangeProperties changeProperties = new ChangeProperties();
      changeProperties.setCommitText(GitUtils.getCommitText(projectFolder, version));
      changeProperties.setCommitter(GitUtils.getCommitter(projectFolder, version));
      versionProperties.getVersions().put(version, changeProperties);
      int count = 0;
      for (final Entry<String, List<Change>> changes : versionChanges.getValue().getTestcaseChanges().entrySet()) {
         final String testclazz = changes.getKey();
         final List<ChangeProperty> properties = new LinkedList<>();
         changeProperties.getProperties().put(testclazz, properties);
         for (final Change testcaseChange : changes.getValue()) {
            final PropertyReadHelper reader = new PropertyReadHelper(version, predecessor, new ChangedEntity(testclazz, ""), testcaseChange, projectFolder, viewFolder, methodFolder);
            final ChangeProperty currentProperty = reader.read();
            // if (currentProperty != null) {
            properties.add(currentProperty);
            FolderSearcher.MAPPER.writeValue(out, versionProperties);
            writeCSVLine(csvWriter, currentProperty, projectFolder.getName());
            // }

            count++;
         }
      }
      return count;
   }

   public static VersionChangeProperties readVersionProperties(final ProjectChanges knowledge, final File versionFile) {
      final VersionChangeProperties versionProperties = new VersionChangeProperties();
      try {
         final VersionChangeProperties allProperties = FolderSearcher.MAPPER.readValue(versionFile, VersionChangeProperties.class);
         for (final Entry<String, Changes> versionChanges : knowledge.getVersionChanges().entrySet()) {
            final String version = versionChanges.getKey();
            final ChangeProperties allProps = allProperties.getVersions().get(version);
            if (allProps != null) {
               final ChangeProperties changeProperties = new ChangeProperties();
               changeProperties.setCommitText(allProps.getCommitText());
               changeProperties.setCommitter(allProps.getCommitText());
               versionProperties.getVersions().put(version, changeProperties);
               for (final Entry<String, List<Change>> changes : versionChanges.getValue().getTestcaseChanges().entrySet()) {
                  final String testclazz = changes.getKey();
                  final List<ChangeProperty> properties = new LinkedList<>();
                  final List<ChangeProperty> oldTestcaseProperties = allProps.getProperties().get(changes.getKey());
                  if (oldTestcaseProperties != null) {
                     changeProperties.getProperties().put(testclazz, properties);
                     for (final Change change : changes.getValue()) {
                        for (final ChangeProperty prop : oldTestcaseProperties) {
                           if (prop.getMethod().equals(change.getMethod())) {
                              properties.add(prop);
                           }
                        }
                     }
                  }
               }
            }
         }

      } catch (final IOException e) {
         e.printStackTrace();
      }
      return versionProperties;
   }

   public static void writeCSVHeadline(final BufferedWriter csvWriter) throws IOException {
      String line = "#Diff" + ";" + "project;" + "method" + ";" + "intensity" + ";" + "testChange;" + "traceChange" + ";";
      line += "calls" + ";" + "callsOld" + ";" + "oldTime" + ";";
      for (final String keyword : PropertyReadHelper.keywords) {
         line += keyword + "Added;" + keyword + "Removed;";
      }
      csvWriter.write(line.substring(0, line.length() - 1) + "\n");
      csvWriter.flush();

      String line2 = "CREATE TABLE properties(Diff VARCHAR(250),  method VARCHAR(250),  intensity DOUBLE, testChange VARCHAR(50), traceChange VARCHAR(50), ";
      line2 += "calls INT, callsOld INT, oldTime INT,";
      for (final String keyword : PropertyReadHelper.keywords) {
         line2 += keyword.replace('.', '_') + "Added INT," + keyword.replace('.', '_') + "Removed INT,";
      }
      System.out.println(line2.substring(0, line2.length() - 1) + ")");
   }

   private static void writeCSVLine(final BufferedWriter csvWriter, final ChangeProperty currentProperty, final String project) throws IOException {
      String testChange;
      if (currentProperty.isAffectsSource() && currentProperty.isAffectsTestSource()) {
         testChange = "AFFECTSBOTH";
      } else if (currentProperty.isAffectsSource()) {
         testChange = "AFFECTSSOURCE";
      } else if (currentProperty.isAffectsTestSource()) {
         testChange = "AFFECTSTEST";
      } else {
         testChange = "NONE";
      }

      String line = currentProperty.getDiff() + ";" + project + ";" + currentProperty.getMethod() + ";" + currentProperty.getChangePercent() + ";"
            + testChange + ";"
            + currentProperty.getTraceChangeType() + ";";
      line += currentProperty.getCalls() + ";" + currentProperty.getCallsOld() + ";" + currentProperty.getOldTime() + ";";
      for (final String keyword : PropertyReadHelper.keywords) {
         Integer value = currentProperty.getAddedMap().get(keyword);
         if (value == null)
            value = 0;
         line += value + ";";
         Integer valueRemove = currentProperty.getRemovedMap().get(keyword);
         if (valueRemove == null)
            valueRemove = 0;
         line += valueRemove + ";";
      }
      csvWriter.write(line.substring(0, line.length() - 1) + "\n");
      csvWriter.flush();
   }

   public static void writeOnlySource(final VersionChangeProperties versionProperties, final ProjectChanges oldKnowledge)
         throws IOException, JsonGenerationException, JsonMappingException {
      final File resultOnlysource = new File(AnalysisUtil.getProjectResultFolder(), "properties_onlysource.json");
      final File changesOnlysource = new File(AnalysisUtil.getProjectResultFolder(), "changes_onlysource.json");
      final VersionChangeProperties propertiesOnlySource = new VersionChangeProperties();
      final ProjectChanges knowledgeOnlySource = new ProjectChanges();
      int testcaseCount = 0;
      for (final Entry<String, ChangeProperties> version : versionProperties.getVersions().entrySet()) {
         final Map<String, List<ChangeProperty>> testcases = new LinkedHashMap<>();
         for (final Entry<String, List<ChangeProperty>> testcase : version.getValue().getProperties().entrySet()) {
            final List<ChangeProperty> newList = new LinkedList<>();
            for (final ChangeProperty property : testcase.getValue()) {
               if (property.isAffectsSource() && !property.isAffectsTestSource()) {
                  newList.add(property);

                  final TestCase test = new TestCase(testcase.getKey(), property.getMethod());
                  final Changes oldVersion = oldKnowledge.getVersion(version.getKey());
                  final Change change = oldVersion.getChange(test);
                  if (change == null) {
                     LOG.error(test); // TODO throw exception here and debug
                  } else {
                     knowledgeOnlySource.addChange(test, version.getKey(), change);
                     testcaseCount++;
                  }

               }
            }
            if (newList.size() > 0) {
               testcases.put(testcase.getKey(), newList);
            }
         }
         if (testcases.size() > 0) {
            final ChangeProperties properties = new ChangeProperties();
            properties.setProperties(testcases);
            properties.setCommitter(version.getValue().getCommitter());
            properties.setCommitText(version.getValue().getCommitText());
            propertiesOnlySource.getVersions().put(version.getKey(), properties);
         }
      }
      knowledgeOnlySource.setVersionCount(knowledgeOnlySource.getVersionChanges().size());
      knowledgeOnlySource.setTestcaseCount(testcaseCount);
      FolderSearcher.MAPPER.writeValue(resultOnlysource, propertiesOnlySource);
      FolderSearcher.MAPPER.writeValue(changesOnlysource, knowledgeOnlySource);
   }

}
