package de.peran.analysis.helper;

/*-
 * #%L
 * peran-analysis
 * %%
 * Copyright (C) 2015 - 2017 DaGeRe
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


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result;
import de.peran.FindLowestPossibleIterations;

public class MinimalVMDeterminer extends MinimalValueDeterminer {
	private static final Logger LOG = LogManager.getLogger(MinimalValueDeterminer.class);

	@Override
	int getSize(final List<Result> results) {
		return results.size();
	}

	@Override
	int getMin(final List<Result> results) {
		return 2;
	}

	@Override
	int getChange(final List<Result> results) {
		return 1;
	}

	@Override
	int analyzeMeasurement(final int oldResult, final List<Result> current, final List<Result> before) {
		final List<Double> allStatistics = getValues(current);
		final List<Double> allStatisticsBefore = getValues(before);

		int localMinValue = current.size();

		vmloop: for (; localMinValue > 2; localMinValue--) {
			for (int start = 0; start < current.size() - localMinValue; start++) {
				final boolean significant;
				final List<Double> shortenedResult, shortenedBeforeResult;
				if (start + localMinValue <= current.size()) {
					shortenedResult = allStatistics.subList(start, start + localMinValue);
					shortenedBeforeResult = allStatisticsBefore.subList(start, start + localMinValue);
				} else {
					final int altStart = (start + localMinValue) % allStatistics.size();
					shortenedResult = allStatistics.subList(altStart, start);
					shortenedBeforeResult = allStatisticsBefore.subList(altStart, start);
				}
				significant = FindLowestPossibleIterations.isStillSignificant(shortenedResult, shortenedBeforeResult, oldResult);
				// final boolean significant = isStillSignificant(statistics.subList(start, start + localMinVmTry), statisticsBefore.subList(start, start + localMinVmTry), oldResult);
				if (!significant) {
					final File f = new File("results/measure_" + FindLowestPossibleIterations.fileindex + "_" + localMinValue + ".csv");
					try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
						LOG.info("Break at " + localMinValue);
						for (int i = 0; i < shortenedBeforeResult.size(); i++) {
							bw.write(shortenedResult.get(i) + ";" + shortenedBeforeResult.get(i) + "\n");
						}
					} catch (final IOException e) {
						e.printStackTrace();
					}

					FindLowestPossibleIterations.fileindex++;
					break vmloop;
				}
			}
		}
		return localMinValue;
	}
}
