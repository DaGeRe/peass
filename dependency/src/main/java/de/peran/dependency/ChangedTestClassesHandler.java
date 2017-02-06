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
package de.peran.dependency;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ParseException;

import de.peran.dependency.analysis.CalledMethodLoader;
import de.peran.dependency.analysis.FileComparisonUtil;
import de.peran.dependency.changes.ClazzChangeData;
import de.peran.dependency.changes.VersionDiff;
import de.peran.vcs.GitDiffLoader;
import de.peran.vcs.SVNDiffLoader;
import de.peran.vcs.VersionControlSystem;

public class ChangedTestClassesHandler extends TestRunHandler {

	private static final Logger LOG = LogManager.getLogger(ChangedTestClassesHandler.class);

	private final TestDependencies dependencies = new TestDependencies();
	private final File lastSourcesFolder;
	private final VersionControlSystem vcs;

	/**
	 * Creates a new ChangeTestClassesHandler for the given folder with the given groupId and projectId. The groupId and projectId are needed to determine where the results are afterwards.
	 * 
	 * @param projectFolder
	 * @param groupId
	 * @param artifactId
	 */
	public ChangedTestClassesHandler(final File projectFolder) {
		super(projectFolder);
		lastSourcesFolder = new File(projectFolder, "lastSources");
		vcs = VersionControlSystem.getVersionControlSystem(projectFolder);
	}

	public TestDependencies getDependencyMap() {
		return dependencies;
	}

	public boolean initialyGetTraces() throws IOException, InterruptedException {
		final MavenKiekerTestExecutor generator = new MavenKiekerTestExecutor(projectFolder, resultsFolder, new File(projectFolder, "peran_logs"));

		if (resultsFolder.exists()) {
			FileUtils.deleteDirectory(resultsFolder);
		}

		generator.executeTests();

		if (resultsFolder.exists()) {
			LOG.debug("Initial test execution finished, starting result collection");
			for (final File testResultFile : FileUtils.listFiles(resultsFolder, new WildcardFileFilter("*.xml"), TrueFileFilter.INSTANCE)) {
				final String testClassName = testResultFile.getParentFile().getName();
				final String testMethodName = testResultFile.getName().substring(0, testResultFile.getName().length() - 4); // remove .xml
				final File parent = testResultFile.getParentFile();
				updateDependenciesOnce(testClassName, testMethodName, parent);
			}
			LOG.debug("Result collection finished");
			return true;
		} else {
			LOG.debug("No result data available - error occured?");
			return false;
		}
	}

	private void updateDependenciesOnce(final String testClassName, final String testMethodName, final File parent) {
		LOG.debug("Parent: " + parent);
		final File[] listFiles = parent.listFiles(new FileFilter() {
			@Override
			public boolean accept(final File pathname) {
				return pathname.getName().matches("[0-9]*");
			}
		});
		LOG.debug("Kieker-Dateien: {}", listFiles.length);
		final File kiekerAllFolder = listFiles[0];
		LOG.debug("Analysiere Ordner: {} {}", kiekerAllFolder.getAbsolutePath(), testMethodName);
		final File kiekerNextFolder = new File(kiekerAllFolder, testMethodName);
		final File kiekerResultFolder = kiekerNextFolder.listFiles()[0];
		LOG.debug("Test: " + testMethodName);

		final PrintStream out = System.out;
		final PrintStream err = System.err;

		final File kiekerOutputFile = new File(projectFolder.getParent(), "ausgabe_kieker.txt");
		Map<String, Set<String>> calledClasses = null;
		try {
			System.setOut(new PrintStream(kiekerOutputFile));
			System.setErr(new PrintStream(kiekerOutputFile));
			calledClasses = new CalledMethodLoader(kiekerResultFolder).getCalledMethods();
			for (final Iterator<String> iterator = calledClasses.keySet().iterator(); iterator.hasNext();) {
				final String clazz = iterator.next();
				final String onlyClass = clazz.substring(clazz.lastIndexOf(".") + 1);
				final Collection<File> files = FileUtils.listFiles(projectFolder, new WildcardFileFilter(onlyClass + "*"), TrueFileFilter.INSTANCE);
				if (files.size() == 0) {
					iterator.remove();
				}
			}
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			System.setOut(out);
			System.setErr(err);
		}

		LOG.debug("Test: {} {}", testClassName, testMethodName);
		LOG.debug("Kieker: {} Dependencies: {}", kiekerResultFolder.getAbsolutePath(), calledClasses.size());
		final Map<String, Set<String>> dependencies = this.dependencies.getDependenciesForTest(testClassName + "." + testMethodName);
		dependencies.putAll(calledClasses);
	}

