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

import de.peran.dependency.analysis.CalledMethodLoader;
import de.peran.dependency.analysis.data.CalledMethods;
import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependency.analysis.data.TestExistenceChanges;
import de.peran.dependency.analysis.data.TestDependencies;
import de.peran.dependency.analysis.data.TestSet;
import de.peran.dependency.execution.MavenKiekerTestExecutor;
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
	 * Creates a new ChangeTestClassesHandler for the given folder with the
	 * given groupId and projectId. The groupId and projectId are needed to
	 * determine where the results are afterwards.
	 * 
	 * @param projectFolder
	 */
	public DependencyManager(final File projectFolder) {
		super(projectFolder);
	}

	public DependencyManager(final File projectFolder, File moduleFolder) {
		super(projectFolder, moduleFolder);
	}

	public TestDependencies getDependencyMap() {
		return dependencies;
	}

	public boolean initialyGetTraces() throws IOException, InterruptedException {
		if (resultsFolder.exists()) {
			FileUtils.deleteDirectory(resultsFolder);
		}

		executor.executeAllTests(new File(logFolder, "init_log.txt"));

		loadClasses();
		if (resultsFolder.exists()) {
			final Collection<File> xmlFiles = FileUtils.listFiles(resultsFolder, new WildcardFileFilter("*.xml"), TrueFileFilter.INSTANCE);
			LOG.debug("Initial test execution finished, starting result collection, analyzing {} files", xmlFiles.size());
			for (final File testResultFile : xmlFiles) {
				final String testClassName = testResultFile.getParentFile().getName();
				final String testMethodName = testResultFile.getName().substring(0, testResultFile.getName().length() - 4); // remove
																															// .xml
				final File parent = testResultFile.getParentFile();
				updateDependenciesOnce(testClassName, testMethodName, parent);
			}
			LOG.debug("Result collection finished");

			resultsFolder.renameTo(new File(resultsFolder.getParentFile(), "initialresults_kieker"));
			return true;
		} else {
			final File testSourceFolder = new File(moduleFolder, "src/test");
			final Collection<File> javaTestFiles = FileUtils.listFiles(testSourceFolder, new WildcardFileFilter("*test*.java", IOCase.INSENSITIVE), TrueFileFilter.INSTANCE);
			if (javaTestFiles.size() > 0) {
				LOG.debug("No result data available - error occured?");
				return false;
			} else {
				LOG.debug("No result data available, but no test-classes existing - so it is ok.");
				return true;
			}
		}
	}

	/**
	 * Loads which classes exist
	 */
	private void loadClasses() {
		existingClasses = new LinkedList<>();
		final Collection<File> files = FileUtils.listFiles(moduleFolder, new WildcardFileFilter("*.java"), TrueFileFilter.INSTANCE);
		for (final File file : files) {
			final String filename = file.getName().substring(0, file.getName().lastIndexOf("."));
			existingClasses.add(filename);
		}
	}

	/**
	 * Updates Dependencies of the given testClassName and the given
	 * testMethodName based upon the file where the kieker-results are stored
	 * 
	 * @param testClassName
	 * @param testMethodName
	 * @param parent
	 */
	public void updateDependenciesOnce(final String testClassName, final String testMethodName, final File parent) {
		LOG.debug("Parent: " + parent);
		final File kiekerResultFolder = findKiekerFolder(testMethodName, parent);

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

		final File kiekerOutputFile = new File(logFolder, "ausgabe_kieker.txt");

		try {
			System.setOut(new PrintStream(kiekerOutputFile));
			System.setErr(new PrintStream(kiekerOutputFile));
			final Map<String, Set<String>> calledClasses = new CalledMethodLoader(kiekerResultFolder).getCalledMethods();

			for (final Iterator<String> iterator = calledClasses.keySet().iterator(); iterator.hasNext();) {
				final String clazzAndMethod = iterator.next();
				final String onlyClass = clazzAndMethod.substring(clazzAndMethod.lastIndexOf(".") + 1);
				if (!existingClasses.contains(onlyClass)) { // Removes e.g inner
															// classes named
															// like
															// Classname$InnerClass
					iterator.remove();
				}
			}

			LOG.debug("Test: {} {}", testClassName, testMethodName);
			LOG.debug("Kieker: {} Dependencies: {}", kiekerResultFolder.getAbsolutePath(), calledClasses.size());
			addDependencies(testClassName, testMethodName, calledClasses);
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
	 * @param calledClasses
	 *            Map from name of the called class to the methods of the class
	 *            that are called
	 */
	public void addDependencies(final String testClassName, final String testMethodName, final Map<String, Set<String>> calledClasses) {
		final Map<String, Set<String>> testDependencies = dependencies.getDependenciesForTest(testClassName + "." + testMethodName);
		testDependencies.putAll(calledClasses);
	}

	/**
	 * Updates the dependencies of the current version by running each testclass
	 * once. The testcases, that have been added in this version, are returned
	 * (since they can not be determined from the old dependency information or
	 * the svn diff directly). TODO: What if testcases are removed?
	 * 
	 * @param testsToUpdate
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public TestExistenceChanges updateDependencies(final TestSet testsToUpdate, final String version) throws IOException {
		final Map<String, Map<String, Set<String>>> oldDepdendencies = dependencies.getCopiedDependencies();
		// Remove all old dependencies where changes happened, because they may
		// have been deleted
		for (final Entry<String, List<String>> className : testsToUpdate.entrySet()) {
			for (final String method : className.getValue()) {
				dependencies.getDependencyMap().remove(className + "." + method);
			}
		}

		truncateKiekerResults();

		LOG.debug("Führe Tests neu aus für Abhängigkeiten-Aktuallisierung, Ergebnisordner: {}", resultsFolder);
		final TestSet tests = new TestSet();
		for (final String clazzname : testsToUpdate.getClasses()) {
			tests.addTest(clazzname.intern(), "");
		}
		executeKoPeMeKiekerRun(tests, version);

		LOG.debug("Beginne Abhängigkeiten-Aktuallisierung für {} Klassen", testsToUpdate.getClasses().size());

		final TestExistenceChanges changes = new TestExistenceChanges();

		loadClasses();
		final File xmlFileFolder = getXMLFileFolder();
		if (xmlFileFolder == null) {
			LOG.error("No pom - version is not analysable");
			return new TestExistenceChanges();
		}
		for (final Entry<String, List<String>> entry : testsToUpdate.entrySet()) {
			final String testClassName = entry.getKey();
			final File testclazzFolder = new File(xmlFileFolder, entry.getKey());
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
					updateDependenciesOnce(testClassName, testMethodName, parent);
					notFound.remove(testMethodName);
				}
				LOG.debug("Removed tests: {}", notFound);
				for (final String testMethodName : notFound) {
					dependencies.removeTest(testClassName, testMethodName);
					testsToUpdate.removeTest(testClassName, testMethodName);
					changes.addRemovedTest(new TestCase(testClassName, testMethodName));
				}
			} else {
				LOG.error("Testklasse {} existiert nicht mehr bzw. liefert keine Ergebnisse mehr (JUnit 4 statt 3?).", entry.getKey());
				changes.addRemovedTest(new TestCase(entry.getKey(), ""));
			}
		}

		/**
		 * In this part, the method is unknown if a class-wide change happened,
		 * p.e. if a new subclass is declared and because of this change a new
		 * testcase needs to be called.
		 */
		// final Map<String, Set<String>> newTestCases = new TreeMap<>(); // Map
		// from changedclass to a set of testcases that may have changed
		for (final Map.Entry<String, CalledMethods> entry : dependencies.getDependencyMap().entrySet()) { // testclass
																											// ->
																											// depending
																											// class
																											// ->
																											// method
			final String testcase = entry.getKey();
			if (!oldDepdendencies.containsKey(testcase)) {
				for (final Map.Entry<String, Set<String>> changeEntry : entry.getValue().getCalledMethods().entrySet()) {
					final String changedclass = changeEntry.getKey();
					for (final String changedMethod : changeEntry.getValue()) {
						// Since the testcase is new, is is always caused
						// primarily by a change of the test class, and not of
						// any other changed class
						changes.addAddedTest(changedclass, changedMethod, testcase);
					}
				}
			}
		}
		return changes;
	}

	private void truncateKiekerResults() {
		LOG.debug("Truncating: {}", resultsFolder.getAbsolutePath());
		try {
			FileUtils.deleteDirectory(resultsFolder);
		} catch (final IOException e) {
			e.printStackTrace();
			if (resultsFolder.exists()) {
				try {
					FileUtils.deleteDirectory(resultsFolder);
				} catch (final IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	/**
	 * Returns the tests that need to be run in the current version based on the
	 * given changes, i.e. the given changed classes and changed methods
	 * 
	 * @param map
	 *            from changed classes to changed methods (or, if class changed
	 *            as a whole, an empty set)
	 * @return Map from testclasses to the test methods of the class that need
	 *         to be run
	 */
	public TestSet getTestsToRun(final Map<String, Set<String>> changedClassNames) {
		final TestSet testsToRun = new TestSet();
		for (final String testName : changedClassNames.keySet()) {
			if (testName.toLowerCase().contains("test")) {
				testsToRun.addTest(testName, null);
			}
		}
		for (final Map.Entry<String, CalledMethods> testDependencies : dependencies.getDependencyMap().entrySet()) {
			final Set<String> currentTestDependencies = testDependencies.getValue().getCalledClasses();
			for (final String changedClass : changedClassNames.keySet()) {
				LOG.trace("Prüfe Abhängigkeiten für {} von {}", changedClass, testDependencies.getKey());
				LOG.trace("Abhängig: {} Abhängig von Testklasse: {}", currentTestDependencies.contains(changedClass), changedClass.equals(testDependencies.getKey()));
				if (currentTestDependencies.contains(changedClass)) {
					LOG.info("Test " + testDependencies.getKey() + " benötigt geänderte Klasse " + changedClass);
					final String testClassName = testDependencies.getKey().substring(0, testDependencies.getKey().lastIndexOf('.'));
					final String testMethodName = testDependencies.getKey().substring(testDependencies.getKey().lastIndexOf('.') + 1);
					testsToRun.addTest(testClassName, testMethodName);
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
