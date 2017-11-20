package de.peran.measurement.analysis;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datacollection.TimeDataCollector;
import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result.Fulldata;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result.Fulldata.Value;
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
		final XMLDataLoader fullDataLoader = new XMLDataLoader(resultFile);
		final Kopemedata fullResultData = fullDataLoader.getFullData();
		final Datacollector oneRunDatacollector = getTimeDataCollector(fullResultData);
		final SummaryStatistics st = new SummaryStatistics();
		for (final Result r : oneRunDatacollector.getResult()) {
			st.addValue(r.getValue());
		}
		LOG.info("Durchschnitt: " + st.getMean());
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
	 * Takes the given result and the given verion and creates a file containing the aggregated result.
	 * 
	 * @param fullResultFile
	 * @param oneRunData
	 * @param version
	 * @throws JAXBException
	 */
	public static void fillOtherData(final File fullResultFile, final TestcaseType oneRunData, final String clazz, final String method, final String version) throws JAXBException {
		final XMLDataLoader fullDataLoader = new XMLDataLoader(fullResultFile);
		final Kopemedata fullResultData = fullDataLoader.getFullData();
		if (fullResultData.getTestcases().getTestcase().size() == 0) {
			fullResultData.getTestcases().setClazz(clazz);
			fullResultData.getTestcases().getTestcase().add(new TestcaseType());
			fullResultData.getTestcases().getTestcase().get(0).setName(method);
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

		final Fulldata realData = oneRunDatacollector.getResult().get(0).getFulldata();
		final double[][] values = new double[1][];
		if (realData != null && realData.getValue() != null && realData.getValue().size() > 0) {
			final SummaryStatistics st = new SummaryStatistics();
			createStatistics(st, realData, values);

			// final File chartfile = getFirstFreeFile(fullResultFile.getParentFile(), fullResultFile.getName(), ".png");
			// HistogramUtil.createHistogram(chartfile, values);

			final Result result = createResultFromStatistic(version, st);
			// result.setFulldata(new Fulldata());
			// result.getFulldata().getValue().addAll(realData.getValue());// Hack - why do we now need a different at all?

			fullFileDatacollector.getResult().add(result);
			XMLDataStorer.storeData(fullResultFile, fullResultData);
		} else {
			LOG.error("Achtung: Fulldata von " + fullResultFile + " leer!");
		}
	}

	private static Result createResultFromStatistic(final String version, final SummaryStatistics st) {
		final Result result = new Result();
		result.setValue(st.getMean());
		result.setMin((long) st.getMin());
		result.setMax((long) st.getMax());
		result.setVersion(new Versioninfo());
		result.getVersion().setGitversion(version);
		result.setDeviation(st.getStandardDeviation());
		result.setExecutionTimes(st.getN());
		return result;
	}

	private static void createStatistics(final SummaryStatistics st, final Fulldata realData, final double[][] values) {
		values[0] = new double[realData.getValue().size()];
		int i = 0;
		for (final Value value : realData.getValue()) {
			final long parseDouble = Long.parseLong(value.getValue());
			st.addValue(parseDouble);
			values[0][i++] = parseDouble;
		}
	}

	public static List<Double> getAverages(final List<Result> before) {
		return before.stream()
				.mapToDouble(beforeVal -> beforeVal.getFulldata().getValue().stream()
						.mapToDouble(val -> Double.parseDouble(val.getValue())).sum()
						/ beforeVal.getFulldata().getValue().size())
				.boxed().sorted().collect(Collectors.toList());
	}

	public static SummaryStatistics getStatistic(List<Result> results) {
		SummaryStatistics statistisc = new SummaryStatistics();
		results.forEach(result -> statistisc.addValue(result.getValue()));
		return statistisc;
	}

	public static int compareDouble(final List<Double> before, final List<Double> after) {
		boolean change = TestUtils.tTest(ArrayUtils.toPrimitive(before.toArray(new Double[0])), ArrayUtils.toPrimitive(after.toArray(new Double[0])), 0.05);
		SummaryStatistics statisticBefore = new SummaryStatistics();
		before.forEach(result -> statisticBefore.addValue(result));

		SummaryStatistics statisticAfter = new SummaryStatistics();
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
