package de.peran.dependencytests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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
import de.peran.dependency.analysis.data.ChangedEntity;
import de.peran.dependency.reader.DependencyReader;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency.Testcase;
import de.peran.vcs.VersionIterator;

public class SuperclassDetectorIT {


	private static final File VERSIONS_FOLDER = new File("src/test/resources/dependencyIT");
	private static final File CURRENT = new File("target", "current");
	private static final File BASIC_STATE = new File(VERSIONS_FOLDER, "superclass_old");

	@Before
	public void initialize() throws IOException, InterruptedException {
		Assert.assertTrue(VERSIONS_FOLDER.exists());

		FileUtils.deleteDirectory(CURRENT);
		FileUtils.copyDirectory(BASIC_STATE, CURRENT);
	}

	@Test
	public void testSuperclassChange() throws IOException, InterruptedException {
		final File secondVersion = new File(VERSIONS_FOLDER, "superclass_changed");

		final Map<ChangedEntity, Set<String>> changes = new TreeMap<>();
		changes.put(new ChangedEntity("defaultpackage.NormalSuperclass", ""), new TreeSet<>());

		final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
		Mockito.when(changeManager.getChanges()).thenReturn(changes);

		final VersionIterator fakeIterator = new FakeIterator(CURRENT, Arrays.asList(secondVersion));

		final DependencyReader reader = new DependencyReader(CURRENT, new File("/dev/null"), null, fakeIterator);
		reader.readInitialVersion();
		fakeIterator.goToNextCommit();

		reader.analyseVersion(changeManager);

		System.out.println(reader.getDependencies());

		final Dependency testMe = DependencyDetectorIT.findDependency(reader.getDependencies(), "defaultpackage.NormalSuperclass", "1");
		final Testcase testcase = testMe.getTestcase().get(0);
		Assert.assertEquals("defaultpackage.TestMe", testcase.getClazz());
		Assert.assertEquals("testMe", testcase.getMethod().get(0));
	}


}
