package de.peass.measurement.rca;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.rca.CauseSearcherComplete;
import de.peass.measurement.rca.CauseTester;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.helper.TestConstants;
import de.peass.measurement.rca.helper.TreeBuilder;
import de.peass.measurement.rca.helper.TreeBuilderBig;
import de.peass.measurement.rca.helper.TreeBuilderDifferent;
import de.peass.measurement.rca.kieker.BothTreeReader;
import kieker.analysis.exception.AnalysisConfigurationException;

public class CauseSearcherCompleteTest {

   @Before
   public void cleanup() {
      final File folder = new File("target/test_peass/");
      try {
         FileUtils.deleteDirectory(folder);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }
   
   @Test
   public void testSameNodesChanges()
         throws InterruptedException, IOException, IllegalStateException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final CallTreeNode root1 = new TreeBuilderBig(true).getRoot();
      final CallTreeNode root2 = new TreeBuilderBig(true).getRoot();

      final List<ChangedEntity> changes = getChanges(root1, root2);

      Assert.assertEquals(2, changes.size());
      Assert.assertEquals("ClassB#methodB", changes.get(0).toString());
      Assert.assertEquals("ClassB#methodB", changes.get(1).toString());
   }
   
   @Test
   public void testSameNodesNotDifferent()
         throws InterruptedException, IOException, IllegalStateException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final CallTreeNode root1 = new TreeBuilderBig(false).getRoot();
      final CallTreeNode root2 = new TreeBuilderBig(false).getRoot();

      final List<ChangedEntity> changes = getChanges(root1, root2);

      Assert.assertEquals(1, changes.size());
      Assert.assertEquals("ClassB#methodB", changes.get(0).toString());
   }
   
   @Test
   public void testDifferentTree()
         throws InterruptedException, IOException, IllegalStateException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final CallTreeNode root1 = new TreeBuilder().getRoot();
      final CallTreeNode root2 = new TreeBuilderDifferent().getRoot();

      final List<ChangedEntity> changes = getChanges(root1, root2);

      System.out.println(changes);
      Assert.assertEquals(2, changes.size());
      Assert.assertEquals("ClassB#methodB", changes.get(0).toString());
      Assert.assertEquals("ClassC#methodC", changes.get(1).toString());
   }

   private List<ChangedEntity> getChanges(final CallTreeNode root1, final CallTreeNode root2)
         throws InterruptedException, IOException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final File folder = new File("target/test/");
      folder.mkdir();

      final BothTreeReader treeReader = Mockito.mock(BothTreeReader.class);

      Mockito.when(treeReader.getRootPredecessor()).thenReturn(root1);
      Mockito.when(treeReader.getRootVersion()).thenReturn(root2);
      final CauseTester measurer = Mockito.mock(CauseTester.class);

      final CauseSearcherComplete searcher = new CauseSearcherComplete(treeReader, TestConstants.SIMPLE_CAUSE_CONFIG, measurer, 
            TestConstants.SIMPLE_MEASUREMENT_CONFIG,
            new CauseSearchFolders(folder));
      final List<ChangedEntity> changes = searcher.search();
      return changes;
   }

}
