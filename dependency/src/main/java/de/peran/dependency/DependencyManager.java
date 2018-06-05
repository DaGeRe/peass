/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.peran.dependency;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peran.dependency.analysis.CalledMethodLoader;
import de.peran.dependency.analysis.ModuleClassMapping;
import de.peran.dependency.analysis.data.CalledMethods;
import de.peran.dependency.analysis.data.ChangedEntity;
import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependency.analysis.data.TestDependencies;
import de.peran.dependency.analysis.data.TestExistenceChanges;
import de.peran.dependency.analysis.data.TestSet;
import de.peran.dependency.execution.MavenPomUtil;
import de.peran.dependency.execution.TestExecutor;

/**
 * Runs tests with kieker and reads the dependencies of tests for each version
 * 
 * @author reichelt
 *
 */
public class DependencyManager extends TestResultManager {

   private static final Logger LOG = LogManager.getLogger(DependencyManager.class);

   private final TestDependencies dependencies = new TestDependencies();
   private List<String> existingClasses;

   /**
    * Creates a new ChangeTestClassesHandler for the given folder with the given groupId and projectId. The groupId and projectId are needed to determine where the results are
    * afterwards.
    * 
    * @param projectFolder
    */
   public DependencyManager(final File projectFolder) {
      super(projectFolder);
   }

   public TestDependencies getDependencyMap() {
      return dependencies;
   }

   public boolean initialyGetTraces() throws IOException, InterruptedException {
      if (folders.getKiekerResultFolder().exists()) {
         FileUtils.deleteDirectory(folders.getKiekerResultFolder());
      }

      ModuleClassMapping.loadClasses(folders.getProjectFolder());
      executor.executeAllTests(new File(folders.getLogFolder(), "init_log.txt"));

      loadClasses();
      if (folders.getKiekerResultFolder().exists()) {
         final Collection<File> xmlFiles = FileUtils.listFiles(folders.getKiekerResultFolder(), new WildcardFileFilter("*.xml"), TrueFileFilter.INSTANCE);
         LOG.debug("Initial test execution finished, starting result collection, analyzing {} files", xmlFiles.size());
         for (final File testResultFile : xmlFiles) {
            final String testClassName = testResultFile.getParentFile().getName();
            final String testMethodName = testResultFile.getName().substring(0, testResultFile.getName().length() - 4); // remove
            // .xml
            final File parent = testResultFile.getParentFile();
            final String moduleOfClass = ModuleClassMapping.getModuleOfClass(testClassName);
            if (moduleOfClass == null) {
               throw new RuntimeException("Module of class " + testClassName + " is null");
            }
            final ChangedEntity entity = new ChangedEntity(testClassName, moduleOfClass, testMethodName);
            updateDependenciesOnce(entity, parent);
         }
         LOG.debug("Result collection finished");

         folders.getKiekerResultFolder().renameTo(new File(folders.getKiekerResultFolder().getParentFile(), "initialresults_kieker"));
         return true;
      } else {
         try {
            boolean sourceFound = false;
            for (final File module : MavenPomUtil.getModules(new File(folders.getProjectFolder(), "pom.xml"))) {
               final File testSourceFolder = new File(module, "src/test");
               final Collection<File> javaTestFiles = FileUtils.listFiles(testSourceFolder, new WildcardFileFilter("*test*.java", IOCase.INSENSITIVE), TrueFileFilter.INSTANCE);
               if (javaTestFiles.size() > 0) {
                  sourceFound = true;
               }
            }
            if (sourceFound) {
               LOG.debug("No result data available - error occured?");
               return false;
            } else {
               LOG.debug("No result data available, but no test-classes existing - so it is ok.");
               return true;
            }
         } catch (final XmlPullParserException e) {
            e.printStackTrace();
            return false;
         }

      }
   }

