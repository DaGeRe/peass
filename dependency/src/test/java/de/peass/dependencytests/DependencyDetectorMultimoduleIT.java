package de.peass.dependencytests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import co.unruly.matchers.StreamMatchers;
import de.peass.dependency.ChangeManager;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.ClazzChangeData;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.InitialDependency;
import de.peass.dependency.reader.DependencyReader;
import de.peass.vcs.VersionIterator;

public class DependencyDetectorMultimoduleIT {
   
   private static final Logger LOG = LogManager.getLogger(DependencyDetectorMultimoduleIT.class);

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
	public void testNormalChange() throws IOException, InterruptedException, XmlPullParserException {
		final File secondVersion = new File(VERSIONS_FOLDER, "normal_change");
		final File thirdVersion = new File(VERSIONS_FOLDER, "another_change");

		ChangeManager changeManager;
		{
			final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
			final ClazzChangeData methodChanges = new ClazzChangeData("de.dagere.base.BaseChangeable");
			methodChanges.getChangedMethods().add("doSomething");
			changes.put(new ChangedEntity("de.dagere.base.BaseChangeable", "base-module"), methodChanges);

			changeManager = Mockito.mock(ChangeManager.class);
			Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);
		}

		final VersionIterator fakeIterator = new FakeIterator(CURRENT, Arrays.asList(secondVersion, thirdVersion));

		final DependencyReader reader = new DependencyReader(CURRENT, new File("/dev/null"), null, fakeIterator, 5000, changeManager);
		final boolean success = reader.readInitialVersion();
		Assert.assertTrue(success);
		fakeIterator.goToNextCommit();

		reader.analyseVersion(changeManager);
		
		LOG.debug(reader.getDependencies().getInitialversion().getInitialDependencies());
		final InitialDependency dependency = reader.getDependencies().getInitialversion().getInitialDependencies().get(new ChangedEntity("AnotherTest", "using-module", "testMe"));
		LOG.debug(dependency.getEntities());
		Assert.assertThat(dependency.getEntities(), IsCollectionContaining.hasItem(new ChangedEntity("de.dagere.base.BaseChangeable", "base-module", "doSomething")));
		

		final TestSet foundDependency = DependencyDetectorIT.findDependency(reader.getDependencies(), "base-module§de.dagere.base.BaseChangeable#doSomething", DependencyDetectorIT.VERSION_1);
		testBaseChangeEffect(foundDependency);

		{
			final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
			final ClazzChangeData methodChanges = new ClazzChangeData("de.dagere.base.BaseChangeable");
			methodChanges.getChangedMethods().add("doSomething");
			changes.put(new ChangedEntity("de.dagere.base.BaseChangeable", "base-module"), methodChanges);
			
			final ClazzChangeData methodChanges2 = new ClazzChangeData("de.dagere.base.NextBaseChangeable");
			methodChanges2.getChangedMethods().add("doSomething");
			changes.put(new ChangedEntity("de.dagere.base.NextBaseChangeable", "base-module"), methodChanges2);

			changeManager = Mockito.mock(ChangeManager.class);
			Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);
		}

		fakeIterator.goToNextCommit();
		reader.analyseVersion(changeManager);

		final TestSet foundDependency2 = DependencyDetectorIT.findDependency(reader.getDependencies(), "base-module§de.dagere.base.BaseChangeable#doSomething", DependencyDetectorIT.VERSION_2);
		testBaseChangeEffect(foundDependency2);

		final TestSet foundDependency3 = DependencyDetectorIT.findDependency(reader.getDependencies(), "base-module§de.dagere.base.NextBaseChangeable#doSomething", DependencyDetectorIT.VERSION_2);
		Assert.assertThat(foundDependency3.getTests().stream(), StreamMatchers.anyMatch(
				Matchers.allOf(Matchers.hasProperty("clazz", Matchers.is("NextTest")),
						Matchers.hasProperty("method", Matchers.isOneOf("nextTestMe", "nextTestMeAlso")))));
	}
	
	@Test
	public void test() {
	   final TestSet tests = new TestSet();
	   tests.addTest(new TestCase("de.dagere.base.BaseTest#testMeAlso"));
	   tests.addTest(new TestCase("de.dagere.base.BaseTest#testMe"));
	   
	   Assert.assertThat(tests.getTests().stream(), StreamMatchers.anyMatch(
            Matchers.allOf(Matchers.hasProperty("clazz", Matchers.is("de.dagere.base.BaseTest")),
                  Matchers.hasProperty("method", Matchers.isOneOf("testMe", "testMeAlso"))
            )));
	}

	private void testBaseChangeEffect(final TestSet foundDependency) {
	   
		System.out.println(foundDependency.getTestcases());

		Assert.assertThat(foundDependency.getTests().stream(), StreamMatchers.anyMatch(
            Matchers.allOf(Matchers.hasProperty("clazz", Matchers.is("de.dagere.base.BaseTest")),
                  Matchers.hasProperty("method", Matchers.isOneOf("testMe", "testMeAlso"))
            )));
		
		Assert.assertThat(foundDependency.getTests().stream(), StreamMatchers.anyMatch(
            Matchers.allOf(Matchers.hasProperty("clazz", Matchers.is("NextTest")),
                  Matchers.hasProperty("method", Matchers.is("nextTestMe"))
            )));
		
		Assert.assertThat(foundDependency.getTests().stream(), StreamMatchers.anyMatch(
            Matchers.allOf(Matchers.hasProperty("clazz", Matchers.is("AnotherTest")),
                  Matchers.hasProperty("method", Matchers.isOneOf("testMeAlso"))
            )));

//		Assert.assertThat(foundDependency.getTests().stream(), StreamMatchers.anyMatch(
//				Matchers.allOf(Matchers.hasProperty("clazz", Matchers.is("NextTest")),
//						Matchers.hasProperty("method", Matchers.containsInAnyOrder("nextTestMe")))));
//
//		Assert.assertThat(foundDependency.getTests().stream(), StreamMatchers.anyMatch(
//				Matchers.allOf(Matchers.hasProperty("clazz", Matchers.is("AnotherTest")),
//						Matchers.hasProperty("method", Matchers.containsInAnyOrder("testMeAlso")))));
	}
}
