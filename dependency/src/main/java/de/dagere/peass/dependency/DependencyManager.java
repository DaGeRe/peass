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
package de.dagere.peass.dependency;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
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

import de.dagere.peass.ci.NonIncludedTestRemover;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.CalledMethodLoader;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.CalledMethods;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestDependencies;
import de.dagere.peass.dependency.analysis.data.TestExistenceChanges;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.execution.MavenPomUtil;
import de.dagere.peass.dependency.execution.TestExecutor;
import de.dagere.peass.dependency.traces.KiekerFolderUtil;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

/**
 * Runs tests with kieker and reads the dependencies of tests for each version
 * 
 * @author reichelt
 *
 */
public class DependencyManager extends KiekerResultManager {

   private static final Logger LOG = LogManager.getLogger(DependencyManager.class);

   private final TestDependencies dependencies = new TestDependencies();
   int deleteFolderSize = 100;

   /**
    * Creates a new ChangeTestClassesHandler for the given folder with the given groupId and projectId. The groupId and projectId are needed to determine where the results are
    * afterwards.
    * 
    * @param projectFolder
    */
   public DependencyManager(final PeASSFolders folders, final ExecutionConfig executionConfig, final EnvironmentVariables env) {
      super(folders, executionConfig, env);
   }

   public DependencyManager(final TestExecutor executor, final PeASSFolders folders, final JUnitTestTransformer testTransformer) {
      super(executor, folders, testTransformer);
   }

   public TestDependencies getDependencyMap() {
      return dependencies;
   }

   public int getDeleteFolderSize() {
      return deleteFolderSize;
   }

   public void setDeleteFolderSize(final int deleteFolderSize) {
      this.deleteFolderSize = deleteFolderSize;
   }

   public boolean initialyGetTraces(final String version) throws IOException, InterruptedException, XmlPullParserException {
      if (folders.getTempMeasurementFolder().exists()) {
         FileUtils.deleteDirectory(folders.getTempMeasurementFolder());
      }

      final ModuleClassMapping mapping = new ModuleClassMapping(executor);
      executor.loadClasses();
      final File logFile = new File(folders.getLogFolder(), version + File.separator + "init_log.txt");
      logFile.getParentFile().mkdirs();

      TestSet tests = findIncludedTests(mapping);
      runTraceTests(tests, version);

      if (folders.getTempMeasurementFolder().exists()) {
         return readInitialResultFiles(mapping);
      } else {
         return printErrors();
      }
   }

   private TestSet findIncludedTests(final ModuleClassMapping mapping) throws IOException, XmlPullParserException {
      List<String> includedModules;
      if (testTransformer.getConfig().getExecutionConfig().getPl() != null) {
         includedModules = MavenPomUtil.getDependentModules(folders.getProjectFolder(), testTransformer.getConfig().getExecutionConfig().getPl());
         LOG.debug("Included modules: {}", includedModules);
      } else {
         includedModules = null;
      }

      testTransformer.determineVersions(executor.getModules().getModules());
      final TestSet tests = new TestSet();
      for (final File module : executor.getModules().getModules()) {
         for (final String clazz : ClazzFileFinder.getTestClazzes(new File(module, "src"))) {
            final String currentModule = mapping.getModuleOfClass(clazz);
            final List<String> testMethodNames = testTransformer.getTestMethodNames(module, new ChangedEntity(clazz, currentModule));
            for (String method : testMethodNames) {
               final TestCase test = new TestCase(clazz, method, currentModule);
               final List<String> includes = testTransformer.getConfig().getExecutionConfig().getIncludes();
               if (NonIncludedTestRemover.isTestIncluded(test, includes)) {
                  if (includedModules == null || includedModules.contains(test.getModule())) {
                     tests.addTest(test);
                  }
               }
            }
         }
      }
      LOG.info("Included tests: {}", tests.getTests().size());
      return tests;
   }