   /**
    * Loads which classes exist
    */
   private void loadClasses() {
      existingClasses = new LinkedList<>();
      try {
         for (final File module : MavenPomUtil.getModules(new File(folders.getProjectFolder(), "pom.xml"))) {
            final Collection<File> files = FileUtils.listFiles(module, new WildcardFileFilter("*.java"), TrueFileFilter.INSTANCE);
            for (final File file : files) {
               final String withoutProjectPrefixPath = file.getAbsolutePath().replaceAll(module.getAbsolutePath(), "");
               final String packagePath = withoutProjectPrefixPath.substring(1, withoutProjectPrefixPath.lastIndexOf("."));
               final String packatsch = packagePath
                     .replaceAll("src/test/java/", "")
                     .replaceAll("src/main/java/", "")
                     .replaceAll("src/main/", "")
                     .replaceAll("src/test/", "")
                     .replaceAll("src/java/", "");
               existingClasses.add(packatsch.replaceAll(File.separator, "."));
            }
         }
      } catch (IOException | XmlPullParserException e) {
         e.printStackTrace();
      }

   }

   /**
    * Updates Dependencies of the given testClassName and the given testMethodName based upon the file where the kieker-results are stored
    * 
    * @param testClassName
    * @param testMethodName
    * @param parent
    */
   public void updateDependenciesOnce(final ChangedEntity testClassName, final File parent) {
      LOG.debug("Parent: " + parent);
      final File kiekerResultFolder = findKiekerFolder(testClassName.getMethod(), parent);

      if (kiekerResultFolder == null) {
         return;
      }

      final long size = FileUtils.sizeOfDirectory(kiekerResultFolder);
      final long sizeInMB = size / (1024 * 1024);

      LOG.debug("Größe: {} Ordner: {}", sizeInMB, kiekerResultFolder);
      if (sizeInMB > 2000) {
         return;
      }

      final PrintStream out = System.out;
      final PrintStream err = System.err;

      final File kiekerOutputFile = new File(folders.getLogFolder(), "ausgabe_kieker.txt");

      try {
         System.setOut(new PrintStream(kiekerOutputFile));
         System.setErr(new PrintStream(kiekerOutputFile));
         final Map<ChangedEntity, Set<String>> calledClasses = new CalledMethodLoader(kiekerResultFolder, folders.getProjectFolder()).getCalledMethods();

         for (final Iterator<ChangedEntity> iterator = calledClasses.keySet().iterator(); iterator.hasNext();) {
            final ChangedEntity entity = iterator.next();
            final String wholeClassName = entity.getJavaClazzName();

            // ignore inner class part, because it is in the same file - if the file exists, it is very likely that a subclass, which is in the logs, exists also
            final String outerClazzName = ClazzFinder.getOuterClass(wholeClassName);
            LOG.trace("Testing: " + outerClazzName);
            if (!existingClasses.contains(outerClazzName)) {
               // Removes classes not in package
               iterator.remove();
            } else {
               LOG.trace("Existing: " + outerClazzName);
            }
         }
         for (final Iterator<ChangedEntity> iterator = calledClasses.keySet().iterator(); iterator.hasNext();) {
            final ChangedEntity clazz = iterator.next();
            if (clazz.getModule() == null) {
               throw new RuntimeException("Class " + clazz.getJavaClazzName() + " has no module!");
            }
         }

         LOG.debug("Test: {} ", testClassName);
         LOG.debug("Kieker: {} Dependencies: {}", kiekerResultFolder.getAbsolutePath(), calledClasses.size());
         addDependencies(testClassName, calledClasses);
      } catch (final FileNotFoundException e) {
         e.printStackTrace();
      } finally {
         System.setOut(out);
         System.setErr(err);
      }

   }

   private File findKiekerFolder(final String testMethodName, final File parent) {
      final File[] listFiles = parent.listFiles(new FileFilter() {
         @Override
         public boolean accept(final File pathname) {
            return pathname.getName().matches("[0-9]*");
         }
      });
      LOG.debug("Kieker-Files: {}", listFiles.length);
      if (listFiles.length == 0) {
         LOG.info("No result folder existing - probably a package name change?");
         LOG.info("Files: {}", Arrays.toString(parent.list()));
         return null;
      }
      final File kiekerAllFolder = listFiles[0];
      LOG.debug("Analysing Folder: {} {}", kiekerAllFolder.getAbsolutePath(), testMethodName);
      final File kiekerNextFolder = new File(kiekerAllFolder, testMethodName);
      if (kiekerNextFolder.exists()) {
         final File kiekerResultFolder = kiekerNextFolder.listFiles()[0];
         LOG.debug("Test: " + testMethodName);
         return kiekerResultFolder;
      } else {
         return null;
      }

   }

