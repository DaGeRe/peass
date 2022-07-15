package de.dagere.peass.dependency.execution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.RTSTestTransformerBuilder;
import de.dagere.peass.dependencytests.DependencyTestConstants;
import de.dagere.peass.execution.kieker.KiekerEnvironmentPreparer;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.testtransformation.TestTransformer;

public class TestKiekerEnvironmentPreparer {

   @BeforeEach
   public void init() throws IOException {
      FileUtils.deleteDirectory(DependencyTestConstants.CURRENT);
      FileUtils.copyDirectory(DependencyTestConstants.BASIC_STATE, DependencyTestConstants.CURRENT);
   }

   @Test
   public void testAOPXMLGeneration() throws IOException, InterruptedException {
      Set<String> includedMethodPatterns = new HashSet<String>();
      includedMethodPatterns.add("public void defaultpackage.NormalDependency.methodA(java.lang.String,int)");
      includedMethodPatterns.add("private int defaultpackage.NormalDependency.methodB()");
      List<File> modules = new LinkedList<File>();
      modules.add(DependencyTestConstants.CURRENT);
      KiekerConfig kiekerConfig = new KiekerConfig(true);
      kiekerConfig.setUseSourceInstrumentation(false);
      PeassFolders folders = new PeassFolders(DependencyTestConstants.CURRENT);
      TestTransformer testTransformer = RTSTestTransformerBuilder.createTestTransformer(folders, new ExecutionConfig(10), kiekerConfig); 
      KiekerEnvironmentPreparer kiekerEnvironmentPreparer = new KiekerEnvironmentPreparer(includedMethodPatterns, new LinkedList<>(), folders,
            testTransformer, modules);
      
      kiekerEnvironmentPreparer.prepareKieker();
      
      checkAopCorrectness();
   }

   private void checkAopCorrectness() throws IOException, FileNotFoundException {
      File aopXml = new File(DependencyTestConstants.CURRENT, "src/main/resources/META-INF/aop.xml");
      Assert.assertTrue(aopXml.exists());
      
      try (FileInputStream inputStream = new FileInputStream(aopXml)){
         String fileText = IOUtils.toString(inputStream, "UTF-8");
         MatcherAssert.assertThat(fileText, Matchers.containsString("defaultpackage.NormalDependency"));
      }
   }
   
   @Test
   public void testFullAopXMLGeneration() throws IOException, InterruptedException {
      List<File> modules = new LinkedList<File>();
      modules.add(DependencyTestConstants.CURRENT);
      
      KiekerConfig kiekerConfig = new KiekerConfig(true);
      kiekerConfig.setUseSourceInstrumentation(false);
      PeassFolders folders = new PeassFolders(DependencyTestConstants.CURRENT);
      TestTransformer testTransformer = RTSTestTransformerBuilder.createTestTransformer(folders, new ExecutionConfig(10), kiekerConfig); 
      KiekerEnvironmentPreparer kiekerEnvironmentPreparer = new KiekerEnvironmentPreparer(null, Arrays.asList("defaultpackage.NormalDependency"), folders,
            testTransformer, modules);
      
      kiekerEnvironmentPreparer.prepareKieker();
      
      checkAopCorrectness();
   }

}
