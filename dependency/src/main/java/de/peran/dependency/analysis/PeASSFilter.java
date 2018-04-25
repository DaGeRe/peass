/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.peran.dependency.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.peran.dependency.ClazzFinder;
import de.peran.dependency.analysis.data.ChangedEntity;
import de.peran.dependency.analysis.data.TraceElement;
import kieker.analysis.IProjectContext;
import kieker.analysis.plugin.annotation.InputPort;
import kieker.analysis.plugin.annotation.Plugin;
import kieker.analysis.plugin.filter.AbstractFilterPlugin;
import kieker.common.configuration.Configuration;
import kieker.tools.traceAnalysis.systemModel.Execution;
import kieker.tools.traceAnalysis.systemModel.ExecutionTrace;

/**
 * Loads all methods for parsing the trace
 * @author reichelt
 *
 */
@Plugin(description = "A filter to transform PeASS-Traces")
public class PeASSFilter extends AbstractFilterPlugin {
	public static final String INPUT_EXECUTION_TRACE = "INPUT_EXECUTION_TRACE";

	private final Map<ChangedEntity, Set<String>> classes = new HashMap<>();
	private final ArrayList<TraceElement> calls = new ArrayList<>();
	private final String prefix;
	private final File projectFolder;
	
	private final static int CALLCOUNT = 10000000;

	public PeASSFilter(final String prefix, final Configuration configuration, final IProjectContext projectContext, final File projectFolder) {
		super(configuration, projectContext);
		this.prefix = prefix;
		this.projectFolder = projectFolder;
	}

	@Override
	public Configuration getCurrentConfiguration() {
		return super.configuration;
	}

	@InputPort(name = INPUT_EXECUTION_TRACE, eventTypes = { ExecutionTrace.class })
	public void handleInputs(final ExecutionTrace trace) {
		LOG.info("Trace: " + trace.getTraceId());

		for (final Execution execution : trace.getTraceAsSortedExecutionSet()) {
			if (calls.size() > CALLCOUNT ){
				calls.add(new TraceElement("ATTENTION", "TO MUCH CALLS", 0));
				LOG.info("Trace Reading Aborted, Cause More Than "+CALLCOUNT+" Appeared");
				break;
			}
			
			final String fullClassname = execution.getOperation().getComponentType().getFullQualifiedName().intern();
			if (prefix == null || prefix != null && fullClassname.startsWith(prefix)) {
				if (!fullClassname.contains("junit") && !fullClassname.contains("log4j")) {
					final String methodname = execution.getOperation().getSignature().getName().intern();

					final TraceElement traceelement = new TraceElement(fullClassname, methodname, execution.getEss());
					if (Arrays.asList(execution.getOperation().getSignature().getModifier()).contains("static")) {
						traceelement.setStatic(true);
					}
					final String[] paramTypeList = execution.getOperation().getSignature().getParamTypeList();
					final String[] internParamTypeList = new String[paramTypeList.length];
					for (int i = 0; i < paramTypeList.length; i++){
						internParamTypeList[i] = paramTypeList[i].intern();
					}
					traceelement.setParameterTypes(paramTypeList);
					
					// KoPeMe-methods are not relevant
					if (!methodname.equals("logFullData") 
							&& !methodname.equals("useKieker") 
							&& !methodname.equals("getWarmupExecutions") 
							&& !methodname.equals("getExecutionTimes") 
							&& !methodname.equals("getMaximalTime")
							&& !methodname.equals("getRepetitions")
							&& !methodname.equals("getDataCollectors") ) {
						calls.add(traceelement);
						final String clazzFilename = ClazzFinder.getClassFilename(projectFolder, fullClassname);
                  final String outerClazzName = ClazzFinder.getOuterClass(fullClassname);
                  final ChangedEntity fullClassEntity = new ChangedEntity(clazzFilename, ModuleClassMapping.getModuleOfClass(outerClazzName));
						Set<String> currentMethodSet = classes.get(fullClassEntity);
						if (currentMethodSet == null) {
							currentMethodSet = new HashSet<>();
                     classes.put(fullClassEntity, currentMethodSet);
						}
						currentMethodSet.add(methodname);
					}
				}
			}
		}
		LOG.info("Finished");
	}

	public ArrayList<TraceElement> getCalls() {
		return calls;
	}

	public Map<ChangedEntity, Set<String>> getCalledMethods() {
		return classes;
	}

}
