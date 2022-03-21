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

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.changesreading.ClazzFinder;
import de.dagere.peass.dependency.changesreading.JavaParserProvider;
import de.dagere.peass.dependency.traces.TraceElementContent;
import de.dagere.peass.dependency.traces.TraceReadUtils;

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

   private final ExecutionConfig executionConfig;
   
   public ClazzFileFinder(final ExecutionConfig executionConfig) {
      this.executionConfig = executionConfig;
   }

   /**
    * Returns a list of all classes of a maven project as Java FQN
    * 
    * @param projectFolder Folder where to search for classes
    * @return list of classes
    */
   public List<String> getClasses(final File projectFolder) {
      File clazzpathFolder = getFirstExistingFolder(projectFolder, executionConfig.getClazzFolders());
      
      final List<String> clazzes = new LinkedList<>();
      if (clazzpathFolder != null) {
         addClazzes(clazzes, clazzpathFolder);
      }

      final List<String> testClazzes = getTestClazzes(projectFolder);
      clazzes.addAll(testClazzes);
      return clazzes;
   }

   private File getFirstExistingFolder(final File projectFolder, final List<String> folders) {
      File foundFolder = null;
      for (String folderCandidate : folders) {
         File candidate = new File(projectFolder, folderCandidate);
         if (candidate.exists()) {
            foundFolder = candidate;
            break;
         }
      }
      return foundFolder;
   }

   /**
    * Returns a list of classes or a project as Java FQN
    * 
    * @param src
    * @return
    */
   public List<String> getTestClazzes(final File projectFolder) {
      final List<String> clazzes = new LinkedList<>();
      final File testFolder = getFirstExistingFolder(projectFolder, executionConfig.getTestClazzFolders());
      if (testFolder != null && testFolder.exists()) {
         addClazzes(clazzes, testFolder);
      }
      return clazzes;
   }

   /**
    * Searches for classes in a specific folder
    * 
    * @param clazzes List where classes should be added
    * @param folder Main folder that should be searched
    */
   private static void addClazzes(final List<String> clazzes, final File folder) {
      Collection<File> javaFiles = FileUtils.listFiles(folder, new WildcardFileFilter("*.java"), TrueFileFilter.INSTANCE);
      for (final File clazzFile : javaFiles) {
         final String clazz = getClazz(folder, clazzFile);
         final String packageName = clazz.lastIndexOf('.') != -1 ? clazz.substring(0, clazz.lastIndexOf('.')) : "";

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

   private static String getClazz(final File folder, final File clazzFile) {
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

   public File getClazzFile(final File module, final TestCase testcase) {
      return getClazzFile(module, testcase.toEntity());
   }
   
   public File getClazzFile(final File module, final ChangedEntity entity) {
      LOG.debug("Searching: {} in {}", entity, module.getAbsolutePath());
      final String clazzName = getOuterClass(entity.getClazz());
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

   private File findFile(final File sourceParentFolder, final String clazzFileName, final File naturalCandidate) {
      File potentialFile = null;
      if (naturalCandidate.exists()) {
         potentialFile = naturalCandidate;
      }
      
      for (final String potentialFolder : executionConfig.getAllClazzFolders()) {
         final File candidate = new File(sourceParentFolder, potentialFolder + File.separator + clazzFileName);
         if (candidate.exists()) {
            potentialFile = candidate;
         }
      }
      return potentialFile;
   }

   public File getSourceFile(final File folder, final ChangedEntity clazz) {
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
