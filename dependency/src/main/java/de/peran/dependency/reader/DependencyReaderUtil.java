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
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peran.dependency.analysis.data.CalledMethods;
import de.peran.dependency.analysis.data.ChangeTestMapping;
import de.peran.dependency.analysis.data.ChangedEntity;
import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependency.analysis.data.TestDependencies;
import de.peran.dependency.analysis.data.TestExistenceChanges;
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

	public static Testcase findOrAddTestcase(final Dependency dependency, final String testclass, String module) {
		Testcase tc = null;
		for (final Testcase current : dependency.getTestcase()) {
			if (current.getClazz().equals(testclass) && current.getModule().equals(module)) {
				tc = current;
				break;
			}
		}
		if (tc == null) {
			tc = new Testcase();
			tc.setClazz(testclass);
			tc.setModule(module);
			dependency.getTestcase().add(tc);
		}
		return tc;
	}

	static void addNewTestcases(final Version newVersionInfo, final Map<ChangedEntity, Set<ChangedEntity>> newTestcases) {
		for (final Map.Entry<ChangedEntity, Set<ChangedEntity>> newTestcase : newTestcases.entrySet()) {
			final ChangedEntity changedClazz = newTestcase.getKey();
			Dependency correctDependency = null;
			for (final Dependency dependency : newVersionInfo.getDependency()) {
				final String dependencyCangedClass = dependency.getChangedclass();
				if (dependencyCangedClass.equals(changedClazz.getJavaClazzName())) {
					correctDependency = dependency;
				}
			}
			if (correctDependency == null) {
				correctDependency = new Dependency();
				correctDependency.setModule(changedClazz.getModule());
				correctDependency.setChangedclass(changedClazz.getJavaClazzName());
				newVersionInfo.getDependency().add(correctDependency);
			}
			for (final ChangedEntity testcase : newTestcase.getValue()) {
				final ChangedEntity methodEntity = testcase.copy();
				methodEntity.setMethod(testcase.getMethod());
				addTestcase(correctDependency, methodEntity);
			}
		}
	}

	private static void addTestcase(final Dependency correctDependency, final ChangedEntity testcase) {
		final String testclass = testcase.getClazz();
		final String testmethod = testcase.getMethod();
		final Testcase testcaseObject = findOrAddTestcase(correctDependency, testclass, testcase.getModule());
		testcaseObject.getMethod().add(testmethod);
	}

	static Version createVersionFromChangeMap(final String revision, final Map<ChangedEntity, Set<String>> changedClassNames, final ChangeTestMapping changeTestMap) {
		final Version version = new Version();
		version.setVersion(revision);
		LOG.debug("Beginne schreiben");
		// changeTestMap.keySet ist fast wie changedClassNames, bloß dass
		// Klassen ohne Abhängigkeit drin sind
		for (final Map.Entry<ChangedEntity, Set<String>> changedClassName : changedClassNames.entrySet()) {
			if (changedClassName.getValue().isEmpty()) { // class changed as a whole
				handleWholeClassChange(changeTestMap, version, changedClassName.getKey());
			} else {
				handleMethodChange(changeTestMap, version, changedClassName);
			}
		}
		System.out.println("Testrevision: " + revision);
		return version;

	}

	private static void handleMethodChange(final ChangeTestMapping changeTestMap, final Version version, final Map.Entry<ChangedEntity, Set<String>> changedClassName) {
		for (final String method : changedClassName.getValue()) {
			final ChangedEntity underminedChange = changedClassName.getKey().copy();
			underminedChange.setMethod(method);
			boolean contained = false;
			final String changedEntryFullName = changedClassName.getKey().getJavaClazzName() + "." + method;
			for (final Dependency currentDependency : version.getDependency()) {
				if (currentDependency.getChangedclass().equals(changedEntryFullName)) {
					contained = true;
				}
			}
			if (!contained) {
				final Dependency dependency = new Dependency();
				dependency.setChangedclass(changedEntryFullName);
				dependency.setModule(changedClassName.getKey().getModule());
				if (changeTestMap.getChanges().containsKey(underminedChange)) {
					for (final ChangedEntity testClass : changeTestMap.getChanges().get(underminedChange)) {
						addTestcase(dependency, testClass);
					}
				}
				version.getDependency().add(dependency);
			}
		}
	}

	private static void handleWholeClassChange(final ChangeTestMapping changeTestMap, final Version version, final ChangedEntity changedClassName) {
		final Dependency dependency = new Dependency();
		dependency.setChangedclass(changedClassName.getJavaClazzName());
		dependency.setModule(changedClassName.getModule());
		if (changeTestMap.getChanges().containsKey(changedClassName)) {
			for (final ChangedEntity testcase : changeTestMap.getChanges().get(changedClassName)) {
				if (testcase.getMethod() != null) {
					addTestcase(dependency, testcase);
				} else {
					throw new RuntimeException("Testcase without method detected: " + testcase + " Dependency: " + dependency);
				}
			}
		} 
		version.getDependency().add(dependency);
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

	/**
	 * Returns a list of all tests that changed based on given changed classes and the dependencies of the current version. So the result mapping is changedclass to a set of tests, that could have
	 * been changed by this changed class.
	 * 
	 * @param dependencies
	 * @param changes
	 * @return Map from changed class to the influenced tests
	 */
	static ChangeTestMapping getChangeTestMap(final TestDependencies dependencies, final Map<ChangedEntity, Set<String>> changes) {
		final ChangeTestMapping changeTestMap = new ChangeTestMapping();
		for (final Entry<ChangedEntity, CalledMethods> dependencyEntry : dependencies.getDependencyMap().entrySet()) {
			final ChangedEntity currentTestcase = dependencyEntry.getKey();
			final CalledMethods currentTestDependencies = dependencyEntry.getValue();

			for (final Map.Entry<ChangedEntity, Set<String>> changedEntry : changes.entrySet()) {
//				LOG.debug("Prüfe Abhängigkeiten für {} von {}", changedEntry, currentTestcase);
				final ChangedEntity changedClass = changedEntry.getKey();
				final Set<ChangedEntity> calledClasses = currentTestDependencies.getCalledClasses();
				if (calledClasses.contains(changedClass)) {
					if (changedEntry.getValue().isEmpty()) {
						addChangeEntry(changedClass, currentTestcase, changeTestMap);
					} else {
						for (final String method : changedEntry.getValue()) {
							final Set<String> dependentClassesOfTest = currentTestDependencies.getCalledMethods().get(changedClass);
							if (dependentClassesOfTest.contains(method)) {
								final ChangedEntity classWithMethod = new ChangedEntity(changedClass.getClazz(), changedClass.getModule(), method);
								addChangeEntry(classWithMethod, currentTestcase, changeTestMap);
							}
						}
					}
				}
			}
		}
		for (final Map.Entry<ChangedEntity, Set<ChangedEntity>> element : changeTestMap.getChanges().entrySet()) {
			LOG.debug("Element: {} Dependencies: {} {}", element.getKey(), element.getValue().size(), element.getValue());
		}

		return changeTestMap;
	}

	public static void write(final Versiondependencies deps, final File file) {
		try {
			LOG.debug("Schreibe in: {}", file);
			final JAXBContext jaxbContext = JAXBContext.newInstance(Versiondependencies.class);
			final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			jaxbMarshaller.marshal(deps, file);
		} catch (final JAXBException e) {
			e.printStackTrace();
		}
	}
	
	public static void write(final Version deps, final File file) {
      try {
         final JAXBContext jaxbContext = JAXBContext.newInstance(Version.class);
         final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
         jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
         
         final QName qName = new QName("com.codenotfound.jaxb.root", "root");
         final JAXBElement<Version> root = new JAXBElement<>(qName, Version.class, deps);
         
         jaxbMarshaller.marshal(root, file);
      } catch (final JAXBException e) {
         e.printStackTrace();
      }
   }
}
