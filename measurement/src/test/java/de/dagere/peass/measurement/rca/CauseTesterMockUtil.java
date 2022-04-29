package de.dagere.peass.measurement.rca;

import java.io.IOException;
import java.util.List;



import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.helper.TreeBuilder;
import kieker.analysis.exception.AnalysisConfigurationException;

public class CauseTesterMockUtil {

   public static void mockMeasurement(final CauseTester measurer, final TreeBuilder builderPredecessor)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException {
      Mockito.doAnswer(new Answer<Void>() {
         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            Object[] args = invocation.getArguments();
            System.out.println("Mocking measurement of: " + args[0]);
            List<CallTreeNode> nodes = (List<CallTreeNode>) args[0] ;
            builderPredecessor.buildMeasurements(nodes.toArray(new CallTreeNode[0]));
            return null;
         }
      }).when(measurer).measureVersion(Mockito.any());
   }

}
