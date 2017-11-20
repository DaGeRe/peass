package de.peran.measurement.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result;
import de.peran.AnalyseOneTest;
import de.peran.analysis.helper.MinimalExecutionDeterminer;
import de.peran.analysis.knowledge.Change;
import de.peran.analysis.knowledge.Changes;
import de.peran.analysis.knowledge.VersionKnowledge;
import de.peran.dependencyprocessors.VersionComparator;
import de.peran.measurement.analysis.statistics.ConfidenceIntervalInterpretion;
import de.peran.measurement.analysis.statistics.EvaluationPair;
import de.peran.measurement.analysis.statistics.Relation;
import de.peran.measurement.analysis.statistics.TestData;

/**
 * Analyzes full data and tells which version contain changes based upon given statistical tests (confidence interval, MannWhitney, ..)
 * 
 * @author reichelt
 *
 */
public class AnalyseFullData {
 
	private static final Logger LOG = LogManager.getLogger(AnalyseFullData.class);

	public static Set<String> versions = new HashSet<>();
	public static int testcases = 0, changes = 0;

	public static void analyseFolder(final File fullDataFolder) throws InterruptedException {
		LOG.info("Loading: {}", fullDataFolder);

		if (!fullDataFolder.exists()) {
			LOG.error("Ordner existiert nicht!");
			System.exit(1);
		}

		final LinkedBlockingQueue<TestData> measurements = DataReader.startReadVersionDataMap(fullDataFolder);

		TestData measurementEntry = measurements.take();

		while (measurementEntry != DataReader.POISON_PILL) {
			processTestdata(measurementEntry);

			measurementEntry = measurements.take();
		}
	}

	private static File myFile = new File("results_summary.csv");

	private static final File changeKnowledgeFile = new File(AnalyseOneTest.RESULTFOLDER, "changes.json");
	public static VersionKnowledge knowledge = new VersionKnowledge();
	public static VersionKnowledge oldKnowledge;

	private static BufferedWriter csvResultWriter;

