package de.peran.dependency.traces;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.peran.DependencyReadingStarter;
import de.peran.ViewPrintStarter;
import de.peran.analysis.knowledge.Changes;
import de.peran.analysis.knowledge.VersionKnowledge;
import de.peran.dependency.TestResultManager;
import de.peran.dependency.analysis.CalledMethodLoader;
import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependency.analysis.data.TestSet;
import de.peran.dependency.analysis.data.TraceElement;
import de.peran.dependency.execution.MavenPomUtil;
import de.peran.dependencyprocessors.PairProcessor;
import de.peran.dependencyprocessors.VersionComparator;
import de.peran.dependencyprocessors.ViewNotFoundException;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.reduceddependency.ChangedTraceTests;
import de.peran.utils.OptionConstants;
import de.peran.utils.StreamGobbler;
import de.peran.vcs.GitUtils;

public class ViewGenerator extends PairProcessor {

   private static final Logger LOG = LogManager.getLogger(ViewPrintStarter.class);

   private static final ObjectMapper MAPPER = new ObjectMapper();

   static {
      MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
   }

   private final File viewFolder;
   private final File executeFile;
   private final ChangedTraceTests changedTraceMethods = new ChangedTraceTests();

   public ViewGenerator(File projectFolder, Versiondependencies dependencies, File executefile, File viewFolder) {
      super(projectFolder, dependencies);
      this.viewFolder = viewFolder;
      this.executeFile = executefile;
   }

   public ViewGenerator(final String[] args) throws ParseException, JAXBException, JsonParseException, JsonMappingException, IOException {
      super(args);
      final File resultFolder = DependencyReadingStarter.getResultFolder();
      final String projectName = folders.getProjectFolder().getName();
      final String url = GitUtils.getURL(folders.getProjectFolder());
      changedTraceMethods.setUrl(url);

      viewFolder = new File(resultFolder, "views_" + projectName);
      if (!viewFolder.exists()) {
         viewFolder.mkdir();
      }
      executeFile = new File(viewFolder, "execute-" + projectName + ".json");

      if (line.hasOption(OptionConstants.CHANGEFILE.getName())) {
         final File changeFile = new File(line.getOptionValue(OptionConstants.CHANGEFILE.getName()));
         final VersionKnowledge knowledge = new ObjectMapper().readValue(changeFile, VersionKnowledge.class);

         for (final Iterator<Version> iterator = dependencies.getVersions().getVersion().iterator(); iterator.hasNext();) {
            final Version version = iterator.next();
            final Changes changes = knowledge.getVersion(version.getVersion());
            if (changes.getTestcaseChanges().size() == 0) {
               iterator.remove();
            }
         }
      }
   }

   @Override
   protected void processVersion(final Version versioninfo) {
      final String version = versioninfo.getVersion();
      LOG.info("View-Generation for Version {}", version);
      final Set<TestCase> testcases = findTestcases(versioninfo);

      final boolean beforeEndVersion = endversion == null || version.equals(endversion) || VersionComparator.isBefore(version, endversion);
      LOG.debug("Before End Version {}: {}", endversion, beforeEndVersion);
      if (!VersionComparator.isBefore(version, startversion) && beforeEndVersion) {
         for (final TestCase testcase : testcases) {
            if (lastTestcaseCalls.containsKey(testcase)) {

               final String versionOld = lastTestcaseCalls.get(testcase);

               final File viewResultsFolder = new File(viewFolder, "view_" + version);
               if (!viewResultsFolder.exists()) {
                  viewResultsFolder.mkdir();
               }
               final File clazzDir = new File(viewResultsFolder, testcase.getClazz());
               if (!clazzDir.exists()) {
                  clazzDir.mkdir();
               }
               final Map<String, List<File>> traceFileMap = new HashMap<>();

               final File diffFolder = new File(viewResultsFolder, "diffs");
               if (!diffFolder.exists()) {
                  diffFolder.mkdirs();
               }
               try {
                  final boolean tracesWorked = generateTraces(version, testcase, versionOld, clazzDir, traceFileMap);

                  if (tracesWorked) {
                     LOG.debug("Generating Diff " + testcase.getClazz() + "#" + testcase.getMethod() + " " + versionOld + ".." + version);
                     final boolean somethingChanged = generateDiffFiles(testcase, diffFolder, traceFileMap);

                     if (somethingChanged) {
                        changedTraceMethods.addCall(version, testcase);
                     }
                  }
               } catch (final ViewNotFoundException | XmlPullParserException | IOException | InterruptedException | com.github.javaparser.ParseException e) {
                  e.printStackTrace();
               }
            }
            try (FileWriter fw = new FileWriter(executeFile)) {
               fw.write(MAPPER.writeValueAsString(changedTraceMethods));
               fw.flush();
            } catch (final IOException e) {
               e.printStackTrace();
            }
            lastTestcaseCalls.put(testcase, version);
         }
      } else {
         for (final TestCase testcase : testcases) {
            lastTestcaseCalls.put(testcase, version);
         }
      }
   }

