package de.peass.measurement.rca;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.helper.TestConstants;
import de.peass.measurement.rca.helper.TreeBuilder;
import de.peass.measurement.rca.helper.TreeBuilderBig;
import de.peass.measurement.rca.helper.TreeBuilderDifferent;
import de.peass.measurement.rca.kieker.BothTreeReader;
import kieker.analysis.exception.AnalysisConfigurationException;

public class CauseSearcherCompleteTest {

   final CauseTester measurer = Mockito.mock(CauseTester.class);
   
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

      final Set<ChangedEntity> changes = getChanges(root1, root2);

      System.out.println(changes);
      Assert.assertEquals(1, changes.size());
      Assert.assertThat(changes, Matchers.hasItem(new ChangedEntity("ClassB#methodB", "")));
   }
   
   @Test
   public void testSameNodesNotDifferent()
         throws InterruptedException, IOException, IllegalStateException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final CallTreeNode root1 = new TreeBuilderBig(false).getRoot();
      final CallTreeNode root2 = new TreeBuilderBig(false).getRoot();

      final Set<ChangedEntity> changes = getChanges(root1, root2);

      Assert.assertEquals(1, changes.size());
      Assert.assertThat(changes, Matchers.hasItem(new ChangedEntity("ClassB#methodB", "")));
   }
   
   @Test
   public void testDifferentTree()
         throws InterruptedException, IOException, IllegalStateException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final CallTreeNode root1 = new TreeBuilder().getRoot();
      final TreeBuilderDifferent differentTreeBuilder = new TreeBuilderDifferent();
      final CallTreeNode root2 = differentTreeBuilder.getRoot();

      final Set<ChangedEntity> changes = getChanges(root1, root2);

      System.out.println(changes);
      Assert.assertEquals(3, changes.size());
      Assert.assertThat(changes, Matchers.hasItem(new ChangedEntity("ClassB#methodB", "")));
      Assert.assertThat(changes, Matchers.hasItem(new ChangedEntity("ClassD#methodD", "")));
      Assert.assertThat(changes, Matchers.hasItem(new ChangedEntity("ClassE#methodE", "")));
      
      ArgumentCaptor<List<CallTreeNode>> includedNodes = ArgumentCaptor.forClass(List.class);
      Mockito.verify(measurer).measureVersion(includedNodes.capture());
      
      Assert.assertThat(includedNodes.getValue(), Matchers.hasItem(differentTreeBuilder.getD()));
      Assert.assertThat(includedNodes.getValue(), Matchers.hasItem(differentTreeBuilder.getE()));
   }

   private Set<ChangedEntity> getChanges(final CallTreeNode root1, final CallTreeNode root2)
         throws InterruptedException, IOException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final File folder = new File("target/test/");
      folder.mkdir();

      final BothTreeReader treeReader = Mockito.mock(BothTreeReader.class);

      Mockito.when(treeReader.getRootPredecessor()).thenReturn(root1);
      Mockito.when(treeReader.getRootVersion()).thenReturn(root2);

      final CauseSearcherComplete searcher = new CauseSearcherComplete(treeReader, TestConstants.SIMPLE_CAUSE_CONFIG, measurer, 
            TestConstants.SIMPLE_MEASUREMENT_CONFIG,
            new CauseSearchFolders(folder));
      final Set<ChangedEntity> changes = searcher.search();
      
      return changes;
   }

}