   /**
    * 
    * @param testClassName
    * @param testMethodName
    * @param calledClasses Map from name of the called class to the methods of the class that are called
    */
   public void addDependencies(final ChangedEntity testClassName, final Map<ChangedEntity, Set<String>> calledClasses) {
      final Map<ChangedEntity, Set<String>> testDependencies = dependencies.getDependenciesForTest(testClassName);
      testDependencies.putAll(calledClasses);
   }

   /**
    * Updates the dependencies of the current version by running each testclass once. The testcases, that have been added in this version, are returned (since they can not be
    * determined from the old dependency information or the svn diff directly). TODO: What if testcases are removed?
    * 
    * @param testsToUpdate
    * @return
    * @throws IOException
    * @throws InterruptedException
    */
   public TestExistenceChanges updateDependencies(final TestSet testsToUpdate, final String version) throws IOException {
      final Map<ChangedEntity, Map<ChangedEntity, Set<String>>> oldDepdendencies = dependencies.getCopiedDependencies();
      // Remove all old dependencies where changes happened, because they may
      // have been deleted
      for (final Entry<ChangedEntity, List<String>> className : testsToUpdate.entrySet()) {
         for (final String method : className.getValue()) {
            final ChangedEntity methodEntity = className.getKey().copy();
            methodEntity.setMethod(method);
            dependencies.getDependencyMap().remove(methodEntity);
         }
      }

      truncateKiekerResults();

      LOG.debug("Führe Tests neu aus für Abhängigkeiten-Aktuallisierung, Ergebnisordner: {}", folders.getKiekerResultFolder());
      final TestSet tests = new TestSet();
      for (final ChangedEntity clazzname : testsToUpdate.getClasses()) {
         tests.addTest(clazzname, "");
      }
      executeKoPeMeKiekerRun(tests, version);

      LOG.debug("Beginne Abhängigkeiten-Aktuallisierung für {} Klassen", testsToUpdate.getClasses().size());

      final TestExistenceChanges changes = new TestExistenceChanges();

      loadClasses();

      for (final Entry<ChangedEntity, List<String>> entry : testsToUpdate.entrySet()) {
         final String testClassName = entry.getKey().getJavaClazzName();
         final File testclazzFolder;
         if (entry.getKey().getModule().equals("")) {
            final File xmlFileFolder = getXMLFileFolder(folders.getProjectFolder());
            testclazzFolder = new File(xmlFileFolder, entry.getKey().getJavaClazzName());
         } else {
            final File moduleFolder = new File(folders.getProjectFolder(), entry.getKey().getModule());
            final File xmlFileFolder = getXMLFileFolder(moduleFolder);
            testclazzFolder = new File(xmlFileFolder, entry.getKey().getJavaClazzName());
         }
         LOG.debug("Suche in {} Existiert: {} Ordner: {} Tests: {}", testclazzFolder, testclazzFolder.exists(), testclazzFolder.isDirectory(), entry.getValue());
         if (testclazzFolder.exists()) {
            // update method(s)
            final Set<String> notFound = new TreeSet<>();
            notFound.addAll(entry.getValue());
            for (final File testResultFile : testclazzFolder.listFiles((FileFilter) new WildcardFileFilter("*.xml"))) {
               final String testClassName2 = testResultFile.getParentFile().getName();
               if (!testClassName2.equals(testClassName)) {
                  LOG.error("Testclass " + testClassName + " != " + testClassName2);
               }
               final File parent = testResultFile.getParentFile();
               final String testMethodName = testResultFile.getName().substring(0, testResultFile.getName().length() - 4);
               final String module = ModuleClassMapping.getModuleOfClass(testClassName);
               updateDependenciesOnce(new ChangedEntity(testClassName, module, testMethodName), parent);
               notFound.remove(testMethodName);
            }
            LOG.debug("Removed tests: {}", notFound);
            for (final String testMethodName : notFound) {
               final ChangedEntity entity = entry.getKey().copy();
               entity.setMethod(testMethodName);
               dependencies.removeTest(entity);
               testsToUpdate.removeTest(entry.getKey(), testMethodName);
               changes.addRemovedTest(new TestCase(testClassName, testMethodName));
            }
         } else {
            LOG.error("Testclass {} does not exist anymore or does not create results. Folder: {}", entry.getKey(), testclazzFolder);
            changes.addRemovedTest(new TestCase(entry.getKey().getClazz(), ""));
         }
      }

      /**
       * In this part, the method is unknown if a class-wide change happened, p.e. if a new subclass is declared and because of this change a new testcase needs to be called.
       */
      for (final Map.Entry<ChangedEntity, CalledMethods> entry : dependencies.getDependencyMap().entrySet()) {
         // testclass -> depending class -> method
         final ChangedEntity testcase = entry.getKey();
         if (!oldDepdendencies.containsKey(testcase)) {
            for (final Map.Entry<ChangedEntity, Set<String>> changeEntry : entry.getValue().getCalledMethods().entrySet()) {
               final ChangedEntity changedclass = changeEntry.getKey();
               for (final String changedMethod : changeEntry.getValue()) {
                  // Since the testcase is new, is is always caused
                  // primarily by a change of the test class, and not of
                  // any other changed class
                  final ChangedEntity methodEntity = changedclass.copy();
                  methodEntity.setMethod(changedMethod);
                  changes.addAddedTest(methodEntity, testcase);
               }
            }
         }
      }
      return changes;
   }

