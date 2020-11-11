package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.properties.PropertyReader;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependency.persistence.Version;
import de.peass.dependency.traces.ViewGenerator;
import de.peass.utils.Constants;
import de.peass.vcs.GitCommit;

public class TestChooser {
   
   private static final Logger LOG = LogManager.getLogger(TestChooser.class);
   
   private final boolean useViews;
   File localFolder;
   PeASSFolders folders;
   private GitCommit version;
   private final File viewFolder;
   private final File propertyFolder;
   private final int threads;
   private final List<String> includes;
   
   public TestChooser(boolean useViews, File localFolder, PeASSFolders folders, GitCommit version, File viewFolder, File propertyFolder, int threads, List<String> includes) {
      this.useViews = useViews;
      this.localFolder = localFolder;
      this.folders = folders;
      this.version = version;
      this.viewFolder = viewFolder;
      this.propertyFolder = propertyFolder;
      this.threads = threads;
      this.includes = includes;
   }

   public Set<TestCase> getTestSet(final Dependencies dependencies) throws IOException, JAXBException, JsonParseException, JsonMappingException {
      final String versionName = dependencies.getVersionNames()[dependencies.getVersions().size() - 1];
      final Version currentVersion = dependencies.getVersions().get(versionName);
      
      final Set<TestCase> tests = new HashSet<>();

      if (useViews) {
         final TestSet traceTestSet = getViewTests(dependencies);
         for (final Map.Entry<ChangedEntity, Set<String>> test : traceTestSet.getTestcases().entrySet()) {
            for (final String method : test.getValue()) {
               tests.add(new TestCase(test.getKey().getClazz(), method));
            }
         }
      } else {
         for (final TestSet dep : currentVersion.getChangedClazzes().values()) {
            tests.addAll(dep.getTests());
         }
      }
      
      if (includes.size() > 0) {
         removeNotIncluded(tests);
      }
      return tests;
   }

   private void removeNotIncluded(final Set<TestCase> tests) {
      for (Iterator<TestCase> it = tests.iterator(); it.hasNext(); ) {
         TestCase test = it.next();
         boolean isIncluded = false;
         for (String include : includes) {
            boolean match = FilenameUtils.wildcardMatch(test.getExecutable(), include);
            LOG.info("Testing {} {} {}", test.getExecutable(), include, match);
            if (match) {
               isIncluded = true;
               break;
            }
         }
         if (!isIncluded) {
            LOG.info("Excluding non-included test {}", test);
            it.remove();
         }
      }
   }
   
   private TestSet getViewTests(final Dependencies dependencies)
         throws IOException, JAXBException, JsonParseException, JsonMappingException {
      final File executeFile = new File(localFolder, "execute.json");

      FileUtils.deleteDirectory(folders.getTempMeasurementFolder());
      if (!executeFile.exists()) {
         LOG.debug("Expected file {} does not exist, executing view creation", executeFile);
         final ViewGenerator viewgenerator = new ViewGenerator(folders.getProjectFolder(), dependencies, executeFile, viewFolder, threads, 15);
         viewgenerator.processCommandline();
         final PropertyReader propertyReader = new PropertyReader(propertyFolder, folders.getProjectFolder(), viewFolder);
         propertyReader.readAllTestsProperties(viewgenerator.getChangedTraceMethods());
      }
      final ExecutionData traceTests = Constants.OBJECTMAPPER.readValue(executeFile, ExecutionData.class);
      LOG.debug("Version: {} Path: {}", version, executeFile.getAbsolutePath());
      final TestSet traceTestSet = traceTests.getVersions().get(version.getTag());

      return traceTestSet;
   }
}