	/**
	 * Updates the dependencies of the current version by running each testclass once. The testcases, that have been added in this version, are returned (since they can not be determined from the old
	 * dependency information or the svn diff directly).
	 * 
	 * @param testsToUpdate
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public Map<String, Set<String>> updateDependencies(final Map<String, List<String>> testsToUpdate) throws InterruptedException {
		final Map<String, Map<String, Set<String>>> oldDepdendencies = dependencies.getCopiedDependencies();

		truncateKiekerResults();

		LOG.debug("Führe Tests neu aus für Abhängigkeiten-Aktuallisierung, Ergebnisordner: {}", resultsFolder);
		try {
			final TestSet tests = new TestSet();
			for (final String clazzname : testsToUpdate.keySet()) {
				tests.addTest(clazzname, "");
			}
			executeKoPeMeKiekerRun(tests);
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		LOG.debug("Beginne Abhängigkeiten-Aktuallisierung für {} Klassen", testsToUpdate.keySet().size());

		// Remove all old dependencies, because they may have been deleted
		for (final Entry<String, List<String>> className : testsToUpdate.entrySet()) {
			for (final String method : className.getValue()) {
				dependencies.getDependencyMap().remove(className + "." + method);
			}
		}

		for (final Entry<String, List<String>> entry : testsToUpdate.entrySet()) {
			final File testclazzFolder = new File(xmlFileFolder, entry.getKey());
			LOG.debug("Suche in {} Existiert: {} Ordner: {}", testclazzFolder, testclazzFolder.exists(), testclazzFolder.isDirectory());
			if (testclazzFolder.exists() && entry.getValue().size() > 0) {
				for (final File testResultFile : testclazzFolder.listFiles((FileFilter) new WildcardFileFilter("*.xml"))) {
					if (testResultFile.exists()) {
						final String testClassName = testResultFile.getParentFile().getName();
						final File parent = testResultFile.getParentFile();
						final String methodName = testResultFile.getName().substring(0, testResultFile.getName().length() - 4);
						updateDependenciesOnce(testClassName, methodName, parent);
					}
				}
			} else {
				LOG.debug("Suche in {} Existiert: {} Ordner: {}", testclazzFolder, testclazzFolder.exists(), testclazzFolder.isDirectory());
				if (testclazzFolder.exists()) {
					for (final File testResultFile : FileUtils.listFiles(testclazzFolder, new WildcardFileFilter("*.xml"), TrueFileFilter.INSTANCE)) {
						final String testClassName = testResultFile.getParentFile().getName();
						final String testName = testResultFile.getName().substring(0, testResultFile.getName().length() - 4); // .xml entfernen
						final File parent = testResultFile.getParentFile();
						updateDependenciesOnce(testClassName, testName, parent);
					}
				} else {
					LOG.error("Testklasse {} existiert nicht mehr bzw. liefert keine Ergebnisse mehr (JUnit 4 statt 3?).", entry.getKey());
				}
			}
		}

		final Map<String, Set<String>> newTestCases = new TreeMap<>(); // Map from changedclass to a set of testcases that may have changed
		for (final Map.Entry<String, Map<String, Set<String>>> entry : dependencies.getDependencyMap().entrySet()) {
			final String testcase = entry.getKey();
			if (!oldDepdendencies.containsKey(testcase)) {
				for (final Map.Entry<String, Set<String>> changedClass : entry.getValue().entrySet()) {
					Set<String> testcaseSet = newTestCases.get(changedClass.getKey());
					if (testcaseSet == null) {
						testcaseSet = new TreeSet<>();
						newTestCases.put(changedClass.getKey(), testcaseSet);
					}
					testcaseSet.add(testcase);
				}
			}
		}
		return newTestCases;
	}

	private void truncateKiekerResults() {
		try {
			FileUtils.deleteDirectory(resultsFolder);
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			if (resultsFolder.exists()) {
				try {
					FileUtils.deleteDirectory(resultsFolder);
				} catch (final IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}

	public void saveOldClasses() {
		try {
			if (lastSourcesFolder.exists()) {
				FileUtils.deleteDirectory(lastSourcesFolder);
			}
			lastSourcesFolder.mkdir();
			FileUtils.copyDirectory(new File(projectFolder, "src"), new File(lastSourcesFolder, "main"));
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns a set of the full qualified names of all classes that have been changed in the current revision.
	 * 
	 * @return full qualified names of all classes that have been changed in the current revision.
	 */
	public Set<String> getChangedClasses() {
		final VersionDiff diff;
		if (vcs.equals(VersionControlSystem.SVN)) {
			diff = new SVNDiffLoader().getChangedClasses(projectFolder);
		} else if (vcs.equals(VersionControlSystem.GIT)) {
			diff = GitDiffLoader.getChangedClasses(projectFolder);
		} else {
			throw new RuntimeException(".git or .svn not there - Can only happen if .git or .svn is deleted between constructor and method call ");
		}

		LOG.info("Klassen, die geändert wurden: " + diff.getChangedClasses().size());
		final Set<String> classNames = new TreeSet<>();
		for (final String className : diff.getChangedClasses()) {
			String javaClassName = className.replace(".java", ""); // src/test/java entfernen
			LOG.debug(className + " " + javaClassName);
			javaClassName = javaClassName.replace("src/main/java/", "").replace("src/test/java/", "").replace("src/test/", "").replace("src/java/", "").replace('/', '.');
			LOG.debug(javaClassName);
			classNames.add(javaClassName);
		}
		return classNames;
	}

