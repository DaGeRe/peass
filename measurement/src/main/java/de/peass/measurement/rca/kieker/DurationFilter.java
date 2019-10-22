package de.peass.measurement.rca.kieker;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.measurement.rca.data.CallTreeNode;
import kieker.analysis.IProjectContext;
import kieker.analysis.plugin.annotation.InputPort;
import kieker.analysis.plugin.annotation.Plugin;
import kieker.analysis.plugin.filter.AbstractFilterPlugin;
import kieker.common.configuration.Configuration;
import kieker.tools.traceAnalysis.systemModel.Execution;

@Plugin(description = "A filter to get durations from execution traces")
public class DurationFilter extends AbstractFilterPlugin {

   private static final Logger LOG = LogManager.getLogger(DurationFilter.class);

   public static final String INPUT_EXECUTION_TRACE = "INPUT_EXECUTION_TRACE";

   private final Set<CallTreeNode> measuredNodes;
   private final String version;

   public DurationFilter(final Set<CallTreeNode> measuredNodes, final IProjectContext projectContext, final String version) {
      super(new Configuration(), projectContext);
      this.measuredNodes = measuredNodes;
      this.version = version;

      measuredNodes.forEach(node -> node.newVM(version));
   }

   @InputPort(name = INPUT_EXECUTION_TRACE, eventTypes = { Execution.class })
   public void handleInputs(final Execution execution) {
      LOG.trace("Trace: " + execution.getTraceId());

      final String fullClassname = execution.getOperation().getComponentType().getFullQualifiedName().intern();
      final String methodname = execution.getOperation().getSignature().getName().intern();
      final String call = (fullClassname + "#" + methodname).intern();

      addMeasurements(execution, call);
   }

   private void addMeasurements(final Execution execution, final String call) {
      for (final CallTreeNode node : measuredNodes) {
         if (node.getCall().equals(call)) {
            // Get duration in mikroseconds - Kieker produces nanoseconds
            final long duration = (execution.getTout() - execution.getTin()) / 1000;
            node.addMeasurement(version, duration);
         }
      }
   }

   @Override
   public Configuration getCurrentConfiguration() {
      return super.configuration;
   }

}
