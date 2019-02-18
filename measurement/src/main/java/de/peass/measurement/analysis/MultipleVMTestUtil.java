package de.peass.measurement.analysis;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datacollection.TimeDataCollector;
import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.Result.Fulldata.Value;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.dependency.analysis.data.TestCase;
import de.dagere.kopeme.generated.Versioninfo;

/**
 * Provides utilities for reading KoPeMe-data from multiple runs which should be summarized into one file.
 * 
 * @author reichelt
 *
 */
public class MultipleVMTestUtil {
	private static final Logger LOG = LogManager.getLogger(MultipleVMTestUtil.class);

	public static void main(final String[] args) throws JAXBException {
		final File resultFile = new File(args[0]);
		analyseOneRun(resultFile);
	}

	public static void analyseOneRun(final File resultFile) throws JAXBException {
		final DescriptiveStatistics st = getStatistics(resultFile);
		LOG.info("Durchschnitt: " + st.getMean());
	}

	public static DescriptiveStatistics getStatistics(final File resultFile) throws JAXBException {
		final XMLDataLoader fullDataLoader = new XMLDataLoader(resultFile);
		final Kopemedata fullResultData = fullDataLoader.getFullData();
		final Datacollector oneRunDatacollector = getTimeDataCollector(fullResultData);
		final DescriptiveStatistics st = new DescriptiveStatistics();
		for (final Result r : oneRunDatacollector.getResult()) {
			st.addValue(r.getValue());
		}
		return st;
	}

	public static Datacollector getTimeDataCollector(final Kopemedata fullResultData) {
		Datacollector oneRunDatacollector = null;
		for (final Datacollector collector : fullResultData.getTestcases().getTestcase().get(0).getDatacollector()) {
			if (collector.getName().equals(TimeDataCollector.class.getName())) {
				oneRunDatacollector = collector;
			}
		}
		if (oneRunDatacollector == null) {
			throw new RuntimeException("Achtung: Kein " + TimeDataCollector.class.getName() + " gefunden");
		}
		return oneRunDatacollector;
	}

	/**
	 * Takes the given result and the given version and creates a file containing the aggregated result.
	 * 
	 * @param fullResultFile
	 * @param oneRunData
	 * @param version
	 * @throws JAXBException
	 */
	public static void fillOtherData(final File fullResultFile, final TestcaseType oneRunData, final TestCase testcase, final String version, long currentChunkStart) throws JAXBException {
	   LOG.info("Writing to merged result file: {}", fullResultFile);
	   final XMLDataLoader fullDataLoader = new XMLDataLoader(fullResultFile);
		final Kopemedata fullResultData = fullDataLoader.getFullData();
		if (fullResultData.getTestcases().getTestcase().size() == 0) {
			fullResultData.getTestcases().setClazz(testcase.getClazz());
			fullResultData.getTestcases().getTestcase().add(new TestcaseType());
			fullResultData.getTestcases().getTestcase().get(0).setName(testcase.getMethod());
		}
		Datacollector oneRunDatacollector = null;
		for (final Datacollector collector : oneRunData.getDatacollector()) {
			if (collector.getName().equals(TimeDataCollector.class.getName())) {
				oneRunDatacollector = collector;
			}
		}
		if (oneRunDatacollector == null) {
			throw new RuntimeException("Achtung: Kein " + TimeDataCollector.class.getName() + " gefunden");
		}
		final List<Datacollector> fullResultFileDatacollectorList = fullResultData.getTestcases().getTestcase().get(0).getDatacollector();
		if (fullResultFileDatacollectorList.size() == 0) {
			fullResultFileDatacollectorList.add(new Datacollector());
			fullResultFileDatacollectorList.get(0).setName(oneRunDatacollector.getName());
		}
		final Datacollector fullFileDatacollector = fullResultFileDatacollectorList.get(0);
		Chunk realChunk = findChunk(currentChunkStart, fullFileDatacollector);
      if (realChunk == null) {
         realChunk = new Chunk();
         realChunk.setChunkStartTime(currentChunkStart);
         fullFileDatacollector.getChunk().add(realChunk);
      }

		final Result oneResult = oneRunDatacollector.getResult().get(0);
      final Fulldata realData = oneResult.getFulldata();
		if (realData != null && realData.getValue() != null && realData.getValue().size() > 0) {
			final SummaryStatistics st = new SummaryStatistics();
			createStatistics(st, realData);
			final Result result = createResultFromStatistic(version, st, oneResult.getRepetitions());
			result.setDate(oneResult.getDate());

			realChunk.getResult().add(result);
			XMLDataStorer.storeData(fullResultFile, fullResultData);
		} else {
			LOG.error("Achtung: Fulldata von " + fullResultFile + " leer!");
		}
	}

   public static Chunk findChunk(long currentChunkStart, final Datacollector fullFileDatacollector) {
      Chunk realChunk = null;
      for (final Chunk chunk : fullFileDatacollector.getChunk()) {
         if (chunk.getChunkStartTime() == currentChunkStart) {
            realChunk = chunk;
            break;
         }
      }
      return realChunk;
   }

	private static Result createResultFromStatistic(final String version, final SummaryStatistics st, long repetitions) {
		final Result result = new Result();
		result.setValue(st.getMean());
		result.setMin((long) st.getMin());
		result.setMax((long) st.getMax());
		result.setVersion(new Versioninfo());
		result.getVersion().setGitversion(version);
		result.setDeviation(st.getStandardDeviation());
		result.setExecutionTimes(st.getN());
		result.setRepetitions(repetitions);
		return result;
	}

	private static double[] createStatistics(final SummaryStatistics st, final Fulldata realData) {
		final double[] values = new double[realData.getValue().size()];
		int i = 0;
		for (final Value value : realData.getValue()) {
			final long parseDouble = Long.parseLong(value.getValue());
			st.addValue(parseDouble);
			values[i++] = parseDouble;
		}
		return values;
	}

	public static List<Double> getAverages(final List<Result> before) {
		return before.stream()
				.mapToDouble(beforeVal -> beforeVal.getFulldata().getValue().stream()
						.mapToDouble(val -> Double.parseDouble(val.getValue())).sum()
						/ beforeVal.getFulldata().getValue().size())
				.boxed().sorted().collect(Collectors.toList());
	}

	public static SummaryStatistics getStatistic(List<Result> results) {
		final SummaryStatistics statistisc = new SummaryStatistics();
		results.forEach(result -> statistisc.addValue(result.getValue()));
		return statistisc;
	}

	public static int compareDouble(final List<Double> before, final List<Double> after) {
		final boolean change = TestUtils.tTest(ArrayUtils.toPrimitive(before.toArray(new Double[0])), ArrayUtils.toPrimitive(after.toArray(new Double[0])), 0.05);
		final SummaryStatistics statisticBefore = new SummaryStatistics();
		before.forEach(result -> statisticBefore.addValue(result));

		final SummaryStatistics statisticAfter = new SummaryStatistics();
		after.forEach(result -> statisticAfter.addValue(result));
		if (change) {
			if (statisticBefore.getMean() < statisticAfter.getMean())
				return -1;
			else
				return 1;
		} else {
			return 0;
		}
	}

}
