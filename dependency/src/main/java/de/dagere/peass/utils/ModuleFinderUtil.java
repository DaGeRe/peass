package de.dagere.peass.utils;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.ExecutorCreator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.TestTransformer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;

public class ModuleFinderUtil {

    public static ProjectModules getGenericModules(final File projectFolder, final ExecutionConfig config) throws IOException, XmlPullParserException {
        PeassFolders folders = new PeassFolders(projectFolder);
        TestTransformer testTransformer = ExecutorCreator.createTestTransformer(folders, config, new KiekerConfig(true));
        TestExecutor testExecutor = ExecutorCreator.createExecutor(folders, testTransformer, new EnvironmentVariables());

        return testExecutor.getModules();
    }
}
