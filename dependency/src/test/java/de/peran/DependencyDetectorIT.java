package de.peran;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import de.peran.dependency.ChangeManager;
import de.peran.dependency.DependencyManager;
import de.peran.dependency.reader.DependencyReader;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency.Testcase;
import de.peran.vcs.VersionIterator;

public class DependencyDetectorIT {

	static class FakeIterator extends VersionIterator {
		List<File> commits;

		public FakeIterator(final List<File> commits) {
			super(CURRENT);
			this.commits = commits;
		}

		int tag = 0;

		@Override
		public int getSize() {
			return commits.size();
		}

		@Override
		public String getTag() {
			return "" + tag;
		}

		@Override
		public boolean hasNextCommit() {
			return tag < commits.size() + 1;
		}

		@Override
		public boolean goToNextCommit() {
			tag++;
			try {
				FileUtils.deleteDirectory(CURRENT);
				FileUtils.copyDirectory(commits.get(tag - 1), CURRENT);
			} catch (final IOException e) {
				e.printStackTrace();
			}
			return true;
		}

		@Override
		public boolean goToFirstCommit() {
			tag = 0;
			return true;
		}

		@Override
		public boolean goTo0thCommit() {
			throw new RuntimeException("Not implemented on purpose.");
		}
	}

	private static final File VERSIONS_FOLDER = new File("src/test/resources/dependencyIT");
	private static final File CURRENT = new File(VERSIONS_FOLDER, "current");
	private static final File BASIC_STATE = new File(VERSIONS_FOLDER, "basic_state");

	private DependencyManager handler;

	@Before
	public void initialize() throws IOException, InterruptedException {
		Assert.assertTrue(VERSIONS_FOLDER.exists());

		FileUtils.deleteDirectory(CURRENT);
		FileUtils.copyDirectory(BASIC_STATE, CURRENT);

		handler = new DependencyManager(CURRENT);
		final boolean success = handler.initialyGetTraces();

		Assert.assertTrue(success);
	}

	// @org.junit.After
	// public void cleanAfterwards() throws IOException {
	// FileUtils.deleteDirectory(CURRENT);
	// // be aware: maven does not compile if a .class-file is still in the resources, since it gets identified as test
	// }

	@Test
	public void testNormalChange() throws IOException, InterruptedException {
		final File secondVersion = new File(VERSIONS_FOLDER, "normal_change");

		final Map<String, Set<String>> changes = new TreeMap<>();
		final TreeSet<String> methodChanges = new TreeSet<>();
		methodChanges.add("executeThing");
		changes.put("defaultpackage.NormalDependency", methodChanges);

		final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
		Mockito.when(changeManager.getChanges()).thenReturn(changes);

		final VersionIterator fakeIterator = new FakeIterator(Arrays.asList(secondVersion));

		final DependencyReader reader = new DependencyReader(CURRENT, new File("/dev/null"), null, fakeIterator);
		reader.readInitialVersion();
		fakeIterator.goToNextCommit();

		reader.analyseVersion(changeManager);

		System.out.println(reader.getDependencies());

		final Dependency testMe = findDependency(reader.getDependencies(), "defaultpackage.NormalDependency.executeThing", "1");
		final Testcase testcase = testMe.getTestcase().get(0);
		Assert.assertEquals("defaultpackage.TestMe", testcase.getClazz());
		Assert.assertEquals("testMe", testcase.getMethod().get(0));
	}

	private Dependency findDependency(final Versiondependencies dependencies, final String changedClass, final String version) {

		Version secondVersionDependencies = null;
		for (final Version candidate : dependencies.getVersions().getVersion()) {
			if (candidate.getVersion().equals(version)) {
				secondVersionDependencies = candidate;
			}
		}

		Assert.assertEquals(version, secondVersionDependencies.getVersion());
		Dependency testMe = null;
		for (final Dependency candidate : secondVersionDependencies.getDependency()) {
			if (candidate.getChangedclass().equals(changedClass)) {
				testMe = candidate;
			}
		}
		return testMe;
	}

