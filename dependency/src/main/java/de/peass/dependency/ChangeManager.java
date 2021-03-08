package de.peass.dependency;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.github.javaparser.ParseException;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.VersionDiff;
import de.peass.dependency.changesreading.ClazzChangeData;
import de.peass.dependency.changesreading.FileComparisonUtil;
import de.peass.dependency.execution.MavenPomUtil;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionIterator;
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

   private final PeASSFolders folders;
   private final VersionIterator iterator;

   public ChangeManager(final PeASSFolders folders, VersionIterator iterator) {
      this.folders = folders;
      this.iterator = iterator;
   }

   /**
    * Returns a set of the full qualified names of all classes that have been changed in the current revision.
    * 
    * @return full qualified names of all classes that have been changed in the current revision.
    * @throws XmlPullParserException
    * @throws IOException
    * @throws FileNotFoundException
    */
   private List<ChangedEntity> getChangedClasses(final String lastVersion) throws FileNotFoundException, IOException, XmlPullParserException {
      final VersionDiff diff = iterator.getChangedClasses(folders.getProjectFolder(), MavenPomUtil.getGenericModules(folders.getProjectFolder()), lastVersion);
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
         for (final File module : MavenPomUtil.getGenericModules(folders.getProjectFolder())) {
            saveModule(module);
         }
      } catch (final IOException | XmlPullParserException e) {
         LOG.debug("Could not save (all) old files");
         e.printStackTrace();
      }
   }

   void saveModule(final File module) throws IOException {
      File destModuleDir;
      if (!module.equals(folders.getProjectFolder())) {
         final String relative = folders.getProjectFolder().toURI().relativize(module.toURI()).getPath();
         destModuleDir = new File(folders.getOldSources(), relative + File.separator + "src");
      } else {
         destModuleDir = new File(folders.getOldSources(), "src");
      }
      final File srcDir = new File(module, "src");
      LOG.debug("Copying from {} to {}", srcDir.getAbsolutePath(), destModuleDir.getAbsolutePath());
//      Files.copy(srcDir.toPath(), destModuleDir.toPath());
       FileUtils.copyDirectory(srcDir, destModuleDir, new FileFilter() {
         
         @Override
         public boolean accept(File pathname) {
            return pathname.canRead() && pathname.canWrite();
         }
      });
   }

   public Map<ChangedEntity, ClazzChangeData> getChanges(final String version1, final String version2) {
      GitUtils.goToTag(version1, folders.getProjectFolder());
      saveOldClasses();
      GitUtils.goToTag(version2, folders.getProjectFolder());
      return getChanges(version1);
   }

   /**
    * Attention: assumes, that getChanges() has been called or that somehow different the lastSourcesFolder has been filled
    * 
    * @param clazz
    * @return
    * @throws FileNotFoundException
    */
   public Patch<String> getKeywordChanges(final ChangedEntity clazz) throws FileNotFoundException {
      final String method = FileComparisonUtil.getMethodSource(folders.getProjectFolder(), clazz, clazz.getMethod());
      final String methodOld = FileComparisonUtil.getMethodSource(folders.getOldSources(), clazz, clazz.getMethod());

      final Patch<String> patch = DiffUtils.diff(Arrays.asList(method.split("\n")), Arrays.asList(methodOld.split("\n")));
      return patch;
   }

   /**
    * Returns all changed classes with the corresponding changed methods. If the set of a class is empty, the whole class was changed and all tests using any method of the class
    * need to be re-evaluated.
    * 
    * @return
    */
   public Map<ChangedEntity, ClazzChangeData> getChanges(final String lastRunningVersion) {
      final Map<ChangedEntity, ClazzChangeData> changedClassesMethods = new TreeMap<>();
      try {
         final List<ChangedEntity> changedClasses = getChangedClasses(lastRunningVersion);
         LOG.debug("Before Cleaning: {}", changedClasses);
         if (folders.getOldSources().exists()) {
            for (final Iterator<ChangedEntity> clazzIterator = changedClasses.iterator(); clazzIterator.hasNext();) {
               compareClazz(changedClassesMethods, clazzIterator);
            }
         } else {
            LOG.info("Kein Ordner für alte Dateien vorhanden");
         }
      } catch (IOException | XmlPullParserException e1) {
         e1.printStackTrace();
      }

      LOG.debug("After cleaning: {}", changedClassesMethods);

      return changedClassesMethods;
   }

   private void compareClazz(final Map<ChangedEntity, ClazzChangeData> changedClassesMethods, final Iterator<ChangedEntity> clazzIterator) {
      final ChangedEntity clazz = clazzIterator.next();
      final ClazzChangeData changeData = new ClazzChangeData(clazz);
      try {
         final File newFile = ClazzFileFinder.getSourceFile(folders.getProjectFolder(), clazz);
         final File oldFile = ClazzFileFinder.getSourceFile(folders.getOldSources(), clazz);
         LOG.info("Vergleiche {}", newFile, oldFile);
         if (newFile != null && newFile.exists() && oldFile != null) {
            compareFiles(changedClassesMethods, clazzIterator, clazz, changeData, newFile, oldFile);
         } else {
            LOG.info("Class did not exist before: {}", clazz);
            changeData.addClazzChange(clazz);
            changedClassesMethods.put(clazz, changeData);
         }
      } catch (final ParseException | NoSuchElementException pe) {
         LOG.info("Class is unparsable for java parser, so to be sure it is added to the changed classes: {}", clazz);
         changeData.addClazzChange(clazz);
         changedClassesMethods.put(clazz, changeData);
         pe.printStackTrace();
      } catch (final IOException e) {
         LOG.info("Class is unparsable for java parser, so to be sure it is added to the changed classes: {}", clazz);
         changeData.addClazzChange(clazz);
         changedClassesMethods.put(clazz, changeData);
         e.printStackTrace();
      }
   }

   private void compareFiles(final Map<ChangedEntity, ClazzChangeData> changedClassesMethods, final Iterator<ChangedEntity> clazzIterator, final ChangedEntity clazz,
         final ClazzChangeData changeData, final File newFile, final File oldFile) throws ParseException, IOException {
      FileComparisonUtil.getChangedMethods(newFile, oldFile, changeData);
      boolean isImportChange = false;
      for (ChangedEntity entity : changeData.getImportChanges()) {
         final File entityFile = ClazzFileFinder.getSourceFile(folders.getProjectFolder(), entity);
         if (entityFile != null && entityFile.exists()) {
            isImportChange = true;
            changeData.setChange(true);
            changeData.setOnlyMethodChange(false);
            changeData.addClazzChange(clazz);
         }
      }
      
      if (!changeData.isChange() && !isImportChange) {
         clazzIterator.remove();
         LOG.debug("Dateien gleich: {}", clazz);
      } else {
         changedClassesMethods.put(clazz, changeData);
      }
   }

}
