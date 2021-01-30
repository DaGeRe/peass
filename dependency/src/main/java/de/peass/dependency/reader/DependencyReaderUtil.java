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
package de.peass.dependency.reader;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.analysis.data.ChangeTestMapping;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestExistenceChanges;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.changesreading.ClazzChangeData;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.Version;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.Constants;

/**
 * Utility function for reading dependencies
 * 
 * @author reichelt
 *
 */
public class DependencyReaderUtil {

   private static final Logger LOG = LogManager.getLogger(DependencyReaderUtil.class);

   static void removeDeletedTestcases(final Version newVersionInfo, final TestExistenceChanges testExistenceChanges) {
      LOG.debug("Removed Tests: {}", testExistenceChanges.getRemovedTests());
      for (final TestCase removedTest : testExistenceChanges.getRemovedTests()) {
         LOG.debug("Remove: {}", removedTest);
         for (final Entry<ChangedEntity, TestSet> dependency : newVersionInfo.getChangedClazzes().entrySet()) {
            final TestSet testSet = dependency.getValue();
            if (removedTest.getMethod().length() > 0) {
               for (final Entry<ChangedEntity, Set<String>> testcase : testSet.getTestcases().entrySet()) {
                  if (testcase.getKey().getJavaClazzName().equals(removedTest.getClazz())) {
                     testcase.getValue().remove(removedTest.getMethod());
                  }
               }
            } else {
               ChangedEntity removeTestcase = null;
               for (final Entry<ChangedEntity, Set<String>> testcase : testSet.getTestcases().entrySet()) {
                  if (testcase.getKey().getClazz().equals(removedTest.getClazz())) {
                     removeTestcase = testcase.getKey();
                  }
               }
               // Tests may not be changed by a class change - so a test needs only to be removed, if he is there
               if (removeTestcase != null) {
                  testSet.removeTest(removeTestcase);
               }
            }
         }
      }
   }

   static void addNewTestcases(final Version newVersionInfo, final Map<ChangedEntity, Set<ChangedEntity>> newTestcases) {
      for (final Map.Entry<ChangedEntity, Set<ChangedEntity>> newTestcase : newTestcases.entrySet()) {
         final ChangedEntity changedClazz = newTestcase.getKey();
         TestSet testsetForChange = null;
         for (final Entry<ChangedEntity, TestSet> dependency : newVersionInfo.getChangedClazzes().entrySet()) {
            ChangedEntity dependencyChangedClazz = dependency.getKey();
            if (dependencyChangedClazz.equals(changedClazz)) {
               testsetForChange = dependency.getValue();
            }
         }
         if (testsetForChange == null) {
            testsetForChange = new TestSet();
            newVersionInfo.getChangedClazzes().put(changedClazz, testsetForChange);
         }
         for (final ChangedEntity testcase : newTestcase.getValue()) {
            testsetForChange.addTest(testcase.onlyClazz(), testcase.getMethod());
         }
      }
   }

   static Version createVersionFromChangeMap(final String revision, final Map<ChangedEntity, ClazzChangeData> changedClassNames, final ChangeTestMapping changeTestMap) {
      final Version newVersionInfo = new Version();
      newVersionInfo.setRunning(true);
      LOG.debug("Beginne schreiben");
      // changeTestMap.keySet ist fast wie changedClassNames, bloß dass
      // Klassen ohne Abhängigkeit drin sind
      for (final Map.Entry<ChangedEntity, ClazzChangeData> changedClassName : changedClassNames.entrySet()) {
         ClazzChangeData changedClazzInsideFile = changedClassName.getValue();
         if (!changedClazzInsideFile.isOnlyMethodChange()) { // class changed as a whole
            handleWholeClassChange(changeTestMap, newVersionInfo, changedClazzInsideFile);
         } else {
            handleMethodChange(changeTestMap, newVersionInfo, changedClazzInsideFile);
         }
      }
      LOG.debug("Testrevision: " + revision);
      return newVersionInfo;

   }

