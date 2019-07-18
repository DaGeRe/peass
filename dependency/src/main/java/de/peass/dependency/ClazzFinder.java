package de.peass.dependency;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.changesreading.FileComparisonUtil;
import de.peass.dependency.traces.TraceReadUtils;
import de.peass.dependency.traces.requitur.content.TraceElementContent;

/**
 * Searches for all classes in a maven project. Used for instrumeting them.
 * 
 * @author reichelt
 *
 */
public class ClazzFinder {

   private static final Logger LOG = LogManager.getLogger(ClazzFinder.class);

   public static String getOuterClass(final String clazzname) {
      final int innerClassSeparatorIndex = clazzname.indexOf(ChangedEntity.CLAZZ_SEPARATOR);
      final String outerClazzName = innerClassSeparatorIndex != -1 ? clazzname.substring(0, innerClassSeparatorIndex) : clazzname;
      return outerClazzName;
   }

   /**
    * Returns a list of all classes of a maven project
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
   private static void addClazzes(final List<String> clazzes, final File folder) {
      for (final File clazzFile : FileUtils.listFiles(folder, new WildcardFileFilter("*.java"), TrueFileFilter.INSTANCE)) {
         final String clazz = getClazz(folder, clazzFile);
         final String packageName = clazz.lastIndexOf('.') != -1 ? clazz.substring(0, clazz.lastIndexOf('.')) : clazz;

         try {
            final CompilationUnit cu = FileComparisonUtil.parse(clazzFile);
            for (final Node node : cu.getChildNodes()) {
               clazzes.addAll(getClazzes(node, packageName, "."));
            }

         } catch (final ParseProblemException e) {
            throw new RuntimeException("Problem parsing " + clazz + " from " + clazzFile.getAbsolutePath() + " Existing: " + clazzFile.exists(), e);
         } catch (final FileNotFoundException e) {
            e.printStackTrace();
         }
      }
   }

   private static String getClazz(final File folder, final File clazzFile) {
      String path = clazzFile.getAbsolutePath();
      path = path.replace(folder.getAbsolutePath() + File.separator, "");
      path = path.substring(0, path.length() - 5);
      final String clazz = path.replace(File.separator, ".");
      return clazz;
   }

   private static List<String> getClazzes(final Node node, final String parent, String clazzSeparator) {
      final List<String> clazzes = new LinkedList<>();
      if (node instanceof ClassOrInterfaceDeclaration) {
         final ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) node;
         final String clazzname = parent.length() > 0 ? parent + clazzSeparator + clazz.getName().getIdentifier() : clazz.getName().getIdentifier();
         clazzes.add(clazzname);
         for (final Node child : node.getChildNodes()) {
            clazzes.addAll(getClazzes(child, clazzname, clazzSeparator));
         }
      } else {
         for (final Node child : node.getChildNodes()) {
            clazzes.addAll(getClazzes(child, parent, clazzSeparator));
         }
      }
      return clazzes;
   }
   
   public static List<String> getClazzes(CompilationUnit cu) {
      final List<String> clazzes = new LinkedList<>();
      for (final Node node : cu.getChildNodes()) {
         clazzes.addAll(getClazzes(node, "", "$"));
      }
      return clazzes;
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

   public static File getClazzFile(final File module, final ChangedEntity name) {
      LOG.debug("Searching: {} in {}", name, module.getAbsolutePath());
      File potentialFile = null;
      final String clazzFileName = name.getClazz().endsWith(".java") ? name.getClazz() : name.getClazz().replace('.', File.separatorChar) + ".java";
      final File naturalCandidate = new File(module, clazzFileName);
      if (naturalCandidate.exists()) {
         potentialFile = naturalCandidate;
      }
      for (final String potentialFolder : ChangedEntity.potentialClassFolders) {
         final File candidate = new File(module, potentialFolder + File.separator + clazzFileName);
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
