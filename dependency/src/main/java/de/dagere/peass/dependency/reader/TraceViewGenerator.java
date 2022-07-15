package de.dagere.peass.dependency.reader;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ParseException;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.dependency.DependencyManager;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.traces.OneTraceGenerator;
import de.dagere.peass.dependency.traces.TraceFileMapping;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.vcs.GitUtils;

public class TraceViewGenerator {

   private static final Logger LOG = LogManager.getLogger(TraceViewGenerator.class);

   private final DependencyManager dependencyManager;
   private final PeassFolders folders;
   private final String version;
   private final TraceFileMapping traceFileMapping;
   
   private final KiekerConfig kiekerConfig;
   private final TestSelectionConfig testSelectionConfig;

   public TraceViewGenerator(final DependencyManager dependencyManager, final PeassFolders folders, final String version, final TraceFileMapping mapping, 
         final KiekerConfig kiekerConfig, TestSelectionConfig testSelectionConfig) {
      this.dependencyManager = dependencyManager;
      this.folders = folders;
      this.version = version;
      this.traceFileMapping = mapping;
      this.kiekerConfig = kiekerConfig;
      this.testSelectionConfig = testSelectionConfig;
   }

   public void generateViews(final ResultsFolders resultsFolders, final TestSet examinedTests)
         throws IOException, ParseException {
      LOG.debug("Generating views for {}", version);
      GitUtils.reset(folders.getProjectFolder());
      ProjectModules modules = dependencyManager.getExecutor().getModules();
      ModuleClassMapping mapping = new ModuleClassMapping(dependencyManager.getExecutor());
      List<File> classpathFolders = getClasspathFolders(modules);
      for (TestCase testcase : examinedTests.getTests()) {
         final OneTraceGenerator oneViewGenerator = new OneTraceGenerator(resultsFolders, folders, testcase, traceFileMapping, version, classpathFolders, mapping, kiekerConfig, testSelectionConfig);
         oneViewGenerator.generateTrace(version);
      }
   }

   private List<File> getClasspathFolders(final ProjectModules modules) {
      ExecutionConfig executionConfig = dependencyManager.getTestTransformer().getConfig().getExecutionConfig();
      final List<File> files = new LinkedList<>();
      for (int i = 0; i < modules.getModules().size(); i++) {
         final File module = modules.getModules().get(i);
         for (String clazzPath : executionConfig.getClazzFolders()) {
            files.add(new File(module, clazzPath));
         }
         for (String testClazzPath : executionConfig.getTestClazzFolders()) {
            files.add(new File(module, testClazzPath));
         }
      }
      return files;
   }
}
