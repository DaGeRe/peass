package de.peass.dependency.traces;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.CalledMethodLoader;
import de.peass.dependency.analysis.ModuleClassMapping;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TraceElement;
import de.peass.dependency.execution.ProjectModules;
import de.peass.dependencyprocessors.ViewNotFoundException;

public class OneTraceGenerator {

   private static final int MAX_SIZE_MB = 100;
   static final String METHOD = "_method";
   static final String METHOD_EXPANDED = "_method_expanded";
   public static final String NOCOMMENT = "_nocomment";

   private static final Logger LOG = LogManager.getLogger(OneTraceGenerator.class);

   private final File viewFolder;
   private final PeASSFolders folders;
   private final TestCase testcase;
   private final Map<String, List<File>> traceFileMap;
   private final String version;
   private final File resultsFolder;
   private final ProjectModules modules;

   public OneTraceGenerator(final File viewFolder, final PeASSFolders folders, final TestCase testcase, final Map<String, List<File>> traceFileMap, final String version,
         final File resultsFolder, final ProjectModules modules) {
      super();
      this.viewFolder = viewFolder;
      this.folders = folders;
      this.testcase = testcase;
      this.traceFileMap = traceFileMap;
      this.version = version;
      this.resultsFolder = resultsFolder;
      this.modules = modules;
   }

   public boolean generateTrace(final String versionCurrent)
         throws com.github.javaparser.ParseException, IOException, ViewNotFoundException, XmlPullParserException {
      boolean success = false;
      try {
         final File methodResult = KiekerFolderUtil.getClazzMethodFolder(testcase, resultsFolder);
         LOG.debug("Searching for: {}", methodResult);
         if (methodResult.exists() && methodResult.isDirectory()) {
            success = generateTraceFiles(versionCurrent, methodResult);
         } else {
            LOG.error("Error: {} does not produce {}", versionCurrent, methodResult.getAbsolutePath());
         }
      } catch (final RuntimeException | ViewNotFoundException e) {
         e.printStackTrace();
      }
      return success;
   }

   private boolean generateTraceFiles(final String versionCurrent, final File kiekerResultFolder)
         throws FileNotFoundException, IOException, XmlPullParserException, com.github.javaparser.ParseException {
      boolean success = false;
      final long size = FileUtils.sizeOfDirectory(kiekerResultFolder);
      final long sizeInMB = size / (1024 * 1024);
      LOG.debug("Filesize: {} ({})", sizeInMB, size);
      if (sizeInMB < MAX_SIZE_MB) {
         final ModuleClassMapping mapping = new ModuleClassMapping(folders.getProjectFolder(), modules);
         final List<TraceElement> shortTrace = new CalledMethodLoader(kiekerResultFolder, mapping).getShortTrace("");
         if (shortTrace != null) {
            LOG.debug("Short Trace: {} Folder: {} Project: {}", shortTrace.size(), kiekerResultFolder.getAbsolutePath(), folders.getProjectFolder());
            final List<File> files = getClasspathFolders();
            if (shortTrace.size() > 0) {
               final TraceMethodReader traceMethodReader = new TraceMethodReader(shortTrace, files.toArray(new File[0]));
               final TraceWithMethods trace = traceMethodReader.getTraceWithMethods();
               List<File> traceFiles = traceFileMap.get(testcase.toString());
               if (traceFiles == null) {
                  traceFiles = new LinkedList<>();
                  traceFileMap.put(testcase.toString(), traceFiles);
               }
               TraceWriter traceWriter = new TraceWriter(version, testcase, viewFolder);
               traceWriter.writeTrace(versionCurrent, sizeInMB, traceMethodReader, trace, traceFiles);
               success = true;
            } else {
               LOG.error("Trace empty!");
            }
         }
      } else {
         LOG.error("File size exceeds 2000 MB");
      }
      return success;
   }

   

   

   private List<File> getClasspathFolders() {
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
