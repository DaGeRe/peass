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
package de.peass.dependency.analysis.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.changesreading.ClazzChangeData;

/**
 * Represents information about the tests and their dependencies, i.e. the classes they call.
 * 
 * @author reichelt
 *
 */
public class TestDependencies {
	
   private static final Logger LOG = LogManager.getLogger(TestDependencies.class);
   
	/**
	 * Map from testcase (package.clazz.method) to dependent class to the list of called methods of this class
	 */
	private final Map<ChangedEntity, CalledMethods> dependencyMap = new HashMap<>();
	
	public TestDependencies(){
		
	}

	public Map<ChangedEntity, CalledMethods> getDependencyMap() {
		return dependencyMap;
	}

	/**
	 * Gets the dependencies for a test, i.e. the used classes. If the test is not known yet, an empty Set is returned.
	 * 
	 * @param test
	 */
	public Map<ChangedEntity, Set<String>> getOrAddDependenciesForTest(final ChangedEntity test) {
		CalledMethods tests = dependencyMap.get(test);
		if (tests == null) {
			tests = new CalledMethods();
			dependencyMap.put(test, tests);
			final ChangedEntity onlyClass = new ChangedEntity(test.getClazz(), test.getModule());
			final HashSet<String> calledMethods = new HashSet<>();
         tests.getCalledMethods().put(onlyClass, calledMethods);
			calledMethods.add(test.getMethod());
		}
		return tests.getCalledMethods();
	}

	public void removeTest(final ChangedEntity entity) {
		dependencyMap.remove(entity);
	}

	public int size() {
		return dependencyMap.size();
	}

	public Map<ChangedEntity, Map<ChangedEntity, Set<String>>> getCopiedDependencies() {
		final Map<ChangedEntity, Map<ChangedEntity, Set<String>>> copy = new HashMap<>();
		for (final Map.Entry<ChangedEntity, CalledMethods> entry : dependencyMap.entrySet()) {
			final Map<ChangedEntity, Set<String>> dependencies = new HashMap<>();
			for (final Map.Entry<ChangedEntity, Set<String>> testcase : entry.getValue().getCalledMethods().entrySet()) {
				final Set<String> copiedMethods = new HashSet<>();
				copiedMethods.addAll(testcase.getValue());
				dependencies.put(entry.getKey(), copiedMethods);
			}
			copy.put(entry.getKey(), dependencies);
		}
		return copy;
	}

	@Override
	public String toString() {
		return dependencyMap.toString();
	}
	
	/**
    * Returns a list of all tests that changed based on given changed classes and the dependencies of the current version. So the result mapping is changedclass to a set of tests,
    * that could have been changed by this changed class.
    * 
    * @param dependencies
    * @param changes
    * @return Map from changed class to the influenced tests
    */
   public ChangeTestMapping getChangeTestMap(final Map<ChangedEntity, ClazzChangeData> changes) {
      final ChangeTestMapping changeTestMap = new ChangeTestMapping();
      for (final Entry<ChangedEntity, CalledMethods> dependencyEntry : dependencyMap.entrySet()) {
         final ChangedEntity currentTestcase = dependencyEntry.getKey();
         final CalledMethods currentTestDependencies = dependencyEntry.getValue();
         for (ClazzChangeData changedEntry : changes.values()) {
            for (ChangedEntity change : changedEntry.getChanges()) {
               final ChangedEntity changedClass = change.onlyClazz();
               final Set<ChangedEntity> calledClasses = currentTestDependencies.getCalledClasses();
               if (calledClasses.contains(changedClass)) {
                  addCall(changeTestMap, currentTestcase, currentTestDependencies, changedEntry, change, changedClass);
               }
            }
         }
      }
      for (final Map.Entry<ChangedEntity, Set<ChangedEntity>> element : changeTestMap.getChanges().entrySet()) {
         LOG.debug("Element: {} Dependencies: {} {}", element.getKey(), element.getValue().size(), element.getValue());
      }

      return changeTestMap;
   }

   public void addCall(final ChangeTestMapping changeTestMap, final ChangedEntity currentTestcase, final CalledMethods currentTestDependencies, ClazzChangeData changedEntry,
         ChangedEntity change, final ChangedEntity changedClass) {
      if (!changedEntry.isOnlyMethodChange()) {
         addChangeEntry(changedClass, currentTestcase, changeTestMap);
      } else {
         String method = change.getMethod();
         final Map<ChangedEntity, Set<String>> calledMethods = currentTestDependencies.getCalledMethods();
         final Set<String> calledMethodsInChangeClass = calledMethods.get(changedClass);
         final int parameterIndex = method.indexOf("("); // TODO Parameter korrekt prüfen
         final String methodWithoutParameters = parameterIndex != -1 ? method.substring(0, parameterIndex) : method;
         if (calledMethodsInChangeClass.contains(methodWithoutParameters)) {
            final ChangedEntity classWithMethod = new ChangedEntity(changedClass.getClazz(), changedClass.getModule(), method);
            addChangeEntry(classWithMethod, currentTestcase, changeTestMap);
         }
      }
   }
   
   private static void addChangeEntry(final ChangedEntity changedFullname, final ChangedEntity currentTestcase, final ChangeTestMapping changeTestMap) {
      Set<ChangedEntity> changedClasses = changeTestMap.getChanges().get(changedFullname);
      if (changedClasses == null) {
         changedClasses = new HashSet<>();
         changeTestMap.getChanges().put(changedFullname, changedClasses);
         // TODO: Statt einfach die Klasse nehmen prüfen, ob die Methode genutzt wird
      }
      LOG.debug("Füge {} zu {} hinzu", currentTestcase, changedFullname);
      changedClasses.add(currentTestcase);
   }

}
