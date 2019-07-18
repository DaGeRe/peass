package de.peass.measurement.searchcause.kieker;

import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.measurement.searchcause.data.CallTreeNode;
import kieker.analysis.IProjectContext;
import kieker.analysis.plugin.annotation.InputPort;
import kieker.analysis.plugin.annotation.Plugin;
import kieker.analysis.plugin.filter.AbstractFilterPlugin;
import kieker.common.configuration.Configuration;
import kieker.tools.traceAnalysis.systemModel.Execution;
import kieker.tools.traceAnalysis.systemModel.ExecutionTrace;

@Plugin(description = "A filter to get durations from execution traces")
public class DurationFilter extends AbstractFilterPlugin {

   private static final Logger LOG = LogManager.getLogger(DurationFilter.class);

   public static final String INPUT_EXECUTION_TRACE = "INPUT_EXECUTION_TRACE";

   private final Set<CallTreeNode> measuredNodes;
   private final String version;

   public DurationFilter(Set<CallTreeNode> measuredNodes, final IProjectContext projectContext, String version) {
      super(new Configuration(), projectContext);
      this.measuredNodes = measuredNodes;
      this.version = version;
      
      measuredNodes.forEach(node -> node.newChunk(version));
   }

   @InputPort(name = INPUT_EXECUTION_TRACE, eventTypes = { ExecutionTrace.class })
   public void handleInputs(final ExecutionTrace trace) {
      LOG.trace("Trace: " + trace.getTraceId());

      for (final Execution execution : trace.getTraceAsSortedExecutionSet()) {
         final String fullClassname = execution.getOperation().getComponentType().getFullQualifiedName().intern();
         final String methodname = execution.getOperation().getSignature().getName().intern();
         final String call = fullClassname + "#" + methodname;

         addMeasurements(execution, call);
      }
   }

   private void addMeasurements(final Execution execution, final String call) {
      for (CallTreeNode node : measuredNodes) {
         if (node.getCall().equals(call)) {
            long duration = execution.getTout() - execution.getTin();
            node.addMeasurement(version, duration);
         }
      }
   }

   @Override
   public Configuration getCurrentConfiguration() {
      return super.configuration;
   }

}
