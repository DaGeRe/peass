package de.peass.dependency;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import de.peass.dependency.analysis.data.ChangedEntity;

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
         String path = clazzFile.getAbsolutePath();
         path = path.replace(folder.getAbsolutePath() + File.separator, "");
         path = path.substring(0, path.length() - 5);
         final String clazz = path.replace(File.separator, ".");
         clazzes.add(clazz);

         try {
            final CompilationUnit cu = JavaParser.parse(clazzFile);
            for (final Node node : cu.getChildNodes()) {
               clazzes.addAll(getClazzes(node, clazz));
            }

         } catch (final ParseProblemException e) {
            throw new RuntimeException("Problem parsing " + clazz + " from " + clazzFile.getAbsolutePath(), e);
         } catch (final FileNotFoundException e) {
            e.printStackTrace();
         }

      }
   }

   private static List<String> getClazzes(final Node node, final String parent) {
      final String ownClazzName = parent.substring(parent.lastIndexOf('.')+1);
      final List<String> clazzes = new LinkedList<>();
      if (node instanceof ClassOrInterfaceDeclaration) {
         final ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) node;
         if (clazz.getParentNode().isPresent() && !clazz.getName().getIdentifier().equals(ownClazzName)) {
            final String clazzname = parent + "." + clazz.getName().getIdentifier();
            clazzes.add(clazzname);
         }
      }
      for (final Node child : node.getChildNodes()) {
         clazzes.addAll(getClazzes(child, parent));
      }
      return clazzes;
   }

   public static File getClazzFile(final File module, final ChangedEntity name) {
      LOG.debug("Searching: {} in {}", name, module.getAbsolutePath());
      File potentialFile = null;
      final String clazzFileName = (name.getClazz().endsWith(".java")) ? name.getClazz() : name.getClazz().replace('.', File.separatorChar) + ".java";
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

}