	@Test
	public void testAddedClass() throws IOException, InterruptedException {
		final File secondVersion = new File(VERSIONS_FOLDER, "added_class");

		final Map<String, Set<String>> changes = new TreeMap<>();
		changes.put("defaultpackage.NormalDependency", new TreeSet<>());
		changes.put("defaultpackage.TestMeAlso", new TreeSet<>());

		final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
		Mockito.when(changeManager.getChanges()).thenReturn(changes);

		final VersionIterator fakeIterator = new FakeIterator(Arrays.asList(secondVersion));

		final DependencyReader reader = new DependencyReader(CURRENT, new File("/dev/null"), null, fakeIterator);
		reader.readInitialVersion();
		fakeIterator.goToNextCommit();

		reader.analyseVersion(changeManager);

		System.out.println(reader.getDependencies());

		final Dependency testMeAlso = findDependency(reader.getDependencies(), "defaultpackage.TestMeAlso", "1");

		System.out.println(testMeAlso);
		Assert.assertEquals("defaultpackage.TestMeAlso", testMeAlso.getChangedclass());
		Assert.assertEquals("defaultpackage.TestMeAlso", testMeAlso.getTestcase().get(0).getClazz());
	}
	
	@Test
	public void testClassChange() throws IOException, InterruptedException {
		final File secondVersion = new File(VERSIONS_FOLDER, "changed_class");

		final Map<String, Set<String>> changes = new TreeMap<>();
		changes.put("defaultpackage.NormalDependency", new TreeSet<>());

		final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
		Mockito.when(changeManager.getChanges()).thenReturn(changes);

		final VersionIterator fakeIterator = new FakeIterator(Arrays.asList(secondVersion));

		final DependencyReader reader = new DependencyReader(CURRENT, new File("/dev/null"), null, fakeIterator);
		reader.readInitialVersion();
		fakeIterator.goToNextCommit();

		reader.analyseVersion(changeManager);

		System.out.println(reader.getDependencies());

		final Dependency testMe = findDependency(reader.getDependencies(), "defaultpackage.NormalDependency", "1");

		System.out.println(testMe);
		Assert.assertEquals("defaultpackage.NormalDependency", testMe.getChangedclass());
	}

	/**
	 * Tests removal of a method. In the first version, the method should not be called (but the other method of TestMe should be called, since the class interface changed). In the second version, the
	 * changes should only influence TestMe.testMe, not TestMe.removeMe.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testRemoval() throws IOException, InterruptedException {
		final File secondVersion = new File(VERSIONS_FOLDER, "removed_method");
		final File thirdVersion = new File(VERSIONS_FOLDER, "removed_method_change");

		final Map<String, Set<String>> changes = new TreeMap<>();
		changes.put("defaultpackage.TestMe", new TreeSet<>());

		final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
		Mockito.when(changeManager.getChanges()).thenReturn(changes);

		final VersionIterator fakeIterator = new FakeIterator(Arrays.asList(secondVersion, thirdVersion));

		final DependencyReader reader = new DependencyReader(CURRENT, new File("/dev/null"), null, fakeIterator);
		reader.readInitialVersion();

		fakeIterator.goToNextCommit();
		reader.analyseVersion(changeManager);
		System.out.println(reader.getDependencies());
		
		Assert.assertEquals(1, reader.getDependencies().getVersions().getVersion().get(0).getDependency().size());
		
		fakeIterator.goToNextCommit();
		reader.analyseVersion(changeManager);

		System.out.println(reader.getDependencies());
		
		final Dependency testMe = findDependency(reader.getDependencies(), "defaultpackage.TestMe", "2");
		
		Assert.assertEquals(1, testMe.getTestcase().size());
		Assert.assertEquals("defaultpackage.TestMe", testMe.getTestcase().get(0).getClazz());
		Assert.assertEquals(1, testMe.getTestcase().get(0).getMethod().size());
		Assert.assertEquals("testMe", testMe.getTestcase().get(0).getMethod().get(0));
	}

	@Test
	public void testPackageChange() {

	}
}
