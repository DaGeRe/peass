package de.dagere.peass.dependency;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.changesreading.ClazzFinder;
import de.dagere.peass.dependency.changesreading.JavaParserProvider;
import de.dagere.peass.dependency.traces.TraceReadUtils;
import de.dagere.peass.dependency.traces.requitur.content.TraceElementContent;

/**
 * Searches for all classes in a maven project. Used for instrumeting them.
 * 
 * @author reichelt
 *
 */
public class ClazzFileFinder {

   private static final Logger LOG = LogManager.getLogger(ClazzFileFinder.class);

   public static String getOuterClass(final String clazzname) {
      final int innerClassSeparatorIndex = clazzname.indexOf(ChangedEntity.CLAZZ_SEPARATOR);
      final String outerClazzName = innerClassSeparatorIndex != -1 ? clazzname.substring(0, innerClassSeparatorIndex) : clazzname;
      return outerClazzName;
   }

   /**
    * Returns a list of all classes of a maven project as Java FQN
    * 
    * @param projectFolder Folder where to search for classes
    * @return list of classes
    */
   public static List<String> getClasses(final File projectFolder) {
      final List<String> clazzes = new LinkedList<>();
      final File src = new File(projectFolder, "src");
      final File main = new File(src, "main");
      final File mainJava = new File(src, "java");

      if (mainJava.exists()) {
         addClazzes(clazzes, mainJava);
      }
      if (main.exists()) {
         final File java = new File(main, "java");
         if (java.exists()) {
            addClazzes(clazzes, java);
         } else {
            addClazzes(clazzes, main);
         }
      }

      final List<String> testClazzes = getTestClazzes(src);
      clazzes.addAll(testClazzes);
      return clazzes;
   }

   /**
    * Returns a list of classes or a project as Java FQN
    * 
    * @param src
    * @return
    */
   public static List<String> getTestClazzes(final File src) {
      final List<String> clazzes = new LinkedList<>();
      final File testFolder = getTestFolder(src);
      if (testFolder != null && testFolder.exists()) {
         addClazzes(clazzes, testFolder);
      }
      return clazzes;
   }

   public static File getTestFolder(final File src) {
      final File test = new File(src, "test");
      if (test.exists()) {
         final File java = new File(test, "java");
         if (java.exists()) {
            return java;
         } else {
            return test;
         }
      } else {
         return null;
      }
   }

   /**
    * Searches for classes in a specific folder
    * 
    * @param clazzes List where classes should be added
    * @param folder Main folder that should be searched
    */
   public static void addClazzes(final List<String> clazzes, final File folder) {
      Collection<File> javaFiles = FileUtils.listFiles(folder, new WildcardFileFilter("*.java"), TrueFileFilter.INSTANCE);
      for (final File clazzFile : javaFiles) {
         final String clazz = getClazz(folder, clazzFile);
         final String packageName = clazz.lastIndexOf('.') != -1 ? clazz.substring(0, clazz.lastIndexOf('.')) : clazz;

         try {
            final CompilationUnit cu = JavaParserProvider.parse(clazzFile);
            for (final Node node : cu.getChildNodes()) {
               final List<String> nodeChildClazzes = ClazzFinder.getClazzes(node, packageName, ".");
               clazzes.addAll(nodeChildClazzes);
            }
         } catch (final ParseProblemException e) {
            throw new RuntimeException("Problem parsing " + clazz + " from " + clazzFile.getAbsolutePath() + " Existing: " + clazzFile.exists(), e);
         } catch (final FileNotFoundException e) {
            e.printStackTrace();
         }
      }
   }

   public static String getClazz(final File folder, final File clazzFile) {
      try {
         String path = clazzFile.getCanonicalPath();
         String projectFolderPrefix = folder.getCanonicalPath() + File.separator;
         path = path.replace(projectFolderPrefix, "");
         path = path.substring(0, path.length() - 5);
         final String clazz = path.replace(File.separator, ".");
         return clazz;
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Finds the given class file in a list of possible folders
    * 
    * @param traceelement
    * @param clazzFolder
    * @return
    */
   public static File getClazzFile(final TraceElementContent traceelement, final File[] clazzFolder) {
      File clazzFile = null;
      final String clazzFileName = TraceReadUtils.getClassFileName(traceelement);
      for (final File clazzFolderCandidate : clazzFolder) {
         final File clazzFileCandidate = new File(clazzFolderCandidate, clazzFileName);
         if (clazzFileCandidate.exists()) {
            clazzFile = clazzFileCandidate;
         }
      }
      return clazzFile;
   }

   public static File getClazzFile(final File module, final ChangedEntity entity) {
      LOG.debug("Searching: {} in {}", entity, module.getAbsolutePath());
      String clazzName = getOuterClass(entity.getClazz());
      final String clazzFileName = clazzName.endsWith(".java") ? clazzName : clazzName.replace('.', File.separatorChar) + ".java";
      final File naturalCandidate = new File(module, clazzFileName);
      File potentialFile = findFile(module, clazzFileName, naturalCandidate);
      if (potentialFile == null && entity.getModule() != null && !entity.getModule().equals("")) {
         File moduleFolder = new File(module, entity.getModule());
         potentialFile = findFile(moduleFolder, clazzFileName, naturalCandidate);
      }
      try {
         if (potentialFile != null) {
            return potentialFile.getCanonicalFile();
         } else {
            return null;
         }
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private static File findFile(final File sourceParentFolder, final String clazzFileName, final File naturalCandidate) {
      File potentialFile = null;
      if (naturalCandidate.exists()) {
         potentialFile = naturalCandidate;
      }
      for (final String potentialFolder : ChangedEntity.potentialClassFolders) {
         final File candidate = new File(sourceParentFolder, potentialFolder + File.separator + clazzFileName);
         if (candidate.exists()) {
            potentialFile = candidate;
         }
      }
      return potentialFile;
   }

   public static File getSourceFile(final File folder, final ChangedEntity clazz) {
      final ChangedEntity sourceContainingClazz = clazz.getSourceContainingClazz();

      File moduleFolder;
      if (sourceContainingClazz.getModule().length() > 0) {
         moduleFolder = new File(folder, sourceContainingClazz.getModule());
         LOG.debug("Module: {}", sourceContainingClazz.getModule());
      } else {
         moduleFolder = folder;
      }
      return getClazzFile(moduleFolder, sourceContainingClazz);
   }

}