	static {
		try {
			csvResultWriter = new BufferedWriter(new FileWriter(myFile));

			if (changeKnowledgeFile.exists()) {
				oldKnowledge = new ObjectMapper().readValue(changeKnowledgeFile, VersionKnowledge.class);
			} else {
				oldKnowledge = new VersionKnowledge();
			}
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void processTestdata(final TestData measurementEntry) {
		for (final Entry<String, EvaluationPair> entry : measurementEntry.getMeasurements().entrySet()) {
			final Changes changeList = knowledge.getVersion(entry.getKey());
			LOG.debug("Analysing: {} ({}#{}) Complete: {}", entry.getKey(), measurementEntry.getTestClass(), measurementEntry.getTestMethod(), entry.getValue().isComplete());

			// if (true || entry.getValue().isComplete()) {
			List<Result> previus = entry.getValue().getPrevius();
			List<Result> current = entry.getValue().getCurrent();

			if (previus.size() == 0 || current.size() == 0) {
				LOG.error("Data empty: {} {}", entry.getKey(), measurementEntry.getTestClass());
				if (entry.getValue().getPrevius().size() == 0) {
					LOG.error("Previous " + entry.getValue().getPreviusVersion() + " empty");
				}
				if (entry.getValue().getCurrent().size() == 0) {
					LOG.error("Previous " + entry.getValue().getVersion() + " empty");
				}
				return;
			}

//			int warmup, end;
//			if (previus.get(0).getFulldata().getValue().size() == 10000) {
//				warmup = 5000;
//				end = 10000;
//				LOG.debug("Values: {} {}", warmup, end);
//				previus = MinimalExecutionDeterminer.shortenValues(previus, warmup, end);
//				current = MinimalExecutionDeterminer.shortenValues(current, warmup, end);
//			} else {
				previus = MinimalExecutionDeterminer.cutValuesMiddle(previus);
				current = MinimalExecutionDeterminer.cutValuesMiddle(current);
//			}

			final int resultslength = Math.min(previus.size(), current.size());

			LOG.debug("Results: {}", resultslength);

			if (resultslength > 1) {
				final List<Result> prevResults = previus.subList(0, resultslength);
				final List<Result> currentResults = current.subList(0, resultslength);
				final Relation confidenceResult = ConfidenceIntervalInterpretion.compare(prevResults, currentResults);
				// final Relation anovaResult = ANOVATestWrapper.compare(prevResults, currentResults);
				final List<Double> before_double = MultipleVMTestUtil.getAverages(prevResults);
				final List<Double> after_double = MultipleVMTestUtil.getAverages(currentResults);
				final boolean change = TestUtils.tTest(ArrayUtils.toPrimitive(before_double.toArray(new Double[0])), ArrayUtils.toPrimitive(after_double.toArray(new Double[0])), 0.05);

				final double mean1 = ConfidenceIntervalInterpretion.getMean(prevResults);
				final double mean2 = ConfidenceIntervalInterpretion.getMean(currentResults);
				final int diff = (int) (((mean1 - mean2) * 10000) / mean1);
				// double anovaDeviation = ANOVATestWrapper.getANOVADeviation(prevResults, currentResults);
				LOG.debug("Means: {} {} Diff: {} % ", mean1, mean2, ((double) diff) / 100);

				if (change || Math.abs(diff) > 500) {
					Relation tRelation;
					if (diff > 0) {
						tRelation = Relation.LESS_THAN;
					} else {
						tRelation = Relation.GREATER_THAN;
					}
					changes++;
					final String viewName = "view_" + entry.getKey() + "/diffs/" + measurementEntry.getTestMethod() + ".txt";
					updateKnowledgeJSON(measurementEntry, entry, changeList, confidenceResult, tRelation, ((double) diff) / 100, viewName);

					LOG.info("Version: {} vs {} Klasse: {}#{}", entry.getKey(), entry.getValue().getPreviusVersion(), measurementEntry.getTestClass(),
							measurementEntry.getTestMethod());
					LOG.debug("Confidence Interval Comparison: {}", confidenceResult);

					// System.out.println(builderPrev.getDataSnapshot().getRunCount() + " " + builderCurrent.getDataSnapshot().getRunCount() + " " + prevResults.size() + " "
					// + currentResults.size());

					System.out.println("git diff " + entry.getKey() + ".." + VersionComparator.getPreviousVersion(entry.getKey()));
					System.out.println("vim " + viewName);
				}
			}
		}
		versions.addAll(measurementEntry.getMeasurements().keySet());
		LOG.debug("Version: {}", measurementEntry.getMeasurements().keySet());
		testcases += measurementEntry.getMeasurements().size();
	}

	private static void updateKnowledgeJSON(final TestData measurementEntry, final Entry<String, EvaluationPair> entry, final Changes changeList, final Relation confidenceResult,
			final Relation anovaResult, final double anovaDeviation, final String viewName) {
		try {

			final Change currentChange = changeList.addChange(measurementEntry.getTestClass(), viewName, measurementEntry.getTestMethod(), anovaDeviation);

			final Changes version = oldKnowledge.getVersion(entry.getKey());
			if (version != null) {
				final List<Change> oldChanges = version.getTestcaseChanges().get(measurementEntry.getTestClass());
				if (oldChanges != null) {
					for (final Change oldChange : oldChanges) {
						if (oldChange.getDiff().equals(viewName)) {
							currentChange.setCorrectness(oldChange.getCorrectness());
							currentChange.setType(oldChange.getType());
						}
					}
				}
			}

			AnalyseFullData.knowledge.setVersions(AnalyseFullData.versions.size());
			AnalyseFullData.knowledge.setTestcases(AnalyseFullData.testcases);
			AnalyseFullData.knowledge.setChanges(AnalyseFullData.changes);

			final ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
			objectMapper.writeValue(changeKnowledgeFile, knowledge);

			csvResultWriter.write(entry.getKey() + ";" + "vim " + viewName + ";" + measurementEntry.getTestClass() + ";" + anovaResult + ";" + confidenceResult + "\n");
			csvResultWriter.flush();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
