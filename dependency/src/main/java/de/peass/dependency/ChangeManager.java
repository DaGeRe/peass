package de.peass.dependency;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.github.javaparser.ParseException;

import de.peass.dependency.analysis.FileComparisonUtil;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.ClazzChangeData;
import de.peass.dependency.analysis.data.VersionDiff;
import de.peass.dependency.execution.MavenPomUtil;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionControlSystem;
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
   private final VersionControlSystem vcs;

   public ChangeManager(final PeASSFolders folders) {
      this.folders = folders;
      vcs = VersionControlSystem.getVersionControlSystem(folders.getProjectFolder());
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
      final VersionDiff diff;
      if (vcs.equals(VersionControlSystem.GIT)) {
         diff = GitUtils.getChangedClasses(folders.getProjectFolder(), MavenPomUtil.getGenericModules(folders.getProjectFolder()), lastVersion);
      }else if (vcs.equals(VersionControlSystem.SVN)) {
         throw new RuntimeException("SVN not supported currently.");
      } else {
         throw new RuntimeException(".git or .svn not there - Can only happen if .git or .svn is deleted between constructor and method call ");
      }

      LOG.info("Changed classes: " + diff.getChangedClasses().size());
      return diff.getChangedClasses();
   }

   public void saveOldClasses() {
      try {
         if (folders.getOldSources().exists()) {
            FileUtils.deleteDirectory(folders.getOldSources());
         }
         folders.getOldSources().mkdir();
         for (final File module : MavenPomUtil.getGenericModules(folders.getProjectFolder())) {
            if (!module.equals(folders.getProjectFolder())) {
               final String relative = folders.getProjectFolder().toURI().relativize(module.toURI()).getPath();

               // final String moduleName = module.getName();
               FileUtils.copyDirectory(new File(module, "src"), new File(folders.getOldSources(), relative + File.separator + "src"));
            } else {
               FileUtils.copyDirectory(new File(module, "src"), new File(folders.getOldSources(), "src"));
            }

         }
      } catch (final IOException e) {
         e.printStackTrace();
      } catch (final XmlPullParserException e) {
         e.printStackTrace();
      }
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
      final String method = FileComparisonUtil.getMethod(folders.getProjectFolder(), clazz, clazz.getMethod());
      final String methodOld = FileComparisonUtil.getMethod(folders.getOldSources(), clazz, clazz.getMethod());

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
               final ChangedEntity clazz = clazzIterator.next();
               try {
                  final File newFile = getSourceFile(folders.getProjectFolder(), clazz);
                  final File oldFile = getSourceFile(folders.getOldSources(), clazz);
                  LOG.info("Vergleiche {}", newFile, oldFile);
                  if (newFile != null && newFile.exists() && oldFile != null) {
                     final ClazzChangeData changeData = FileComparisonUtil.getChangedMethods(newFile, oldFile);
                     if (!changeData.isChange()) {
                        clazzIterator.remove();
                        LOG.debug("Dateien gleich: {}", clazz);
                     } else {
                        changedClassesMethods.put(clazz, changeData);
                     }
                  } else {
                     LOG.info("Class did not exist before: {}", clazz);
                     changedClassesMethods.put(clazz, new ClazzChangeData(clazz, false));
                  }
               } catch (final ParseException pe) {
                  LOG.info("Class is unparsable for java parser, so to be sure it is added to the changed classes: {}", clazz);
                  changedClassesMethods.put(clazz, new ClazzChangeData(clazz, false));
                  pe.printStackTrace();
               } catch (final IOException e) {
                  LOG.info("Class is unparsable for java parser, so to be sure it is added to the changed classes: {}", clazz);
                  changedClassesMethods.put(clazz, new ClazzChangeData(clazz, false));
                  e.printStackTrace();
               }
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

   public static File getSourceFile(final File folder, final ChangedEntity clazz) {
      final ChangedEntity sourceContainingClazz = clazz.getSourceContainingClazz();
      for (final String potentialClassFolder : ChangedEntity.potentialClassFolders) {
         final File src;
         if (sourceContainingClazz.getModule().length() > 0) {
            final File moduleFolder = new File(folder, sourceContainingClazz.getModule());
            LOG.debug("Module: {}", sourceContainingClazz.getModule());
            src = new File(moduleFolder, potentialClassFolder);
         } else {
            src = new File(folder, potentialClassFolder);
         }

         final String onlyClassName = sourceContainingClazz.getJavaClazzName().substring(clazz.getJavaClazzName().lastIndexOf(".") + 1);
         LOG.debug("Suche nach {} in {}", onlyClassName, src);
         
         if (src.exists()) {
            final Iterator<File> newFileIterator = FileUtils.listFiles(src, new WildcardFileFilter(onlyClassName + ".java"), TrueFileFilter.INSTANCE).iterator();
            while (newFileIterator.hasNext()) {
               final File file = newFileIterator.next();
               final String relative = src.toURI().relativize(file.toURI()).getPath();
               LOG.debug("Searching: " + sourceContainingClazz.getFilename() + " in path: " + relative); // Asure correct package
               final String currentClazzName = potentialClassFolder + relative;
               final String partPath = sourceContainingClazz.getJavaClazzName().replace('.', File.separatorChar);
               LOG.debug(sourceContainingClazz.getFilename() + " " + currentClazzName);
               if (relative.contains(partPath)) {
//               if (clazz.getFilename().equals(currentClazzName)) {
                  LOG.debug("Found");
                  return file;
               }
            }
         }
      }
      
      return null;
   }

}
