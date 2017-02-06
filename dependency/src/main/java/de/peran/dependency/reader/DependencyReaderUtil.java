/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the Affero GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     Affero GNU General Public License for more details.
 *
 *     You should have received a copy of the Affero GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.peran.dependency.reader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peran.dependency.ChangedTestClassesHandler;
import de.peran.dependency.TestDependencies;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency.Testcase;

public class DependencyReaderUtil {

	private static final Logger LOG = LogManager.getLogger(DependencyReaderUtil.class);

	/**
	 * Determines the tests that may have got new dependencies, writes that changes (i.e. the tests that need to be run in that version) and re-runs the tests in order to get the updated test
	 * dependencies.
	 * 
	 * @param dependencyFile
	 * @param handler
	 * @param dependencies
	 * @param dependencyResult
	 * @param version
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static Map<String, List<String>> analyseVersion(final File dependencyFile, final ChangedTestClassesHandler handler, final TestDependencies dependencies,
			final Versiondependencies dependencyResult, final String version)
			throws IOException, InterruptedException {
		final Map<String, Set<String>> changedClassNames = handler.getChangedClassesCleaned();
		handler.saveOldClasses();

		final Map<String, Set<String>> changeTestMap = getChangeTestMap(dependencies, changedClassNames);

		final Version newVersionInfo = addVersionFromChangeMap(version, changedClassNames, changeTestMap);

		LOG.debug("Aktuallisiere Abhängigkeiten..");

		final Map<String, List<String>> testsToRun = handler.getTestsToRun(changedClassNames);
		if (testsToRun.size() > 0) {
			final Map<String, Set<String>> newTestcases = handler.updateDependencies(testsToRun);
			addNewTestcases(dependencyResult, newVersionInfo, newTestcases);

			write(dependencyResult, dependencyFile);
		}
		return testsToRun;
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

	private static void addNewTestcases(final Versiondependencies dependencyResult, final Version newVersionInfo, final Map<String, Set<String>> newTestcases) {
		for (final Map.Entry<String, Set<String>> newTestcase : newTestcases.entrySet()) {
			final String changedClazz = newTestcase.getKey();
			Dependency correctDependency = null;
			for (final Dependency dependency : newVersionInfo.getDependency()) {
				if (dependency.getChangedclass().equals(changedClazz)) {
					correctDependency = dependency;
				}
			}
			if (correctDependency == null) {
				correctDependency = new Dependency();
				correctDependency.setChangedclass(changedClazz);
				newVersionInfo.getDependency().add(correctDependency);
			}
			for (final String testcase : newTestcase.getValue()) {
				addTestcase(correctDependency, testcase);
			}
		}
		dependencyResult.getVersions().getVersion().add(newVersionInfo);
	}

	private static void addTestcase(final Dependency correctDependency, final String testcase) {
		final String testclass = testcase.substring(0, testcase.lastIndexOf("."));
		final String testmethod = testcase.substring(testcase.lastIndexOf(".") + 1);
		final Testcase testcaseObject = findOrAddTestcase(correctDependency, testclass);
		testcaseObject.getMethod().add(testmethod);
	}

	private static Version addVersionFromChangeMap(final String revision, final Map<String, Set<String>> changedClassNames, final Map<String, Set<String>> changeTestMap) {
		final Version version = new Version();
		version.setRevision(revision);
		LOG.debug("Beginne schreiben");
		// changeTestMap.keySet ist fast wie changedClassNames, bloß dass
		// Klassen ohne Abhängigkeit drin sind
		for (final Map.Entry<String, Set<String>> className : changedClassNames.entrySet()) {
			final Dependency dependency = new Dependency();
			if (className.getValue().isEmpty()) {
				dependency.setChangedclass(className.getKey());// TODO: Statt für die Klasse für alle Methoden Änderung eintragen
				if (changeTestMap.containsKey(className.getKey())) {
					for (final String testcase : changeTestMap.get(className.getKey())) {
						if (testcase.contains(".")){
							addTestcase(dependency, testcase);
						}
					}
				}
				version.getDependency().add(dependency);
			} else {
				for (final String method : className.getValue()) {
					final String changedEntryFullName = className.getKey() + "." + method;
					dependency.setChangedclass(changedEntryFullName);
					if (changeTestMap.containsKey(changedEntryFullName)) {
						for (final String testClass : changeTestMap.get(changedEntryFullName)) {
							addTestcase(dependency, testClass);
						}
					}
					version.getDependency().add(dependency);
				}

			}

		}
		System.out.println("Testrevision: " + revision);
		return version;

	}

	private static void addChangeEntry(final String fullname, final String currentTestcase, final Map<String, Set<String>> changeTestMap) {
		Set<String> changedClasses = changeTestMap.get(fullname);
		if (changedClasses == null) {
			changedClasses = new HashSet<>();
			changeTestMap.put(fullname, changedClasses);
			// TODO: Statt einfach die Klasse nehmen prüfen, ob die Methode genutzt wird
		}
		LOG.debug("Füge {} zu {} hinzu", currentTestcase, fullname);
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
	private static Map<String, Set<String>> getChangeTestMap(final TestDependencies dependencies, final Map<String, Set<String>> changedClassNames) {
		final Map<String, Set<String>> changeTestMap = new HashMap<>();
		for (final Map.Entry<String, Map<String, Set<String>>> dependencyEntry : dependencies.getDependencyMap().entrySet()) {
			final String currentTestcase = dependencyEntry.getKey();
			final Map<String, Set<String>> currentTestDependencies = dependencyEntry.getValue();

			for (final Map.Entry<String, Set<String>> changedEntry : changedClassNames.entrySet()) {
				LOG.debug("Prüfe Abhängigkeiten für {} von {}", changedEntry, currentTestcase);
				final String changedClass = changedEntry.getKey();
				if (currentTestDependencies.keySet().contains(changedClass)) {
					if (changedEntry.getValue().isEmpty()) {
						addChangeEntry(changedClass, currentTestcase, changeTestMap);
					} else {
						for (final String method : changedEntry.getValue()) {
							final String fullname = changedClass + "." + method;
							if (currentTestDependencies.get(changedClass).contains(method)) {
								addChangeEntry(fullname, currentTestcase, changeTestMap);
							}
						}
					}

				}
			}
		}
		for (final String changedClass : changedClassNames.keySet()) {
			if (!changeTestMap.containsKey(changedClass) && changedClass.toLowerCase().contains("test")) {
				changeTestMap.put(changedClass, new HashSet<>());
				changeTestMap.get(changedClass).add(changedClass);
			}
		}
		for (final Map.Entry<String, Set<String>> element : changeTestMap.entrySet()) {
			LOG.debug("Element: {} Dependencies: {} {}", element.getKey(), element.getValue().size(), element.getValue());
		}

		return changeTestMap;
	}

	public static void write(final Versiondependencies deps, final File file) {
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
