package de.peran.dependencytests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import co.unruly.matchers.StreamMatchers;
import de.peran.dependency.ChangeManager;
import de.peran.dependency.analysis.data.ChangedEntity;
import de.peran.dependency.reader.DependencyReader;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency;
import de.peran.vcs.VersionIterator;

public class DependencyDetectorMultimoduleIT {

	private static final File VERSIONS_FOLDER = new File("src/test/resources/dependencyIT_multimodule");
	private static final File CURRENT = new File(new File("target"), "current");
	private static final File BASIC_STATE = new File(VERSIONS_FOLDER, "basic_state");

//	private DependencyManager handler;

	@Before
	public void initialize() throws IOException, InterruptedException {
		Assert.assertTrue(VERSIONS_FOLDER.exists());

		FileUtils.deleteDirectory(CURRENT);
		FileUtils.copyDirectory(BASIC_STATE, CURRENT);

//		handler = new DependencyManager(CURRENT);
//		final boolean success = handler.initialyGetTraces();
//
//		Assert.assertTrue(success);
	}

	// @org.junit.After
	// public void cleanAfterwards() throws IOException {
	// FileUtils.deleteDirectory(CURRENT);
	// // be aware: maven does not compile if a .class-file is still in the resources, since it gets identified as test
	// }

	@Test
	public void testNormalChange() throws IOException, InterruptedException {
		final File secondVersion = new File(VERSIONS_FOLDER, "normal_change");
		final File thirdVersion = new File(VERSIONS_FOLDER, "another_change");

		ChangeManager changeManager;
		{
			final Map<ChangedEntity, Set<String>> changes = new TreeMap<>();
			final TreeSet<String> methodChanges = new TreeSet<>();
			methodChanges.add("doSomething");
			changes.put(new ChangedEntity("de.dagere.base.BaseChangeable", "base-module"), methodChanges);

			changeManager = Mockito.mock(ChangeManager.class);
			Mockito.when(changeManager.getChanges()).thenReturn(changes);
		}

		final VersionIterator fakeIterator = new FakeIterator(CURRENT, Arrays.asList(secondVersion, thirdVersion));

		final DependencyReader reader = new DependencyReader(CURRENT, new File("/dev/null"), null, fakeIterator);
		final boolean success = reader.readInitialVersion();
		Assert.assertTrue(success);
		fakeIterator.goToNextCommit();

		reader.analyseVersion(changeManager);

		System.out.println(reader.getDependencies());

		final Dependency foundDependency = DependencyDetectorIT.findDependency(reader.getDependencies(), "de.dagere.base.BaseChangeable.doSomething", "1");
		testBaseChangeEffect(foundDependency);

		{
			final Map<ChangedEntity, Set<String>> changes = new TreeMap<>();
			final TreeSet<String> methodChanges = new TreeSet<>();
			methodChanges.add("doSomething");
			changes.put(new ChangedEntity("de.dagere.base.BaseChangeable", "base-module"), methodChanges);
			
			final TreeSet<String> methodChanges2 = new TreeSet<>();
			methodChanges2.add("doSomething");
			changes.put(new ChangedEntity("de.dagere.base.NextBaseChangeable", "base-module"), methodChanges2);

			changeManager = Mockito.mock(ChangeManager.class);
			Mockito.when(changeManager.getChanges()).thenReturn(changes);
		}

		fakeIterator.goToNextCommit();
		reader.analyseVersion(changeManager);

		final Dependency foundDependency2 = DependencyDetectorIT.findDependency(reader.getDependencies(), "de.dagere.base.BaseChangeable.doSomething", "2");
		testBaseChangeEffect(foundDependency2);

		final Dependency foundDependency3 = DependencyDetectorIT.findDependency(reader.getDependencies(), "de.dagere.base.NextBaseChangeable.doSomething", "2");
		Assert.assertThat(foundDependency3.getTestcase().stream(), StreamMatchers.anyMatch(
				Matchers.allOf(Matchers.hasProperty("clazz", Matchers.is("NextTest")),
						Matchers.hasProperty("method", Matchers.containsInAnyOrder("nextTestMe", "nextTestMeAlso")))));
	}
	
	

	private void testBaseChangeEffect(final Dependency foundDependency) {
		System.out.println(foundDependency.getTestcase());

		Assert.assertThat(foundDependency.getTestcase().stream(), StreamMatchers.anyMatch(
				Matchers.allOf(Matchers.hasProperty("clazz", Matchers.is("de.dagere.base.BaseTest")),
						Matchers.hasProperty("method", Matchers.containsInAnyOrder("testMeAlso", "testMe")))));

		Assert.assertThat(foundDependency.getTestcase().stream(), StreamMatchers.anyMatch(
				Matchers.allOf(Matchers.hasProperty("clazz", Matchers.is("NextTest")),
						Matchers.hasProperty("method", Matchers.containsInAnyOrder("nextTestMe")))));

		Assert.assertThat(foundDependency.getTestcase().stream(), StreamMatchers.anyMatch(
				Matchers.allOf(Matchers.hasProperty("clazz", Matchers.is("AnotherTest")),
						Matchers.hasProperty("method", Matchers.containsInAnyOrder("testMeAlso")))));
	}
}
