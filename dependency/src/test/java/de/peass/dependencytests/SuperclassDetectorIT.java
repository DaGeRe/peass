package de.peass.dependencytests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import de.peass.dependency.ChangeManager;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.changesreading.ClazzChangeData;
import de.peass.dependency.reader.DependencyReader;
import de.peass.dependencytests.helper.FakeFileIterator;
import de.peass.vcs.VersionIterator;

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
	public void testSuperclassChange() throws IOException, InterruptedException, XmlPullParserException {
		final File secondVersion = new File(VERSIONS_FOLDER, "superclass_changed");

		final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
		changes.put(new ChangedEntity("defaultpackage.NormalSuperclass", ""), new ClazzChangeData("defaultpackage.NormalSuperclass", false));

		final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
		Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

		final VersionIterator fakeIterator = new FakeFileIterator(CURRENT, Arrays.asList(secondVersion));

		final DependencyReader reader = new DependencyReader(CURRENT, new File("/dev/null"), null, fakeIterator, 5000, changeManager);
		reader.readInitialVersion();
		fakeIterator.goToNextCommit();

		reader.analyseVersion(changeManager);

		System.out.println(reader.getDependencies());

		final TestSet testMe = DependencyDetectorTestUtil.findDependency(reader.getDependencies(), "defaultpackage.NormalSuperclass", DependencyDetectorIT.VERSION_1);
		final TestCase testcase = testMe.getTests().iterator().next();
		Assert.assertEquals("defaultpackage.TestMe", testcase.getClazz());
		Assert.assertEquals("testMe", testcase.getMethod());
	}


}
