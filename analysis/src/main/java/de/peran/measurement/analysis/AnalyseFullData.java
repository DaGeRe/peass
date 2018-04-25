package de.peran.measurement.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
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
public class AnalyseFullData extends DataAnalyser {

	private static final Logger LOG = LogManager.getLogger(AnalyseFullData.class);

	public static Set<String> versions = new HashSet<>();
	public static int testcases = 0, changes = 0;

//	public static void analyseFolder(final File fullDataFolder) throws InterruptedException {
//		LOG.info("Loading: {}", fullDataFolder);
//
//		if (!fullDataFolder.exists()) {
//			LOG.error("Ordner existiert nicht!");
//			System.exit(1);
//		}
//
//		final LinkedBlockingQueue<TestData> measurements = DataReader.startReadVersionDataMap(fullDataFolder);
//
//		TestData measurementEntry = measurements.take();
//
//		while (measurementEntry != DataReader.POISON_PILL) {
//			processTestdata(measurementEntry);
//
//			measurementEntry = measurements.take();
//		}
//	}

	private static File myFile = new File("results_summary.csv");

	private static final File changeKnowledgeFile = new File(AnalyseOneTest.RESULTFOLDER, "changes.json");
	public static final VersionKnowledge knowledge = new VersionKnowledge();
	public static final VersionKnowledge oldKnowledge = new VersionKnowledge();

	private static BufferedWriter csvResultWriter;

	static {
		try {
			csvResultWriter = new BufferedWriter(new FileWriter(myFile));
			for (final File potentialKnowledgeFile : AnalyseOneTest.RESULTFOLDER.listFiles()) {
				if (!potentialKnowledgeFile.isDirectory()) {
					final VersionKnowledge knowledge = new ObjectMapper().readValue(potentialKnowledgeFile, VersionKnowledge.class);
					for (final Map.Entry<String, Changes> oldFileEntry : knowledge.getVersionChanges().entrySet()) {
						final Changes version = oldKnowledge.getVersion(oldFileEntry.getKey());
						if (version == null) {
							oldKnowledge.getVersionChanges().put(oldFileEntry.getKey(), oldFileEntry.getValue());
						} else {
							for (final Map.Entry<String, List<Change>> versionEntry : oldFileEntry.getValue().getTestcaseChanges().entrySet()) {
								final List<Change> changes = version.getTestcaseChanges().get(versionEntry.getKey());
								if (changes == null) {
									version.getTestcaseChanges().put(versionEntry.getKey(), versionEntry.getValue());
								} else {
									for (final Change oldChange : versionEntry.getValue()) {
										boolean found = false;
										for (final Change change : changes) {
											if (change.getDiff().equals(oldChange.getDiff())) {
												found = true;
												if (oldChange.getType() != null) {
													change.setType(oldChange.getType());
												}
												if (oldChange.getCorrectness() != null) {
													change.setCorrectness(oldChange.getCorrectness());
												}
											}
										}
										if (!found) {
											changes.add(oldChange);
										}
									}
								}
							}
						}
					}
				}

				// if (changeKnowledgeFile.exists()) {
				// oldKnowledge = new ObjectMapper().readValue(changeKnowledgeFile, VersionKnowledge.class);
				// } else {
				// oldKnowledge = new VersionKnowledge();
				// }
			}

		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private static final double CONFIDENCE = 0.01;

	@Override
	public void processTestdata(final TestData measurementEntry) {
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

			// int warmup, end;
			// if (previus.get(0).getFulldata().getValue().size() == 10000) {
			// warmup = 5000;
			// end = 10000;
			// LOG.debug("Values: {} {}", warmup, end);
			// previus = MinimalExecutionDeterminer.shortenValues(previus, warmup, end);
			// current = MinimalExecutionDeterminer.shortenValues(current, warmup, end);
			// } else {
			previus = MinimalExecutionDeterminer.cutValuesMiddle(previus);
			current = MinimalExecutionDeterminer.cutValuesMiddle(current);
			// }

			final int resultslength = Math.min(previus.size(), current.size());

			LOG.debug("Results: {}", resultslength);

			if (resultslength > 1) {
				removeOutliers(previus);
				removeOutliers(current);
				final DescriptiveStatistics statistics1 = ConfidenceIntervalInterpretion.getStatistics(previus);
				final DescriptiveStatistics statistics2 = ConfidenceIntervalInterpretion.getStatistics(current);

				final List<Double> before_double = MultipleVMTestUtil.getAverages(previus);
				final List<Double> after_double = MultipleVMTestUtil.getAverages(current);

				// final List<Result> prevResults = previus.subList(0, resultslength);
				// final List<Result> currentResults = current.subList(0, resultslength);
				final Relation confidenceResult = ConfidenceIntervalInterpretion.compare(previus, current);
				// final Relation anovaResult = ANOVATestWrapper.compare(prevResults, currentResults);

				final DescriptiveStatistics ds = new DescriptiveStatistics(ArrayUtils.toPrimitive(before_double.toArray(new Double[0])));
				final DescriptiveStatistics ds2 = new DescriptiveStatistics(ArrayUtils.toPrimitive(after_double.toArray(new Double[0])));
				LOG.debug(ds.getMean() + " " + ds2.getMean() + " " + ds.getStandardDeviation() + " " + ds2.getStandardDeviation());

				final double tValue = TestUtils.t(ArrayUtils.toPrimitive(before_double.toArray(new Double[0])), ArrayUtils.toPrimitive(after_double.toArray(new Double[0])));
				final boolean change = TestUtils.tTest(ArrayUtils.toPrimitive(before_double.toArray(new Double[0])), ArrayUtils.toPrimitive(after_double.toArray(new Double[0])), CONFIDENCE);

				final int diff = (int) (((statistics1.getMean() - statistics2.getMean()) * 10000) / statistics1.getMean());
				// double anovaDeviation = ANOVATestWrapper.getANOVADeviation(prevResults, currentResults);
				LOG.debug("Means: {} {} Diff: {} % T-Value: {} Change: {}", statistics1.getMean(), statistics2.getMean(), ((double) diff) / 100, tValue, change);

				if (change) {
					Relation tRelation;
					if (diff > 0) {
						tRelation = Relation.LESS_THAN;
					} else {
						tRelation = Relation.GREATER_THAN;
					}
					changes++;
					final String viewName = "view_" + entry.getKey() + "/diffs/" + measurementEntry.getTestMethod() + ".txt";
					updateKnowledgeJSON(measurementEntry, entry, changeList, confidenceResult, tRelation, ((double) diff) / 100, viewName, tValue);

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

	private static void removeOutliers(List<Result> previus) {
		final DescriptiveStatistics statistics1 = ConfidenceIntervalInterpretion.getStatistics(previus);
		for (final Iterator<Result> result = previus.iterator(); result.hasNext();) {
			final Result r = result.next();
			final double diff = Math.abs(r.getValue() - statistics1.getPercentile(50));
			final double z = diff / statistics1.getStandardDeviation();
			LOG.debug("Val: {} Z: {} Remove: {}", r.getValue(), z, z > 3);
			if (z > 3) {
				result.remove();
			}
		}
	}

	private static void updateKnowledgeJSON(final TestData measurementEntry, final Entry<String, EvaluationPair> entry, final Changes changeList, final Relation confidenceResult,
			final Relation anovaResult, final double anovaDeviation, final String viewName, final double tvalue) {
		try {

			final Change currentChange = changeList.addChange(measurementEntry.getTestClass(), viewName, measurementEntry.getTestMethod(), anovaDeviation, tvalue);

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
