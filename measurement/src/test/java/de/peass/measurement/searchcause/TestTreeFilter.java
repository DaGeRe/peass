package de.peass.measurement.searchcause;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

import org.aspectj.util.FileUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.searchcause.data.CallTreeNode;
import de.peass.measurement.searchcause.kieker.TreeReader;
import de.peass.measurement.searchcause.kieker.TreeReaderFactory;
import kieker.analysis.exception.AnalysisConfigurationException;

public class TestTreeFilter {
   
   private static final File SOURCE_DIR = new File("src/test/resources/rootCauseIT/basic_state/");
   
   private File tempDir;
   private File projectFolder;
   
   @Before
   public void setUp() {
      try {
         tempDir = Files.createTempDirectory(new File("target").toPath(), "peass_").toFile();
         projectFolder = new File(tempDir, "project");
         
         FileUtil.copyDir(SOURCE_DIR, projectFolder);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   @Test
   public void testComplexTreeCreation() throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException {
      CallTreeNode node = getTree(projectFolder);
      
      Assert.assertNotNull(node);
      Assert.assertEquals(3, node.getChildren().size());
      CallTreeNode executeThingNode = node.getChildren().get(1);
      Assert.assertEquals(3, executeThingNode.getChildren().size());
      Assert.assertEquals(2, executeThingNode.getChildren().get(0).getChildren().size());
      Assert.assertEquals(1, executeThingNode.getChildren().get(2).getChildren().size());
   }

   public static CallTreeNode getTree(File projectFolder) throws IOException, XmlPullParserException, InterruptedException, FileNotFoundException, ViewNotFoundException, AnalysisConfigurationException {
      TreeReader executor = TreeReaderFactory.createTestTreeReader(projectFolder, 15);
      
      TestCase test = new TestCase("defaultpackage.TestMe", "testMe");
//      executor.executeKoPeMeKiekerRun(new TestSet(test), "1");
      CallTreeNode node = executor.getTree(test, "1");
      return node;
   }
}
