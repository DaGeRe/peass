package de.dagere.peass.utils;

import de.dagere.peass.TestConstants;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.DummyExecutor;
import de.dagere.peass.dependency.ExecutorCreator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.mock;

public class TestModuleFinderUtil {

    @Test
    public void testGetMavenModules() throws IOException, XmlPullParserException {
        File projectFolder = new File("..");

        ProjectModules projectModules = getGenericModules(projectFolder);
        
        Assertions.assertEquals(3, projectModules.getModules().size());
    }

    @Test
    public void testGetGradleModules() throws IOException, XmlPullParserException {
        FileUtils.deleteDirectory(TestConstants.CURRENT_FOLDER);
        FileUtils.copyDirectory(new File("src/test/resources/gradle-multimodule-subprojectexample/"), TestConstants.CURRENT_FOLDER);

        ProjectModules projectModules = getGenericModules(TestConstants.CURRENT_FOLDER);

        Assertions.assertEquals(3, projectModules.getModules().size());
    }

    @Test
    public void testGetDummyModules() throws IOException, XmlPullParserException {
        FileUtils.deleteDirectory(TestConstants.CURRENT_FOLDER);
        ProjectModules expectedProjectModules = new ProjectModules(TestConstants.CURRENT_FOLDER);
        ProjectModules projectModules;
        
        try (MockedStatic<ExecutorCreator> executorCreatorMock = Mockito.mockStatic(ExecutorCreator.class, Mockito.CALLS_REAL_METHODS)) {
            DummyExecutor dummyExecutor = mock(DummyExecutor.class);
            Mockito.when(dummyExecutor.getModules()).thenReturn(expectedProjectModules);
            
            executorCreatorMock
                    .when(() -> ExecutorCreator.createExecutor(Mockito.any(PeassFolders.class), Mockito.any(JUnitTestTransformer.class), Mockito.any(EnvironmentVariables.class)))
                    .thenReturn(dummyExecutor);

           projectModules = getGenericModules(TestConstants.CURRENT_FOLDER);
        }

        Assertions.assertEquals(expectedProjectModules, projectModules);
    }
    
    private ProjectModules getGenericModules(File projectFolder) throws XmlPullParserException, IOException {
        return ModuleFinderUtil.getGenericModules(projectFolder,new ExecutionConfig());
    }
}
