package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.xml.bind.JAXBException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.analysis.data.VersionDiff;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependencytests.helper.FakeFileIterator;

public class TestContinuousDependencyReader {

   public static final File CURRENT = new File("target", "current");

   private static final File dependencyFile = new File("target", "dependencies.json");

   @BeforeEach
   public void cleanDependencies() {
      dependencyFile.delete();
      Assert.assertFalse(dependencyFile.exists());
   }

   @Test
   public void testDependencReading() throws JsonParseException, JsonMappingException, JAXBException, IOException, InterruptedException, XmlPullParserException {
      FakeFileIterator iterator = createFakeIterator();
      iterator.goToFirstCommit();

      ContinuousDependencyReader reader = new ContinuousDependencyReader(iterator.getTag(), CURRENT, dependencyFile);
      Dependencies dependencies = reader.getDependencies(iterator, "");

      Assert.assertTrue(dependencyFile.exists());
      MatcherAssert.assertThat(dependencies.getVersions(), Matchers.aMapWithSize(1));
      MatcherAssert.assertThat(dependencies.getVersions().get("000002"), Matchers.notNullValue());
      final TestSet testSet = getTestset(dependencies);
      Assert.assertEquals(new TestCase("defaultpackage.TestMe#testMe"), testSet.getTests().toArray()[0]);
      
   }

   private TestSet getTestset(Dependencies dependencies) {
      final TestSet testSet = dependencies.getVersions().get("000002")
            .getChangedClazzes()
            .get(new ChangedEntity("defaultpackage.NormalDependency", "", ""));
      return testSet;
   }

   private FakeFileIterator createFakeIterator() {
      FakeFileIterator baseIterator = new FakeFileIterator(CURRENT,
            Arrays.asList(new File[] {
                  new File("../dependency/src/test/resources/dependencyIT/basic_state"),
                  new File("../dependency/src/test/resources/dependencyIT/changed_class") }));
      FakeFileIterator spy = Mockito.spy(baseIterator);
      final VersionDiff diff = new VersionDiff(Arrays.asList(new File[] { CURRENT }), CURRENT);
      diff.addChange("src/main/java/defaultpackage/NormalDependency.java");
      
      Mockito.doReturn(diff).when(spy).getChangedClasses(Mockito.any(), Mockito.any(), Mockito.any());
      return spy;
   }
}