	/**
	 * Returns all changed classes with the corresponding changed methods. If the set of a class is empty, the whole class was changed and all tests using any method of the class need to be
	 * re-evaluated.
	 * 
	 * @return
	 */
	public Map<String, Set<String>> getChangedClassesCleaned() {
		final Map<String, Set<String>> changedClassesMethods = new TreeMap<>();
		final Set<String> changedClasses = getChangedClasses();
		LOG.debug("Vor dem Bereinigen: {}", changedClasses);
		if (lastSourcesFolder.exists()) {
			for (final Iterator<String> clazzIterator = changedClasses.iterator(); clazzIterator.hasNext();) {
				final String clazz = clazzIterator.next();
				final String onlyClassName = clazz.substring(clazz.lastIndexOf(".") + 1);
				try {
					final File src = new File(projectFolder, "src");
					LOG.debug("Suche nach {} in {}", clazz, src);
					final File newFile = FileUtils.listFiles(src, new WildcardFileFilter(onlyClassName + "*"), TrueFileFilter.INSTANCE).iterator().next();
					final File oldFile = FileUtils.listFiles(lastSourcesFolder, new WildcardFileFilter(onlyClassName + "*"), TrueFileFilter.INSTANCE).iterator().next();
					LOG.info("Vergleiche {}", newFile, oldFile);
					final ClazzChangeData changeData = FileComparisonUtil.getChangedMethods(newFile, oldFile);
					if (!changeData.isChange()) {
						clazzIterator.remove();
						LOG.debug("Dateien gleich: {}", clazz);
					} else {
						if (changeData.isOnlyMethodChange()) {
							changedClassesMethods.put(clazz, changeData.getChangedMethods());
						} else {
							changedClassesMethods.put(clazz, new HashSet<>());
						}
					}
				} catch (final NoSuchElementException nse) {
					LOG.info("Class did not exist before: {}", clazz);
					changedClassesMethods.put(clazz, new HashSet<>());
				} catch (final ParseException pe) {
					LOG.info("Class is unparsable for java parser, so to be sure it is added to the changed classes: {}", clazz);
					changedClassesMethods.put(clazz, new HashSet<>());
					pe.printStackTrace();
				} catch (final IOException e) {
					LOG.info("Class is unparsable for java parser, so to be sure it is added to the changed classes: {}", clazz);
					changedClassesMethods.put(clazz, new HashSet<>());
					e.printStackTrace();
				}
			}
		} else {
			LOG.info("Kein Ordner für alte Dateien vorhanden");
		}
		LOG.debug("Nach dem Bereinigen: {}", changedClassesMethods);

		return changedClassesMethods;
	}

