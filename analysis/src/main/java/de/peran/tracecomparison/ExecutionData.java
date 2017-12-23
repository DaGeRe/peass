package de.peran.tracecomparison;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import kieker.tools.traceAnalysis.systemModel.Execution;

public class ExecutionData {
	
	private static final Logger LOG = LogManager.getLogger(ExecutionData.class);

	private final String testMethod;

	public ExecutionData(String testMethod) {
		this.testMethod = testMethod;
	}

	List<List<Execution>> iterations = new LinkedList<>();
	List<Execution> currentExecutions = null;

	public void addExecution(Execution execution) {
		String easySignature = execution.getAllocationComponent().getAssemblyComponent().getType().getFullQualifiedName() + "." + execution.getOperation().getSignature().getName();
		synchronized (iterations) {
			if (easySignature.equals(testMethod)) {
				currentExecutions = new LinkedList<>();
				iterations.add(currentExecutions);
			}
			if (currentExecutions != null) {
				currentExecutions.add(execution);
			}
		}
	}
	

	public List<Execution> popExecution() {
		LOG.debug("Popping {}", this);
		List<Execution> popExecutions = new LinkedList<>();
		synchronized (iterations) {
			for (List<Execution> execution : iterations) {
				popExecutions.add(execution.remove(0));
			}
		}
		return popExecutions;
	}

	public boolean hasValues() {
		return getPoppableExecutions() > 0;
	}

	public int getPoppableExecutions() {
		if (iterations.size() == 0) {
			return 0;
		} else {
			int popable = Integer.MAX_VALUE;
			synchronized (iterations) {
				for (List<Execution> execution : iterations) {
					LOG.debug(execution.size());
					popable = Math.min(popable, execution.size());
				}
			}
			LOG.debug("Poppable: {} {}", popable, this);
			return popable;
		}
	}


	public List<Execution> getFirstExecution() {
		return iterations.get(0);
	}


	public List<Execution> getExecutions(Integer index) {
		List<Execution> executions = new LinkedList<>();
		synchronized (iterations) {
			for (List<Execution> execution : iterations) {
				if (execution.size() > index){
					executions.add(execution.get(index));
				}
			}
		}
		if (executions.size() < iterations.size() - 1){
			LOG.error("Warning: Index " + index + " has only " + executions.size() + " entries.");
		}
		return executions;
	}


	public Map<Integer, Execution> getChildren(Integer index) {
		Map<Integer, Execution> children = new LinkedHashMap<>();
		synchronized (iterations) {
			for (List<Execution> execution : iterations) {
				if (execution.size() > index){
					Execution ex = execution.get(index);
					for (Execution possibleChildExecutions : execution) {
						if (possibleChildExecutions.getEss() == ex.getEss() + 1){
							
						}
					}
//					ex.getEss();
//					ex.getEoi();
				}
			}
		}
		return null;
	}
}