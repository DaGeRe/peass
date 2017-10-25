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
package de.peran.dependency.reader;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peran.dependency.analysis.data.CalledMethods;
import de.peran.dependency.analysis.data.ChangeTestMapping;
import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependency.analysis.data.TestExistenceChanges;
import de.peran.dependency.analysis.data.TestDependencies;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency.Testcase;

/**
 * Utility function for reading dependencies
 * @author reichelt
 *
 */
public class DependencyReaderUtil {

	private static final Logger LOG = LogManager.getLogger(DependencyReaderUtil.class);

	static void removeDeletedTestcases(final Version newVersionInfo, final TestExistenceChanges testExistenceChanges) {
		for (final TestCase removedTest : testExistenceChanges.getRemovedTests()) {
			for (final Dependency dependency : newVersionInfo.getDependency()) {
				if (removedTest.getMethod().length() > 0) {
					for (final Testcase testcase : dependency.getTestcase()) {
						testcase.getMethod().remove(removedTest.getMethod());
					}
				} else {
					Testcase removeTestcase = null;
					for (final Testcase testcase : dependency.getTestcase()) {
						if (testcase.getClazz().equals(removedTest.getClazz())) {
							removeTestcase = testcase;
						}
					}
					dependency.getTestcase().remove(removeTestcase);
				}
			}
		}
	}

	public static Testcase findOrAddTestcase(final Dependency dependency, final String testclass) {
		Testcase tc = null;
		for (final Testcase current : dependency.getTestcase()) {
			if (current.getClazz().equals(testclass)) {
				tc = current;
				break;
			}
		}
		if (tc == null) {
			tc = new Testcase();
			tc.setClazz(testclass);
			dependency.getTestcase().add(tc);
		}
		return tc;
	}

	static void addNewTestcases(final Version newVersionInfo, final Map<String, Set<String>> newTestcases) {
		for (final Map.Entry<String, Set<String>> newTestcase : newTestcases.entrySet()) {
			final String changedClazz = newTestcase.getKey();
			Dependency correctDependency = null;
			for (final Dependency dependency : newVersionInfo.getDependency()) {
				if (dependency.getChangedclass().equals(changedClazz)) {
					correctDependency = dependency;
					// correctDependency.setChangedclass(changedClazz);
					// findOrAddTestcase(correctDependency, testclass)
					// correctDependency.getTestcase().add(dependency.getTestcase().get(0));
					// correctDependency = dependency;
				}
			}
			if (correctDependency == null) {
				correctDependency = new Dependency();
				correctDependency.setChangedclass(changedClazz);
				newVersionInfo.getDependency().add(correctDependency);
			}
			// addTestcase(correctDependency, newTestcase.getKey());
			for (final String testcase : newTestcase.getValue()) {
				addTestcase(correctDependency, testcase);
			}
		}
	}

	private static void addTestcase(final Dependency correctDependency, final String testcase) {
		final String testclass = testcase.substring(0, testcase.lastIndexOf("."));
		final String testmethod = testcase.substring(testcase.lastIndexOf(".") + 1);
		final Testcase testcaseObject = findOrAddTestcase(correctDependency, testclass);
		testcaseObject.getMethod().add(testmethod);
	}

	static Version createVersionFromChangeMap(final String revision, final Map<String, Set<String>> changedClassNames, final ChangeTestMapping changeTestMap) {
		final Version version = new Version();
		version.setVersion(revision);
		LOG.debug("Beginne schreiben");
		// changeTestMap.keySet ist fast wie changedClassNames, bloß dass
		// Klassen ohne Abhängigkeit drin sind
		for (final Map.Entry<String, Set<String>> changedClassName : changedClassNames.entrySet()) {
			if (changedClassName.getValue().isEmpty()) { // class changed as a whole
				handleWholeClassChange(changeTestMap, version, changedClassName);
			} else {
				handleMethodChange(changeTestMap, version, changedClassName);
			}
		}
		System.out.println("Testrevision: " + revision);
		return version;

	}

	private static void handleMethodChange(final ChangeTestMapping changeTestMap, final Version version, final Map.Entry<String, Set<String>> changedClassName) {
		for (final String method : changedClassName.getValue()) {
			final String changedEntryFullName = changedClassName.getKey() + "." + method;
			boolean contained = false;
			for (final Dependency currentDependency : version.getDependency()) {
				if (currentDependency.getChangedclass().equals(changedEntryFullName)) {
					contained = true;
				}
			}
			if (!contained) {
				final Dependency dependency = new Dependency();
				dependency.setChangedclass(changedEntryFullName);
				if (changeTestMap.getChanges().containsKey(changedEntryFullName)) {
					for (final String testClass : changeTestMap.getChanges().get(changedEntryFullName)) {
						addTestcase(dependency, testClass);
					}
				}
				version.getDependency().add(dependency);
			}
		}
	}