	/**
	 * Returns the mapping from changed classes to the methods, that have changed. If the method-set of a class is empty, all methods may have changed.
	 * 
	 * @return
	 */
	public Map<String, Set<String>> getChangedMethods() {
		final Map<String, Set<String>> changedMethods = new TreeMap<>();
		final Set<String> changedClasses = getChangedClasses();
		for (final String clazz : changedClasses) {
			try {
				final File newFile = FileUtils.listFiles(new File(projectFolder, "src"), new WildcardFileFilter(clazz + "*"), TrueFileFilter.INSTANCE).iterator().next();
				final File oldFile = FileUtils.listFiles(lastSourcesFolder, new WildcardFileFilter(clazz + "*"), TrueFileFilter.INSTANCE).iterator().next();
				LOG.info("Vergleiche {}", newFile, oldFile);
				// CompilationUnit
			} catch (final NoSuchElementException nse) {
				changedMethods.put(clazz, new HashSet<>());
			}
		}

		return changedMethods;
	}

	/**
	 * 
	 * @return Map from testclasses to the test methods of the class that need to be run
	 */
	public Map<String, List<String>> getTestsToRun(final Map<String, Set<String>> changedClassNames) {
		final Map<String, List<String>> testsToRun = new TreeMap<>();
		for (final String testName : changedClassNames.keySet()) {
			if (testName.toLowerCase().contains("test")) {
				testsToRun.put(testName, new LinkedList<>());
			}
		}
		for (final Map.Entry<String, Map<String, Set<String>>> testDependencies : dependencies.getDependencyMap().entrySet()) {
			final Set<String> currentTestDependencies = testDependencies.getValue().keySet();
			for (final String changedClass : changedClassNames.keySet()) {
				LOG.trace("Prüfe Abhängigkeiten für {} von {}", changedClass, testDependencies.getKey());
				LOG.trace("Abhängig: {} Abhängig von Testklasse: {}", currentTestDependencies.contains(changedClass), changedClass.equals(testDependencies.getKey()));
				if (currentTestDependencies.contains(changedClass)) {
					LOG.info("Test " + testDependencies.getKey() + " benötigt geänderte Klasse " + changedClass);
					final String testClassName = testDependencies.getKey().substring(0, testDependencies.getKey().lastIndexOf('.'));
					final String testMethodName = testDependencies.getKey().substring(testDependencies.getKey().lastIndexOf('.') + 1);
					// testDependencies.getKey().replace('.', '#');
					List<String> currentMethods = testsToRun.get(testClassName);
					if (currentMethods == null) {
						currentMethods = new LinkedList<>();
						testsToRun.put(testClassName, currentMethods);
					}
					currentMethods.add(testMethodName);
					break;
				}
			}
		}
		return testsToRun;
	}
}
