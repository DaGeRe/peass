package de.peran.evaluation;

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


import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import de.peran.evaluation.base.CompareUtil;
import de.peran.evaluation.base.EvaluationProject;
import de.peran.reduceddependency.ChangedTraceTests;

/**
 * Prints tests which would have been executed by EKSTAZI, Infinitest and DePeC.
 * 
 * @author reichelt
 *
 */
public final class CompareEvaluations {

	private static final double PERCENT = 100d;

	private static final Logger LOG = LogManager.getLogger(CompareEvaluations.class);

	private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("##.##");

	public static void main(final String[] args) {
		final File peassFolder = new File(args[0]);
		final File evaluationFolder = new File(args[1]);

		double ekstaziSum = 0;
		double infiniSum = 0;
		double ticSum = 0;
		int count = 0;
		for (final File dependencies : peassFolder.listFiles()) {
			if (dependencies.getName().startsWith("deps_")) {
				final String project = dependencies.getName().substring(5, dependencies.getName().indexOf('.'));
				final File executefile = new File(peassFolder, "execute" + project + ".json");
				if (executefile.exists()) {
					count++;
					try {
						final ObjectMapper objectMapper = new ObjectMapper();
						final SimpleModule module = new SimpleModule();
						module.addDeserializer(ChangedTraceTests.class, new ChangedTraceTests.Deserializer());
						objectMapper.registerModule(module);

						final ChangedTraceTests changedTraceTests = objectMapper.readValue(executefile, ChangedTraceTests.class);

						final EvaluationProject ticData = CompareUtil.createEvaluationData(changedTraceTests);

						final File emptyFile = new File(evaluationFolder, "evaluation_" + project + "_empty.json");
						final File ekstaziFile = new File(evaluationFolder, "evaluation_" + project + "_ekstazi.json");
						final File infinitestFile = new File(evaluationFolder, "evaluation_" + project + "_infinitest.json");

						int infinitestCount = 1;
						int emptyCount = 1;
						int size = 1;
						if (emptyFile.exists()) {
							final EvaluationProject emptyData = objectMapper.readValue(emptyFile, EvaluationProject.class);
							emptyCount = emptyData.getOverallTestCount();
							size = emptyData.getVersions().size();
						}

						final EvaluationProject ekstaziData = objectMapper.readValue(ekstaziFile, EvaluationProject.class);
						if (infinitestFile.exists()) {
							final EvaluationProject infinitestData = objectMapper.readValue(infinitestFile, EvaluationProject.class);
							infinitestCount = infinitestData.getOverallTestCount();
						}

						System.out.println(project + ";" + size + ";" + emptyCount + ";" + ekstaziData.getOverallTestCount() + ";"
								+ PERCENT_FORMAT.format(PERCENT * ekstaziData.getOverallTestCount() / emptyCount) + "%;" + infinitestCount + ";"
								+ PERCENT_FORMAT.format(PERCENT * infinitestCount / emptyCount) + "%;" + ticData.getOverallTestCount() + ";"
								+ PERCENT_FORMAT.format(PERCENT * ticData.getOverallTestCount() / emptyCount) + "%");

						ekstaziSum += (PERCENT * ekstaziData.getOverallTestCount() / emptyCount);
						infiniSum += (PERCENT * infinitestCount / emptyCount);
						ticSum += (PERCENT * ticData.getOverallTestCount() / emptyCount);
					} catch (final IOException e) {
						e.printStackTrace();
					}

				}
			}
		}

		System.out.println(PERCENT_FORMAT.format(ekstaziSum / count) + ";" + PERCENT_FORMAT.format(infiniSum / count) + ";" + PERCENT_FORMAT.format(ticSum / count));
	}

	private CompareEvaluations() {

	}
}
