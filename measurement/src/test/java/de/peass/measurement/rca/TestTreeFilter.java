package de.peass.measurement.rca;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.dependencytests.helper.FakeFileIterator;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.kieker.TreeReader;
import de.peass.measurement.rca.kieker.TreeReaderFactory;
import kieker.analysis.exception.AnalysisConfigurationException;

public class TestTreeFilter {
   
   
   private File tempDir;
   private File projectFolder;
   
   public void setUp(final String source) {
      try {
         File sourceDir = new File(source);
         
         tempDir = Files.createTempDirectory(new File("target").toPath(), "peass_").toFile();
         projectFolder = new File(tempDir, "project");
         
         FakeFileIterator.copy(sourceDir, projectFolder);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   @ParameterizedTest
   @ValueSource(strings = {"src/test/resources/treeReadExample", "src/test/resources/treeReadExampleGradle"})
   public void testComplexTreeCreation(final String sourceDir) throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException {
      setUp(sourceDir);
      
      CallTreeNode rootNode = getTree();
      
      Assert.assertNotNull(rootNode);
      System.out.println(rootNode.getChildren());
      Assert.assertEquals(7, rootNode.getChildren().size());
      CallTreeNode executeThingNode = rootNode.getChildren().get(1);
      Assert.assertEquals(3, executeThingNode.getChildren().size());
      Assert.assertEquals(2, executeThingNode.getChildren().get(0).getChildren().size());
      Assert.assertEquals(1, executeThingNode.getChildren().get(2).getChildren().size());
      
      CallTreeNode otherConstructor = rootNode.getChildren().get(3);
      Assert.assertEquals("new defaultpackage.OtherDependency.<init>()", otherConstructor.getKiekerPattern());
      
      CallTreeNode executeThingOther = rootNode.getChildren().get(5);
      Assert.assertEquals("defaultpackage.OtherDependency#executeThing", executeThingOther.getCall());
      CallTreeNode child1 = executeThingOther.getChildren().get(0);
      CallTreeNode child2 = executeThingOther.getChildren().get(1);
      CallTreeNode child3 = executeThingOther.getChildren().get(2);
      Assert.assertEquals("defaultpackage.OtherDependency#child1", child1.getCall());
      Assert.assertEquals("defaultpackage.OtherDependency#child2", child2.getCall());
      Assert.assertEquals("defaultpackage.OtherDependency#child3", child3.getCall());
      System.out.println(child3.getChildren());
      Assert.assertEquals(1, child3.getChildren().size());
   }

   public CallTreeNode getTree() throws IOException, XmlPullParserException, InterruptedException, FileNotFoundException, ViewNotFoundException, AnalysisConfigurationException {
      TreeReader executor = TreeReaderFactory.createTestTreeReader(projectFolder, 15, new EnvironmentVariables());
      
      TestCase test = new TestCase("defaultpackage.TestMe", "testMe");
//      executor.executeKoPeMeKiekerRun(new TestSet(test), "1");
      CallTreeNode node = executor.getTree(test, "1");
      return node;
   }
}
