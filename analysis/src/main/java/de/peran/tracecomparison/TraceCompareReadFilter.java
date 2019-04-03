package de.peran.tracecomparison;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import kieker.analysis.IProjectContext;
import kieker.analysis.plugin.annotation.InputPort;
import kieker.analysis.plugin.filter.AbstractFilterPlugin;
import kieker.common.configuration.Configuration;
import kieker.tools.traceAnalysis.systemModel.Execution;
import kieker.tools.traceAnalysis.systemModel.ExecutionTrace;

public class TraceCompareReadFilter extends AbstractFilterPlugin {
	private static final Logger LOG = LogManager.getLogger(TraceCompareReadFilter.class);

	public static final String INPUT_EXECUTION_TRACE = "INPUT_EXECUTION_TRACE";
	private final ExecutionTraceData executions;

	public TraceCompareReadFilter(final Configuration configuration, final IProjectContext projectContext, ExecutionTraceData executions) {
		super(configuration, projectContext);
		this.executions = executions;
	}

	@Override
	public Configuration getCurrentConfiguration() {
		return super.configuration;
	}

	@InputPort(name = INPUT_EXECUTION_TRACE, eventTypes = { ExecutionTrace.class })
	public void handleInputs(final ExecutionTrace trace) {
		LOG.info("Trace: " + trace.getTraceId() + " " + trace.getTraceAsSortedExecutionSet().size());

		synchronized (executions) {
			for (final Execution execution : trace.getTraceAsSortedExecutionSet()) {
				if (!execution.getOperation().getSignature().getName().equals("getRepetitions")){
					executions.addExecution(execution);
				}
				

				// CompareMeasurements.LOG.info("Adding: " + execution.getOperation());
				// myList.add(execution);
			}
			// CompareTraces.LOG.info("Listsize: " + myList.size());
		}
	}
}