package de.dagere.peass.dependency.reader;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.github.javaparser.ParseException;

import de.dagere.peass.dependency.DependencyManager;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.ResultsFolders;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.execution.ProjectModules;
import de.dagere.peass.dependency.traces.KiekerFolderUtil;
import de.dagere.peass.dependency.traces.OneTraceGenerator;
import de.dagere.peass.dependency.traces.TraceFileMapping;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.vcs.GitUtils;

public class TraceViewGenerator {
   
   private static final Logger LOG = LogManager.getLogger(TraceViewGenerator.class);
   
   private final DependencyManager dependencyManager;
   private final PeASSFolders folders;
   private final String version;
   private final TraceFileMapping traceFileMapping;
   
   public TraceViewGenerator(final DependencyManager dependencyManager, final PeASSFolders folders, final String version, final TraceFileMapping mapping) {
      this.dependencyManager = dependencyManager;
      this.folders = folders;
      this.version = version;
      this.traceFileMapping = mapping;
   }

   public boolean generateViews(final ResultsFolders resultsFolders, final TestSet examinedTests) throws IOException, XmlPullParserException, ParseException, ViewNotFoundException, InterruptedException {
      LOG.debug("Generating views for {}", version);
      boolean allWorked = true;
      GitUtils.reset(folders.getProjectFolder());
      ProjectModules modules = dependencyManager.getExecutor().getModules();
      ModuleClassMapping mapping = new ModuleClassMapping(folders.getProjectFolder(), modules);
      List<File> classpathFolders = getClasspathFolders(modules);
      for (TestCase testcase : examinedTests.getTests()) {
         final File moduleFolder = KiekerFolderUtil.getModuleResultFolder(folders, testcase);
         final OneTraceGenerator oneViewGenerator = new OneTraceGenerator(resultsFolders, folders, testcase, traceFileMapping, version, moduleFolder,
               classpathFolders, mapping);
          final boolean workedLocal = oneViewGenerator.generateTrace(version);
          allWorked &= workedLocal;
      }
      return allWorked;
   }
   
   private List<File> getClasspathFolders(final ProjectModules modules) {
      final List<File> files = new LinkedList<>();
      for (int i = 0; i < modules.getModules().size(); i++) {
         final File module = modules.getModules().get(i);
         for (int folderIndex = 0; folderIndex < ChangedEntity.potentialClassFolders.length; folderIndex++) {
            final String path = ChangedEntity.potentialClassFolders[folderIndex];
            files.add(new File(module, path));
         }
      }
      return files;
   }
}
