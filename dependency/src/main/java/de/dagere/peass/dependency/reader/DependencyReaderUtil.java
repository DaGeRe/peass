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
package de.dagere.peass.dependency.reader;

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

import de.dagere.nodeDiffDetector.data.MethodCall;
import de.dagere.nodeDiffDetector.data.TestCase;
import de.dagere.nodeDiffDetector.data.TestClazzCall;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.dependency.analysis.data.ChangeTestMapping;
import de.dagere.peass.dependency.analysis.data.TestExistenceChanges;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.changesreading.ClazzChangeData;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.utils.Constants;

/**
 * Utility function for reading dependencies
 * 
 * @author reichelt
 *
 */
public class DependencyReaderUtil {

   private static final Logger LOG = LogManager.getLogger(DependencyReaderUtil.class);

   static void removeDeletedTestcases(final CommitStaticSelection newCommitSelection, final TestExistenceChanges testExistenceChanges) {
      LOG.debug("Removed Tests: {}", testExistenceChanges.getRemovedTests());
      for (final TestCase removedTest : testExistenceChanges.getRemovedTests()) {
         LOG.debug("Remove: {}", removedTest);
         for (final Entry<MethodCall, TestSet> dependency : newCommitSelection.getChangedClazzes().entrySet()) {
            final TestSet testSet = dependency.getValue();
            if (removedTest instanceof TestMethodCall) {
               for (final Entry<TestClazzCall, Set<String>> testcase : testSet.getTestcases().entrySet()) {
                  if (testcase.getKey().getClazz().equals(removedTest.getClazz())) {
                     String method = ((TestMethodCall) removedTest).getMethod();
                     testcase.getValue().remove(method);
                  }
               }
            } else {
               TestClazzCall removeTestcase = null;
               for (final Entry<TestClazzCall, Set<String>> testcase : testSet.getTestcases().entrySet()) {
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

   static void addNewTestcases(final CommitStaticSelection newVersionInfo, final Map<MethodCall, Set<TestMethodCall>> newTestcases) {
      for (final Map.Entry<MethodCall, Set<TestMethodCall>> newTestcase : newTestcases.entrySet()) {
         final MethodCall changedClazz = newTestcase.getKey();
         TestSet testsetForChange = null;
         for (final Entry<MethodCall, TestSet> dependency : newVersionInfo.getChangedClazzes().entrySet()) {
            MethodCall dependencyChangedClazz = dependency.getKey();
            if (dependencyChangedClazz.equals(changedClazz)) {
               testsetForChange = dependency.getValue();
            }
         }
         if (testsetForChange == null) {
            testsetForChange = new TestSet();
            newVersionInfo.getChangedClazzes().put(changedClazz, testsetForChange);
         }
         for (final TestMethodCall testcase : newTestcase.getValue()) {
            testsetForChange.addTest(testcase.onlyClazz(), testcase.getMethod());
         }
      }
   }

   static CommitStaticSelection createCommitFromChangeMap(final Map<MethodCall, ClazzChangeData> changedClassNames, final ChangeTestMapping changeTestMap) {
      final CommitStaticSelection newCommitSelection = new CommitStaticSelection();
      newCommitSelection.setRunning(true);
      LOG.debug("Beginning to write");
      for (final Map.Entry<MethodCall, ClazzChangeData> changedClassName : changedClassNames.entrySet()) {
         ClazzChangeData changedClazzInsideFile = changedClassName.getValue();
         if (!changedClazzInsideFile.isOnlyMethodChange()) { // class changed as a whole
            handleWholeClassChange(changeTestMap, newCommitSelection, changedClazzInsideFile);
         } else {
            handleMethodChange(changeTestMap, newCommitSelection, changedClazzInsideFile);
         }
      }
      return newCommitSelection;

   }

   private static void handleMethodChange(final ChangeTestMapping changeTestMap, final CommitStaticSelection version, final ClazzChangeData changedClassName) {
      for (MethodCall underminedChange : changedClassName.getChanges()) {
         boolean contained = false;

         final MethodCall changedEntryFullName = new MethodCall(underminedChange.toString());
         for (final Entry<MethodCall, TestSet> currentDependency : version.getChangedClazzes().entrySet()) {
            if (currentDependency.getKey().equals(changedEntryFullName)) {
               contained = true;
            }
         }
         if (!contained) {
            final TestSet tests = new TestSet();
            if (changeTestMap.getChanges().containsKey(underminedChange)) {
               for (final TestMethodCall testClass : changeTestMap.getChanges().get(underminedChange)) {
                  tests.addTest(testClass);
               }
            }
            version.getChangedClazzes().put(changedEntryFullName, tests);
         }
      }
   }

   private static void handleWholeClassChange(final ChangeTestMapping changeTestMap, final CommitStaticSelection version, final ClazzChangeData changedClassName) {
      for (MethodCall underminedChange : changedClassName.getUniqueChanges()) {
         final TestSet tests = new TestSet();
         MethodCall realChange = underminedChange.onlyClazz();
         Set<TestMethodCall> testEntities = changeTestMap.getTests(realChange);
         if (testEntities != null) {
            for (final TestMethodCall testcase : testEntities) {
               tests.addTest(testcase);
            }
         }
         if (version.getChangedClazzes().containsKey(realChange)) {
            throw new RuntimeException("Clazz FQNs are unique in Java, but " + realChange.getJavaClazzName() + " was added twice!");
         }
         version.getChangedClazzes().put(realChange, tests);
      }
   }

   public static void write(final StaticTestSelection staticTestSelection, final File file) {
      LOG.debug("Writing to: {}", file);
      try {
         Constants.OBJECTMAPPER.writeValue(file, staticTestSelection);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   public static StaticTestSelection mergeStaticSelection(final StaticTestSelection staticTestSelection1, final StaticTestSelection staticTestSelection2,
         CommitComparatorInstance comparator) {
      final StaticTestSelection merged;
      final StaticTestSelection newer;
      if (comparator.isBefore(staticTestSelection1.getInitialcommit().getCommit(), staticTestSelection2.getInitialcommit().getCommit())) {
         merged = staticTestSelection1;
         newer = staticTestSelection2;
      } else {
         newer = staticTestSelection1;
         merged = staticTestSelection2;
      }
      LOG.debug("Merging: {}", merged.getCommits().size());

      final List<String> removableCommits = new LinkedList<>();
      String commitInBoth = null;
      final Iterator<String> iterator = newer.getCommits().keySet().iterator();
      if (iterator.hasNext()) {
         final String firstOtherCommit = iterator.next();
         for (final String commit : merged.getCommits().keySet()) {
            if (merged == null && commit.equals(firstOtherCommit) || comparator.isBefore(firstOtherCommit, commit)) {
               commitInBoth = commit;
            }
            if (commitInBoth != null) {
               removableCommits.add(commit);
            }
         }
      } else {
         return merged;
      }

      LOG.debug("Removable: " + removableCommits.size());
      for (final String commit : removableCommits) {
         LOG.debug("Removing: {}", commit);
         merged.getCommits().remove(commit);
      }
      int add = 0;
      for (final Map.Entry<String, CommitStaticSelection> newerCommit : newer.getCommits().entrySet()) {
         LOG.debug("Add: {}", newerCommit.getKey());
         add++;
         merged.getCommits().put(newerCommit.getKey(), newerCommit.getValue());
      }
      LOG.debug("Added: {} Size: {}", add, merged.getCommits().size());
      return merged;
   }
}
