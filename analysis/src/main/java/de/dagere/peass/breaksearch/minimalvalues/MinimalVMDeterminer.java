package de.dagere.peass.breaksearch.minimalvalues;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.breaksearch.FindLowestPossibleIterations;

public class MinimalVMDeterminer extends MinimalValueDeterminer {
	private static final Logger LOG = LogManager.getLogger(MinimalValueDeterminer.class);

	@Override
	int getSize(final List<VMResult> results) {
		return results.size();
	}

	@Override
	int getMin(final List<VMResult> results) {
		return 2;
	}

	@Override
	int getChange(final List<VMResult> results) {
		return 1;
	}

	@Override
	int analyzeMeasurement(final int oldResult, final List<VMResult> current, final List<VMResult> before) {
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