   private void truncateKiekerResults() {
      LOG.debug("Truncating: {}", folders.getKiekerResultFolder().getAbsolutePath());
      try {
         FileUtils.deleteDirectory(folders.getKiekerResultFolder());
      } catch (final IOException e) {
         e.printStackTrace();
         if (folders.getKiekerResultFolder().exists()) {
            try {
               FileUtils.deleteDirectory(folders.getKiekerResultFolder());
            } catch (final IOException e1) {
               e1.printStackTrace();
            }
         }
      }
   }

   /**
    * Returns the tests that need to be run in the current version based on the given changes, i.e. the given changed classes and changed methods
    * 
    * @param map from changed classes to changed methods (or, if class changed as a whole, an empty set)
    * @return Map from testclasses to the test methods of the class that need to be run
    */
   public TestSet getTestsToRun(final Map<ChangedEntity, Set<String>> changedClassNames) {
      final TestSet testsToRun = new TestSet();
      for (final ChangedEntity testName : changedClassNames.keySet()) {
         if (testName.getJavaClazzName().toLowerCase().contains("test")) {
            testsToRun.addTest(testName, null);
         }
      }
      for (final Map.Entry<ChangedEntity, CalledMethods> testDependencies : dependencies.getDependencyMap().entrySet()) {
         final Set<ChangedEntity> currentTestDependencies = testDependencies.getValue().getCalledClasses();
         for (final ChangedEntity changedClass : changedClassNames.keySet()) {
            LOG.trace("Prüfe Abhängigkeiten für {} von {}", changedClass, testDependencies.getKey());
            LOG.trace("Abhängig: {} Abhängig von Testklasse: {}", currentTestDependencies.contains(changedClass), changedClass.equals(testDependencies.getKey()));
            if (currentTestDependencies.contains(changedClass)) {
               LOG.info("Test " + testDependencies.getKey() + " benötigt geänderte Klasse " + changedClass);
               // final String javaEntityName = testDependencies.getKey().getJavaClazzName();
               // final String testClassName = testDependencies.getKey().getJavaClazzName();
               final String testMethodName = testDependencies.getKey().getMethod();
               // final String testClassName = testDependencies.getKey().substring(0, testDependencies.getKey().lastIndexOf('.'));
               // final String testMethodName = testDependencies.getKey().substring(testDependencies.getKey().lastIndexOf('.') + 1);
               final ChangedEntity entity = testDependencies.getKey().copy();
               entity.setMethod(null);
               testsToRun.addTest(entity, testMethodName);
               break;
            }
         }
      }
      return testsToRun;
   }

   public TestExecutor getExecutor() {
      return executor;
   }
}