   private static void handleMethodChange(final ChangeTestMapping changeTestMap, final Version version, final ClazzChangeData changedClassName) {
      for (ChangedEntity underminedChange : changedClassName.getChanges()) {
         boolean contained = false;

         final ChangedEntity changedEntryFullName = new ChangedEntity(underminedChange.getJavaClazzName(), underminedChange.getModule(), underminedChange.getMethod());
         for (final Entry<ChangedEntity, TestSet> currentDependency : version.getChangedClazzes().entrySet()) {
            if (currentDependency.getKey().equals(changedEntryFullName)) {
               contained = true;
            }
         }
         if (!contained) {
            final TestSet tests = new TestSet();
            if (changeTestMap.getChanges().containsKey(underminedChange)) {
               for (final ChangedEntity testClass : changeTestMap.getChanges().get(underminedChange)) {
                  tests.addTest(testClass.onlyClazz(), testClass.getMethod());
               }
            }
            version.getChangedClazzes().put(changedEntryFullName, tests);
         }
      }
   }

   private static void handleWholeClassChange(final ChangeTestMapping changeTestMap, final Version version, final ClazzChangeData changedClassName) {
      for (ChangedEntity underminedChange : changedClassName.getUniqueChanges()) {
         final TestSet tests = new TestSet();
         ChangedEntity realChange = underminedChange.onlyClazz();
         Set<ChangedEntity> testEntities = changeTestMap.getTests(realChange);
         if (testEntities != null) {
            for (final ChangedEntity testcase : testEntities) {
               if (testcase.getMethod() != null) {
                  tests.addTest(testcase.onlyClazz(), testcase.getMethod());
               } else {
                  throw new RuntimeException("Testcase without method detected: " + testcase + " Dependency: " + tests);
               }
            }
         }
         if (version.getChangedClazzes().containsKey(realChange)) {
            throw new RuntimeException("Clazz FQNs are unique in Java, but " + realChange.getJavaClazzName() + " was added twice!");
         }
         version.getChangedClazzes().put(realChange, tests);
      }
   }

   public static void write(final Dependencies deps, final File file) {
      LOG.debug("Schreibe in: {}", file);
      try {
         Constants.OBJECTMAPPER.writeValue(file, deps);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   public static Dependencies mergeDependencies(final Dependencies deps1, final Dependencies deps2) {
      final Dependencies merged;
      final Dependencies newer;
      if (VersionComparator.isBefore(deps1.getInitialversion().getVersion(), deps2.getInitialversion().getVersion())) {
         merged = deps1;
         newer = deps2;
      } else {
         newer = deps1;
         merged = deps2;
      }
      LOG.debug("Merging: {}", merged.getVersions().size());

      final List<String> removableVersion = new LinkedList<>();
      String mergeVersion = null;
      final Iterator<String> iterator = newer.getVersions().keySet().iterator();
      if (iterator.hasNext()) {
         final String firstOtherVersion = iterator.next();
         for (final String version : merged.getVersions().keySet()) {
            if (merged == null && version.equals(firstOtherVersion) || VersionComparator.isBefore(firstOtherVersion, version)) {
               mergeVersion = version;
            }
            if (mergeVersion != null) {
               removableVersion.add(version);
            }
         }
      } else {
         return merged;
      }

      // if (mergeVersion == null) {
      // LOG.error("Version {} was newer than newest version of old dependencies - merging not possible", firstOtherVersion);
      // return null;
      // }
      LOG.debug("Removable: " + removableVersion.size());
      for (final String version : removableVersion) {
         LOG.debug("Removing: {}", version);
         merged.getVersions().remove(version);
      }
      int add = 0;
      for (final Map.Entry<String, Version> newerVersion : newer.getVersions().entrySet()) {
         LOG.debug("Add: {}", newerVersion.getKey());
         add++;
         merged.getVersions().put(newerVersion.getKey(), newerVersion.getValue());
      }
      LOG.debug("Added: {} Size: {}", add, merged.getVersions().size());
      return merged;
   }
}