   private boolean printErrors() throws IOException {
      try {
         boolean sourceFound = false;
         sourceFound = searchTestFiles(sourceFound);
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

   private boolean searchTestFiles(boolean sourceFound) throws IOException, XmlPullParserException {
      for (final File module : executor.getModules().getModules()) {
         final File testSourceFolder = new File(module, "src" + File.separator + "test");
         if (testSourceFolder.exists()) {
            final Collection<File> javaTestFiles = FileUtils.listFiles(testSourceFolder, new WildcardFileFilter("*test*.java", IOCase.INSENSITIVE), TrueFileFilter.INSTANCE);
            if (javaTestFiles.size() > 0) {
               sourceFound = true;
            }
         }
      }
      return sourceFound;
   }

   private boolean readInitialResultFiles(final ModuleClassMapping mapping) {
      final Collection<File> xmlFiles = FileUtils.listFiles(folders.getTempMeasurementFolder(), new WildcardFileFilter("*.xml"), TrueFileFilter.INSTANCE);
      LOG.debug("Initial test execution finished, starting result collection, analyzing {} files", xmlFiles.size());
      for (final File testResultFile : xmlFiles) {
         final String testClassName = testResultFile.getParentFile().getName();
         final String testMethodName = testResultFile.getName().substring(0, testResultFile.getName().length() - 4); // remove
         // .xml
         final File parent = testResultFile.getParentFile();
         final String moduleOfClass = mapping.getModuleOfClass(testClassName);
         if (moduleOfClass == null) {
            throw new RuntimeException("Module of class " + testClassName + " is null");
         }
         final ChangedEntity entity = new ChangedEntity(testClassName, moduleOfClass, testMethodName);
         updateDependenciesOnce(entity, parent, mapping);
      }
      LOG.debug("Result collection finished");

      cleanResultFolder();
      return true;
   }

   private void cleanResultFolder() {
      final File movedInitialResults = new File(folders.getTempMeasurementFolder().getParentFile(), "initialresults_kieker");
      if (movedInitialResults.exists()) {
         try {
            LOG.info("Deleting old initialresults");
            FileUtils.deleteDirectory(movedInitialResults);
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      final boolean renameSuccess = folders.getTempMeasurementFolder().renameTo(movedInitialResults);
      if (!renameSuccess) {
         LOG.error("Could not move results");
      }
      for (File classFolder : movedInitialResults.listFiles()) {
         LOG.debug("Cleaning {}", classFolder.getAbsolutePath());
         cleanFolderAboveSize(classFolder, deleteFolderSize);
      }
   }

   /**
    * Updates Dependencies of the given testClassName and the given testMethodName based upon the file where the kieker-results are stored
    * 
    * @param testClassName
    * @param testMethodName
    * @param parent
    */
   public void updateDependenciesOnce(final ChangedEntity testClassName, final File parent, final ModuleClassMapping mapping) {
      LOG.debug("Parent: " + parent);
      final File kiekerResultFolder = KiekerFolderUtil.findKiekerFolder(testClassName.getMethod(), parent);

      if (kiekerResultFolder == null) {
         LOG.error("No kieker folder found: " + parent);
         return;
      }

      final long size = FileUtils.sizeOfDirectory(kiekerResultFolder);
      final long sizeInMB = size / (1024 * 1024);

      LOG.debug("Size: {} Folder: {}", sizeInMB, kiekerResultFolder);
      if (sizeInMB > CalledMethodLoader.TRACE_MAX_SIZE_IN_MB) {
         LOG.error("Trace too big!");
         return;
      }

      final File kiekerOutputFile = new File(folders.getLogFolder(), "ausgabe_kieker.txt");

      final Map<ChangedEntity, Set<String>> calledClasses = new CalledMethodLoader(kiekerResultFolder, mapping).getCalledMethods(kiekerOutputFile);

      removeNotExistingClazzes(calledClasses);
      for (final Iterator<ChangedEntity> iterator = calledClasses.keySet().iterator(); iterator.hasNext();) {
         final ChangedEntity clazz = iterator.next();
         if (clazz.getModule() == null) {
            throw new RuntimeException("Class " + clazz.getJavaClazzName() + " has no module!");
         }
      }

      LOG.debug("Test: {} ", testClassName);
      LOG.debug("Kieker: {} Dependencies: {}", kiekerResultFolder.getAbsolutePath(), calledClasses.size());
      setDependencies(testClassName, calledClasses);

   }

   private void removeNotExistingClazzes(final Map<ChangedEntity, Set<String>> calledClasses) {
      for (final Iterator<ChangedEntity> iterator = calledClasses.keySet().iterator(); iterator.hasNext();) {
         final ChangedEntity entity = iterator.next();
         final String wholeClassName = entity.getJavaClazzName();

         // ignore inner class part, because it is in the same file - if the file exists, it is very likely that a subclass, which is in the logs, exists also
         final String outerClazzName = ClazzFileFinder.getOuterClass(wholeClassName);
         LOG.trace("Testing: " + outerClazzName);
         if (!executor.getExistingClasses().contains(outerClazzName)) {
            // Removes classes not in package
            iterator.remove();
         } else {
            LOG.trace("Existing: " + outerClazzName);
         }
      }
   }

   /**
    * Since we have no information about complete dependencies when reading an old dependencyfile, just add dependencies
    * 
    * @param testClassName
    * @param testMethodName
    * @param calledClasses Map from name of the called class to the methods of the class that are called
    */
   public void addDependencies(final ChangedEntity testClassName, final Map<ChangedEntity, Set<String>> calledClasses) {
      final Map<ChangedEntity, Set<String>> testDependencies = dependencies.getOrAddDependenciesForTest(testClassName);
      for (final Map.Entry<ChangedEntity, Set<String>> calledEntity : calledClasses.entrySet()) {
         LOG.debug("adding: " + calledEntity.getKey() + " Module: " + calledEntity.getKey().getModule());
         LOG.debug(testDependencies.keySet());
         final Set<String> oldSet = testDependencies.get(calledEntity.getKey());
         if (oldSet != null) {
            oldSet.addAll(calledEntity.getValue());
         } else {
            testDependencies.put(calledEntity.getKey(), calledEntity.getValue());
         }
      }
   }

   private void setDependencies(final ChangedEntity testClassName, final Map<ChangedEntity, Set<String>> calledClasses) {
      final Map<ChangedEntity, Set<String>> testDependencies = dependencies.getOrAddDependenciesForTest(testClassName);
      testDependencies.putAll(calledClasses);
   }

   /**
    * Updates the dependencies of the current version by running each testclass once. The testcases, that have been added in this version, are returned (since they can not be
    * determined from the old dependency information or the svn diff directly). TODO: What if testcases are removed?
    * 
    * @param testsToUpdate
    * @return
    * @throws IOException
    * @throws XmlPullParserException
    * @throws InterruptedException
    */
   public TestExistenceChanges updateDependencies(final TestSet testsToUpdate, final String version, final ModuleClassMapping mapping) throws IOException, XmlPullParserException {
      final Map<ChangedEntity, Map<ChangedEntity, Set<String>>> oldDepdendencies = dependencies.getCopiedDependencies();

      // Remove all old dependencies where changes happened, because they may
      // have been deleted
      for (final Entry<ChangedEntity, Set<String>> className : testsToUpdate.entrySet()) {
         for (final String method : className.getValue()) {
            final ChangedEntity methodEntity = className.getKey().copy();
            methodEntity.setMethod(method);
            dependencies.getDependencyMap().remove(methodEntity);
         }
      }

      LOG.debug("Beginne Abhängigkeiten-Aktuallisierung für {} Klassen", testsToUpdate.getClasses().size());

      final TestExistenceChanges changes = populateExistingTests(testsToUpdate, mapping, oldDepdendencies);

      findAddedTests(oldDepdendencies, changes);
      return changes;
   }

   private TestExistenceChanges populateExistingTests(final TestSet testsToUpdate, final ModuleClassMapping mapping,
         final Map<ChangedEntity, Map<ChangedEntity, Set<String>>> oldDepdendencies) throws FileNotFoundException, IOException, XmlPullParserException {
      final TestExistenceChanges changes = new TestExistenceChanges();

      for (final Entry<ChangedEntity, Set<String>> entry : testsToUpdate.entrySet()) {
         final String testClassName = entry.getKey().getJavaClazzName();
         final File testclazzFolder = getTestclazzFolder(entry);
         LOG.debug("Suche in {} Existiert: {} Ordner: {} Tests: {} ", testclazzFolder.getAbsolutePath(), testclazzFolder.exists(), testclazzFolder.isDirectory(), entry.getValue());
         if (testclazzFolder.exists()) {
            updateMethods(mapping, changes, entry, testClassName, testclazzFolder);
         } else {
            checkRemoved(oldDepdendencies, changes, entry, testClassName, testclazzFolder);
         }
      }
      return changes;
   }

   public File getTestclazzFolder(final Entry<ChangedEntity, Set<String>> entry) throws FileNotFoundException, IOException, XmlPullParserException {
      final File testclazzFolder;
      if (entry.getKey().getModule().equals("")) {
         final File xmlFileFolder = getXMLFileFolder(folders.getProjectFolder());
         testclazzFolder = new File(xmlFileFolder, entry.getKey().getJavaClazzName());
      } else {
         final File moduleFolder = new File(folders.getProjectFolder(), entry.getKey().getModule());
         final File xmlFileFolder = getXMLFileFolder(moduleFolder);
         testclazzFolder = new File(xmlFileFolder, entry.getKey().getJavaClazzName());
      }
      return testclazzFolder;
   }

   void updateMethods(final ModuleClassMapping mapping, final TestExistenceChanges changes, final Entry<ChangedEntity, Set<String>> entry, final String testClassName,
         final File testclazzFolder) {
      final Set<String> notFound = new TreeSet<>();
      notFound.addAll(entry.getValue());
      for (final File testResultFile : testclazzFolder.listFiles((FileFilter) new WildcardFileFilter("*.xml"))) {
         final String testClassName2 = testResultFile.getParentFile().getName();
         if (!testClassName2.equals(testClassName)) {
            LOG.error("Testclass " + testClassName + " != " + testClassName2);
         }
         final File parent = testResultFile.getParentFile();
         final String testMethodName = testResultFile.getName().substring(0, testResultFile.getName().length() - 4);
         final String module = mapping.getModuleOfClass(testClassName);
         updateDependenciesOnce(new ChangedEntity(testClassName, module, testMethodName), parent, mapping);
         notFound.remove(testMethodName);
      }
      LOG.debug("Removed tests: {}", notFound);
      for (final String testMethodName : notFound) {
         final ChangedEntity entity = entry.getKey().copy();
         entity.setMethod(testMethodName);
         dependencies.removeTest(entity);
         // testsToUpdate.removeTest(entry.getKey(), testMethodName);
         changes.addRemovedTest(new TestCase(testClassName, testMethodName, entry.getKey().getModule()));
      }
   }

   void checkRemoved(final Map<ChangedEntity, Map<ChangedEntity, Set<String>>> oldDepdendencies, final TestExistenceChanges changes, final Entry<ChangedEntity, Set<String>> entry,
         final String testClassName, final File testclazzFolder) {
      LOG.error("Testclass {} does not exist anymore or does not create results. Folder: {}", entry.getKey(), testclazzFolder);
      final TestCase testclass = new TestCase(testClassName, "", entry.getKey().getModule());
      boolean oldContained = false;
      for (final ChangedEntity oldTest : oldDepdendencies.keySet()) {
         if (testclass.getClazz().equals(oldTest.getClazz()) && testclass.getModule().equals(oldTest.getModule())) {
            oldContained = true;
         }
      }
      if (oldContained) {
         changes.addRemovedTest(testclass);
      } else {
         LOG.error("Test was only added incorrect, no removing necessary.");
      }
   }

   /**
    * A method is unknown if a class-wide change happened, e.g. if a new subclass is declared and because of this change a new testcase needs to be called.
    * 
    * @param oldDepdendencies
    * @param changes
    */
   private void findAddedTests(final Map<ChangedEntity, Map<ChangedEntity, Set<String>>> oldDepdendencies, final TestExistenceChanges changes) {
      for (final Map.Entry<ChangedEntity, CalledMethods> newDependency : dependencies.getDependencyMap().entrySet()) {
         // testclass -> depending class -> method
         final ChangedEntity testcase = newDependency.getKey();
         if (!oldDepdendencies.containsKey(testcase)) {
            changes.addAddedTest(testcase.onlyClazz(), testcase);
            for (final Map.Entry<ChangedEntity, Set<String>> newCallees : newDependency.getValue().getCalledMethods().entrySet()) {
               final ChangedEntity changedclass = newCallees.getKey();
               for (final String changedMethod : newCallees.getValue()) {
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
   }
}
