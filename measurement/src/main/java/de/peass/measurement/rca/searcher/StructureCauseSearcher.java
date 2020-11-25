package de.peass.measurement.rca.searcher;

import java.io.IOException;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.rca.CausePersistenceManager;
import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.CauseTester;
import de.peass.measurement.rca.kieker.BothTreeReader;
import kieker.analysis.exception.AnalysisConfigurationException;

public class StructureCauseSearcher extends CauseSearcher {

   public StructureCauseSearcher(BothTreeReader reader, CauseSearcherConfig causeSearchConfig, CauseTester measurer, MeasurementConfiguration measurementConfig,
         CauseSearchFolders folders) throws InterruptedException, IOException {
      super(reader, causeSearchConfig, measurer, measurementConfig, folders);
      persistenceManager = new CausePersistenceManager(causeSearchConfig, measurementConfig, folders);
   }

   @Override
   protected Set<ChangedEntity> searchCause()
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      // TODO Auto-generated method stub
      return null;
   }

}
