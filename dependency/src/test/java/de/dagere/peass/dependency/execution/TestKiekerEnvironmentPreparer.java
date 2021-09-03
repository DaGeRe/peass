package de.dagere.peass.dependency.execution;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfiguration;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependencytests.DependencyTestConstants;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class TestKiekerEnvironmentPreparer {

   @Before
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
      KiekerEnvironmentPreparer kiekerEnvironmentPreparer = new KiekerEnvironmentPreparer(includedMethodPatterns, new PeassFolders(DependencyTestConstants.CURRENT),
            new JUnitTestTransformer(DependencyTestConstants.CURRENT, new ExecutionConfig(10), new KiekerConfiguration(true)), modules);
      
      kiekerEnvironmentPreparer.prepareKieker();
      
      File aopXml = new File(DependencyTestConstants.CURRENT, "src/main/resources/META-INF/aop.xml");
      Assert.assertTrue(aopXml.exists());
      
      try (FileInputStream inputStream = new FileInputStream(aopXml)){
         String fileText = IOUtils.toString(inputStream, "UTF-8");
         Assert.assertThat(fileText, Matchers.containsString("defaultpackage.NormalDependency"));
      }
   }

}
