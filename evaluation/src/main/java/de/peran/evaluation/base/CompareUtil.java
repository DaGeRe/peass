package de.peran.evaluation.base;

/*-
 * #%L
 * peran-evaluation
 * %%
 * Copyright (C) 2017 DaGeRe
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */


import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.peran.dependency.analysis.data.TestSet;
import de.peran.reduceddependency.ChangedTraceTests;

/**
 * Utility for comparing the evaluations
 * 
 * @author reichelt
 *
 */
public final class CompareUtil {

	private CompareUtil() {

	}

	public static EvaluationProject createEvaluationData(final ChangedTraceTests changedTraceTests) {
		final EvaluationProject ticData = new EvaluationProject();
		for (final Entry<String, TestSet> version : changedTraceTests.getVersions().entrySet()) {
			final EvaluationVersion evalVersion = new EvaluationVersion();
			// version.set
			for (final Map.Entry<String, List<String>> clazz : version.getValue().entrySet()) {
				evalVersion.getTestcaseExecutions().put(clazz.getKey(), clazz.getValue().size());
			}
			ticData.getVersions().put(version.getKey(), evalVersion);
		}
		return ticData;
	}

	public static EvaluationResult compareEvaluations(final EvaluationProject ticData, final EvaluationProject data) {
		final EvaluationResult result = new EvaluationResult();
		int overallMethods = 0;
		int equal = 0;
		for (final Map.Entry<String, EvaluationVersion> version : ticData.getVersions().entrySet()) {
			if (data.getVersions().containsKey(version.getKey())) {
				final EvaluationVersion toolVersion = data.getVersions().get(version.getKey());

				for (final Map.Entry<String, Integer> peassMethods : version.getValue().getTestcaseExecutions().entrySet()) {
					final Integer toolMethods = toolVersion.getTestcaseExecutions().get(peassMethods.getKey());

					if (toolMethods != null) {
						if (toolMethods == 0) {
							equal += peassMethods.getValue();
						} else {
							equal += Math.min(toolMethods, peassMethods.getValue());
							if (toolMethods < peassMethods.getValue()) {
								System.out.println("Methodcount bigger in PeASS: " + version.getKey() + " "
										+ peassMethods.getKey() + " (Other: " + toolMethods + " PeASS: "
										+ peassMethods.getValue() + ")");
							}
						}

					} 

					overallMethods += peassMethods.getValue();
				}
			}
		}

		System.out.println("Marked " + equal + " of " + overallMethods);
		result.setEqualMethods(equal);
		result.setOverallMethods(overallMethods);
		return result;

	}
}
