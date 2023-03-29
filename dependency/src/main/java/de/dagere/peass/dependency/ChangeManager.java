package de.dagere.peass.dependency;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.nodeDiffDetector.data.MethodCall;
import de.dagere.nodeDiffDetector.diffDetection.ChangeDetector;
import de.dagere.nodeDiffDetector.diffDetection.FileComparisonUtil;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.CommitDiff;
import de.dagere.peass.dependency.changesreading.ClazzChangeData;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.vcs.CommitIterator;
import de.dagere.peass.vcs.GitUtils;
import difflib.DiffUtils;
import difflib.Patch;

/**
 * Determines whether a file has a change, and whether this change is class-wide or only affecting a method.
 * 
 * @author reichelt
 *
 */
public class ChangeManager {

   private static final Logger LOG = LogManager.getLogger(ChangeManager.class);

   private final PeassFolders folders;
   private final CommitIterator iterator;
   private final ExecutionConfig config;
   private final TestExecutor testExecutor;

   public ChangeManager(final PeassFolders folders, final CommitIterator iterator, final ExecutionConfig config, TestExecutor testExecutor) {
      this.folders = folders;
      this.iterator = iterator;
      this.config = config;
      this.testExecutor = testExecutor;
   }

   /**
    * Returns a set of the full qualified names of all classes that have been changed in the current revision.
    * 
    * @return full qualified names of all classes that have been changed in the current revision.
    * @throws IOException
    * @throws FileNotFoundException
    */
   private List<MethodCall> getChangedClasses(final String lastCommit) throws FileNotFoundException, IOException {
      List<File> moduleFiles = testExecutor.getModules().getModules(); 
      final CommitDiff diff = iterator.getChangedClasses(folders.getProjectFolder(), moduleFiles, lastCommit, config);
      LOG.info("Changed classes: " + diff.getChangedClasses().size());
      return diff.getChangedClasses();
   }

   public void saveOldClasses() {
      try {
         LOG.debug("Saving old classes");
         if (folders.getOldSources().exists()) {
            FileUtils.deleteDirectory(folders.getOldSources());
         }
         folders.getOldSources().mkdir();
         for (final File module : testExecutor.getModules().getModules()) {
            saveModule(module);
         }
      } catch (final IOException e) {
         LOG.debug("Could not save (all) old files");
         e.printStackTrace();
      }
   }

   private void saveModule(final File module) throws IOException {
      for (String path : config.getAllClazzFolders()) {
         savePath(module, path);
      }
   }

   private void savePath(final File module, final String path) throws IOException {
      final File srcDir = new File(module, path);
      
      // Only copy existing source directories, since multimodule modules may not contain a src directory
      if (srcDir.exists()) {
         File destModuleDir;
         if (!module.equals(folders.getProjectFolder())) {
            final String relative = folders.getProjectFolder().toURI().relativize(module.toURI()).getPath();
            destModuleDir = new File(folders.getOldSources(), relative + File.separator + path);
         } else {
            destModuleDir = new File(folders.getOldSources(), path);
         }
         destModuleDir.getParentFile().mkdirs();
         LOG.debug("Copying from {} to {}", srcDir.getAbsolutePath(), destModuleDir.getAbsolutePath());
         FileUtils.copyDirectory(srcDir, destModuleDir, new FileFilter() {
            
            @Override
            public boolean accept(final File pathname) {
               return pathname.canRead() && pathname.canWrite();
            }
         });
      }
   }

   public Map<MethodCall, ClazzChangeData> getChanges(final String commit1, final String commit2) {
      GitUtils.goToCommit(commit1, folders.getProjectFolder());
      saveOldClasses();
      GitUtils.goToCommit(commit2, folders.getProjectFolder());
      return getChanges(commit1);
   }

   /**
    * Attention: assumes, that getChanges() has been called or that somehow different the lastSourcesFolder has been filled
    * 
    * @param clazz
    * @return
    * @throws FileNotFoundException
    */
   public Patch<String> getKeywordChanges(final MethodCall clazz) throws FileNotFoundException {
      final String method = FileComparisonUtil.getMethodSource(folders.getProjectFolder(), clazz, clazz.getMethod(), config);
      final String methodOld = FileComparisonUtil.getMethodSource(folders.getOldSources(), clazz, clazz.getMethod(), config);

      final Patch<String> patch = DiffUtils.diff(Arrays.asList(method.split("\n")), Arrays.asList(methodOld.split("\n")));
      return patch;
   }

   /**
    * Returns all changed classes with the corresponding changed methods. If the set of a class is empty, the whole class was changed and all tests using any method of the class
    * need to be re-evaluated.
    * 
    * @return
    */
   public Map<MethodCall, ClazzChangeData> getChanges(final String lastRunningVersion) {
      final Map<MethodCall, ClazzChangeData> changedClassesMethods = new TreeMap<>();
      try {
         final List<MethodCall> changedClasses = getChangedClasses(lastRunningVersion);
         LOG.debug("Before Cleaning: {}", changedClasses);
         if (folders.getOldSources().exists()) {
            ChangeDetector detector = new ChangeDetector(config, folders);
            for (final Iterator<MethodCall> clazzIterator = changedClasses.iterator(); clazzIterator.hasNext();) {
               detector.compareClazz(changedClassesMethods, clazzIterator);
            }
         } else {
            LOG.info("There is no folder for old files");
         }
      } catch (IOException e1) {
         e1.printStackTrace();
      }

      LOG.debug("After cleaning: {}", changedClassesMethods);

      return changedClassesMethods;
   }

   

   

}