	private static void handleWholeClassChange(final ChangeTestMapping changeTestMap, final Version version, final Map.Entry<String, Set<String>> changedClassName) {
		final Dependency dependency = new Dependency();
		dependency.setChangedclass(changedClassName.getKey());
		if (changeTestMap.getChanges().containsKey(changedClassName.getKey())) {
			for (final String testcase : changeTestMap.getChanges().get(changedClassName.getKey())) {
				if (testcase.contains(".")) {
					addTestcase(dependency, testcase);
				} else {
					throw new RuntimeException("Testcase without method detected: " + testcase + " " + dependency);
				}
			}
		} 
		version.getDependency().add(dependency);
	}

	private static void addChangeEntry(final String changedFullname, final String currentTestcase, final Map<String, Set<String>> changeTestMap) {
		Set<String> changedClasses = changeTestMap.get(changedFullname);
		if (changedClasses == null) {
			changedClasses = new HashSet<>();
			changeTestMap.put(changedFullname, changedClasses);
			// TODO: Statt einfach die Klasse nehmen prüfen, ob die Methode genutzt wird
		}
		LOG.debug("Füge {} zu {} hinzu", currentTestcase, changedFullname);
		changedClasses.add(currentTestcase);
	}

	/**
	 * Returns a list of all tests that changed based on given changed classes and the dependencies of the current version. So the result mapping is changedclass to a set of tests, that could have
	 * been changed by this changed class.
	 * 
	 * @param dependencies
	 * @param changedClassNames
	 * @return Map from changed class to the influenced tests
	 */
	static ChangeTestMapping getChangeTestMap(final TestDependencies dependencies, final Map<String, Set<String>> changedClassNames) {
		final ChangeTestMapping changeTestMap = new ChangeTestMapping();
		for (final Entry<String, CalledMethods> dependencyEntry : dependencies.getDependencyMap().entrySet()) {
			final String currentTestcase = dependencyEntry.getKey();
			final CalledMethods currentTestDependencies = dependencyEntry.getValue();

			for (final Map.Entry<String, Set<String>> changedEntry : changedClassNames.entrySet()) {
				LOG.debug("Prüfe Abhängigkeiten für {} von {}", changedEntry, currentTestcase);
				final String changedClass = changedEntry.getKey();
				Set<String> calledClasses = currentTestDependencies.getCalledClasses();
				if (calledClasses.contains(changedClass)) {
					if (changedEntry.getValue().isEmpty()) {
						addChangeEntry(changedClass, currentTestcase, changeTestMap.getChanges());
					} else {
						for (final String method : changedEntry.getValue()) {
							final String changedFullname = changedClass + "." + method;
							if (currentTestDependencies.getCalledMethods().get(changedClass).contains(method)) {
								addChangeEntry(changedFullname, currentTestcase, changeTestMap.getChanges());
							}
						}
					}

				}
			}
		}
//		for (final String changedClass : changedClassNames.keySet()) {
//			if (!changeTestMap.getChanges().containsKey(changedClass) && changedClass.toLowerCase().contains("test")) {
//				changeTestMap.addAddedTest(changedClass);
//			}
//		}
		for (final Map.Entry<String, Set<String>> element : changeTestMap.getChanges().entrySet()) {
			LOG.debug("Element: {} Dependencies: {} {}", element.getKey(), element.getValue().size(), element.getValue());
		}

		return changeTestMap;
	}

	public static void write(final Versiondependencies deps, final File file) {
		// Collections.sort(deps.getInitialversion().getInitialdependency(), new Comparator<Initialdependency>() {
		//
		// @Override
		// public int compare(Initialdependency o1, Initialdependency o2) {
		// return o1.getTestclass().compareTo(o2.getTestclass());
		// }
		// });
		// for (Initialdependency dependency : deps.getInitialversion().getInitialdependency()){
		// Collections.sort(dependency.getDependentclass());
		// }

		JAXBContext jaxbContext;
		try {
			LOG.debug("Schreibe in: {}", file);
			jaxbContext = JAXBContext.newInstance(Versiondependencies.class);
			final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			jaxbMarshaller.marshal(deps, file);
		} catch (final JAXBException e) {
			e.printStackTrace();
		}
	}
}
