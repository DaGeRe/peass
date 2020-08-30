package de.peass.dependencytests;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.peass.TestConstants;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependency.persistence.Version;
import de.peass.dependency.traces.ViewGenerator;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.statistics.DependencyStatisticAnalyzer;
import de.peass.utils.Constants;
import de.peass.vcs.GitUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest(GitUtils.class)
@PowerMockIgnore("javax.management.*")
public class ViewGeneratorTest {

   private static final Logger LOG = LogManager.getLogger(ViewGeneratorIT.class);

   private static final File dependencyfile = new File(ViewGeneratorIT.class.getClassLoader().getResource("viewtests/dependencies.json").getFile());
         
//         new File("src/test/resources/viewtests/dependencies.json");

   @Test
   public void testTwoVersions() throws IOException, InterruptedException, JAXBException {
      ViewGeneratorIT.init(ViewGeneratorIT.BASIC);

      final File executefile = new File(ViewGeneratorIT.VIEW_IT, "execute.json");
      final File viewFolder = new File(ViewGeneratorIT.VIEW_IT, "views");
      // final File viewFolder = Files.createTempDirectory(ViewGeneratorIT.VIEW_IT.toPath(), "views").toFile();

      final Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyfile, Dependencies.class);
      VersionComparator.setDependencies(dependencies);
      final ViewGenerator generator = new ViewGenerator(TestConstants.projectFolder, dependencies, executefile, viewFolder, 1, 60000);

      mockGitHandling();

      final String version = "000001";
      final Version testVersion = dependencies.getVersions().get(version);
      generator.processVersion(version, testVersion);

      final ExecutionData tests = Constants.OBJECTMAPPER.readValue(executefile, ExecutionData.class);

      Assert.assertEquals(1, tests.getVersions().size());
      Assert.assertEquals(1, tests.getVersions().get("000001").getTests().size());
      // Problem: Irgendwas geht noch nicht..

      // TODO It should be checked whether the whole ViewGeneration-Process works
   }

   private void mockGitHandling() throws InterruptedException, IOException {
      final PeASSFolders folders = new PeASSFolders(TestConstants.projectFolder);
      final File projectFolderTemp = new File(folders.getTempProjectFolder(), "1");
      PowerMockito.mockStatic(GitUtils.class);
      PowerMockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            FileUtils.copyDirectory(TestConstants.projectFolder, projectFolderTemp);
            return null;
         }
      }).when(GitUtils.class);
      GitUtils.clone(Mockito.any(PeASSFolders.class), Mockito.any(File.class));

      PowerMockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            FileUtils.deleteDirectory(projectFolderTemp);
            FileUtils.copyDirectory(ViewGeneratorIT.REPETITION, projectFolderTemp);
            return null;
         }
      }).when(GitUtils.class);
      GitUtils.goToTag(Mockito.eq("000001"), Mockito.any(File.class));

      PowerMockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            FileUtils.copyDirectory(ViewGeneratorIT.BASIC, projectFolderTemp);
            return null;
         }
      }).when(GitUtils.class);
      GitUtils.goToTag(Mockito.eq("000001~1"), Mockito.any(File.class));

      PowerMockito.doCallRealMethod().when(GitUtils.class);
      GitUtils.getDiff(Mockito.any(), Mockito.any());
   }

   /**
    * Writes the dependencyfile, in case it needs to be changed etc.
    * 
    * @param args
    * @throws IOException
    * @throws JsonMappingException
    * @throws JsonGenerationException
    */
   public static void main(final String[] args) throws JsonGenerationException, JsonMappingException, IOException {
      final Dependencies deps = new Dependencies();
      deps.getInitialversion().setVersion("000000");
      deps.getInitialversion().addDependency(new ChangedEntity("viewtest.TestMe", "", "test"), new ChangedEntity("viewtest.TestMe", "", "test"));
      deps.getInitialversion().addDependency(new ChangedEntity("viewtest.TestMe", "", "test"), new ChangedEntity("viewtest.TestMe$InnerClass", "", "<init>"));
      deps.getInitialversion().addDependency(new ChangedEntity("viewtest.TestMe", "", "test"), new ChangedEntity("viewtest.TestMe$InnerClass", "", "method"));

      final Version version = new Version();
      deps.getVersions().put("000001", version);
      version.getChangedClazzes().put(new ChangedEntity("viewtest.TestMe", "", "test"), new TestSet("viewtest.TestMe#test"));

      final ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
      objectMapper.writeValue(dependencyfile, deps);
   }
}