   @Override
   protected Runnable processVersionParallel(Version versioninfo, final File projectFolderTemp) {
      return new Runnable() {

         @Override
         public void run() {
            final ProcessBuilder builder = new ProcessBuilder("git", "clone", folders.getProjectFolder().getAbsolutePath(), projectFolderTemp.getName());
            builder.directory(folders.getTempProjectFolder());
            final File tempResultFolder = new File(folders.getTempProjectFolder(), "data_" + versioninfo.getVersion());
            tempResultFolder.mkdir();
            final File executeFileTemp = new File(tempResultFolder, "execute.json");
            try {
               builder.start().waitFor();
               
               final ViewGenerator generator = new ViewGenerator(projectFolderTemp, dependencies, executeFileTemp, tempResultFolder);
               generator.setStartversion(versioninfo.getVersion());
               generator.setEndversion(versioninfo.getVersion());
               generator.processCommandline();
            } catch (ParseException | JAXBException e) {
               e.printStackTrace();
            } catch (final InterruptedException e) {
               e.printStackTrace();
            } catch (final IOException e) {
               e.printStackTrace();
            }
            mergeParallelVersion(versioninfo.getVersion(), tempResultFolder, executeFileTemp);
         }
      };
   }
   
   public void mergeParallelVersion(String version, final File tempResultFolder, final File executeFileTemp) {
      synchronized (changedTraceMethods) {
         try (FileWriter fw = new FileWriter(executeFile)) {
            final ChangedTraceTests tests = MAPPER.readValue(executeFileTemp, ChangedTraceTests.class);
            //Inserting on right position would be fast than sorting all versions everytime, but for ~500 versions this is not too slow
            changedTraceMethods.getVersions().putAll(tests.getVersions());
            changedTraceMethods.sort();
            fw.write(MAPPER.writeValueAsString(changedTraceMethods));
            fw.flush();
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }
      final File viewFolderTemp = new File(tempResultFolder, "view_"+version);
      final File viewGoalFolder = new File(viewFolder, "view_"+version);
      try {
         FileUtils.moveDirectory(viewFolderTemp, viewGoalFolder);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   protected boolean generateTraces(final String version, final TestCase testcase, final String versionOld, final File clazzDir, final Map<String, List<File>> traceFileMap)
         throws IOException, InterruptedException, com.github.javaparser.ParseException, ViewNotFoundException, XmlPullParserException {
      for (final String githash : new String[] { versionOld, version }) {
         LOG.debug("Checkout...");
         GitUtils.goToTag(githash, folders.getProjectFolder());

         LOG.debug("Calling Maven-Kieker...");
         final TestResultManager tracereader = new TestResultManager(folders.getProjectFolder());
         final TestSet testset = new TestSet();
         testset.addTest(testcase);
         tracereader.executeKoPeMeKiekerRun(testset, githash);

         LOG.debug("Trace-Analysis..");
         final File moduleFolder;
         if (testcase.getModule() != null) {
            moduleFolder = tracereader.getXMLFileFolder(new File(folders.getProjectFolder(), testcase.getModule()));
         } else {
            moduleFolder = tracereader.getXMLFileFolder(folders.getProjectFolder());
         }
         final boolean worked = analyseTrace(testcase, clazzDir, traceFileMap, githash, moduleFolder);
         if (!worked) {
            return false;
         }

         tracereader.deleteTempFiles();
      }
      return true;
   }

   /**
    * Generates a human-analysable diff-file from traces
    * 
    * @param testcase Name of the testcase
    * @param diffFolder Goal-folder for the diff
    * @param traceFileMap Map for place where traces are saved
    * @return Whether a change happened
    * @throws IOException If files can't be read of written
    */
   protected boolean generateDiffFiles(final TestCase testcase, final File diffFolder, final Map<String, List<File>> traceFileMap) throws IOException {
      final File diffFile = new File(diffFolder, testcase.getMethod() + ".txt");
      final File diffFileMethod = new File(diffFolder, testcase.getMethod() + "_method.txt");
      final List<File> traceFiles = traceFileMap.get(testcase.getMethod());
      final Process checkDiff = Runtime.getRuntime()
            .exec("diff " + traceFiles.get(0).getAbsolutePath() + " " + traceFiles.get(1).getAbsolutePath());
      final String isDifferent = StreamGobbler.getFullProcess(checkDiff, false);
      if (isDifferent.length() > 0) {
         final Process p = Runtime.getRuntime().exec("diff --minimal -y -W 200 "
               + traceFiles.get(0).getAbsolutePath() + " " + traceFiles.get(1).getAbsolutePath());
         final String result = StreamGobbler.getFullProcess(p, false);
         try (final FileWriter fw = new FileWriter(diffFile)) {
            fw.write(result);
         }

         final Process p2 = Runtime.getRuntime()
               .exec("diff --minimal -y -W 200 " + traceFiles.get(0).getAbsolutePath() + "_method "
                     + traceFiles.get(1).getAbsolutePath() + "_method");
         final String result2 = StreamGobbler.getFullProcess(p2, false);
         try (final FileWriter fw = new FileWriter(diffFileMethod)) {
            fw.write(result2);
         }
         return true;
      } else {
         LOG.info("No change; traces equal.");
         return false;
      }
   }

   private boolean analyseTrace(final TestCase testcase, final File clazzDir, final Map<String, List<File>> traceFileMap, final String githash, final File resultsFolder)
         throws com.github.javaparser.ParseException, IOException, ViewNotFoundException, XmlPullParserException {
      final File projectResultFolder = new File(resultsFolder, testcase.getClazz());
      final File[] listFiles = projectResultFolder.listFiles(new FileFilter() {
         @Override
         public boolean accept(final File pathname) {
            return pathname.getName().matches("[0-9]*");
         }
      });
      if (listFiles == null || listFiles.length != 1) {
         throw new ViewNotFoundException("Result folder: " + Arrays.toString(listFiles) + " ("
               + (listFiles != null ? listFiles.length : "null") + ") in " + projectResultFolder.getAbsolutePath() + " should only be exactly one folder!");
      }

      final File methodResult = new File(listFiles[0], testcase.getMethod());
      boolean success = false;

      LOG.debug("Searching for: {}", methodResult);
      if (methodResult.exists() && methodResult.isDirectory()) {
         final long size = FileUtils.sizeOfDirectory(methodResult);
         final long sizeInMB = size / (1024 * 1024);
         LOG.debug("Filesize: {} ({})", sizeInMB, size);
         if (sizeInMB < 2000) {
            final File[] possiblyMethodFolder = methodResult.listFiles();
            final File kiekerResultFolder = possiblyMethodFolder[0];
            final List<TraceElement> shortTrace = new CalledMethodLoader(kiekerResultFolder, folders.getProjectFolder()).getShortTrace("");
            LOG.debug("Short Trace: {}", shortTrace.size());
            final List<File> modules = MavenPomUtil.getModules(new File(folders.getProjectFolder(), "pom.xml"));
            final File[] files = new File[modules.size() * 4];
            for (int i = 0; i < modules.size(); i++) {
               files[i] = new File(modules.get(i), "src/main/java");
               files[i + 1] = new File(modules.get(i), "src/java");
               files[i + 2] = new File(modules.get(i), "src/test/java");
               files[i + 3] = new File(modules.get(i), "src/test");
            }

            final TraceMethodReader traceMethodReader = new TraceMethodReader(shortTrace, files);
            final TraceWithMethods trace = traceMethodReader.getTraceWithMethods();
            List<File> traceFile = traceFileMap.get(testcase.getMethod());
            if (traceFile == null) {
               traceFile = new LinkedList<>();
               traceFileMap.put(testcase.getMethod(), traceFile);
            }
            final File currentTraceFile = new File(clazzDir, testcase.getMethod() + "_hash_" + githash);
            traceFile.add(currentTraceFile);
            try (final FileWriter fw = new FileWriter(currentTraceFile)) {
               fw.write(trace.getWholeTrace());
            }
            final File methodTrace = new File(clazzDir, testcase.getMethod() + "_hash_" + githash + "_method");
            try (final FileWriter fw = new FileWriter(methodTrace)) {
               LOG.debug("Methoden: " + trace.getTraceMethods().length());
               fw.write(trace.getTraceMethods());
            }
            success = true;
         } else {
            LOG.error("File size exceeds 2000 MB");
         }
      }
      FileUtils.deleteDirectory(resultsFolder);
      return success;
   }

   public File getExecuteFile() {
      return executeFile;
   }
}
