package de.peass.statistics;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.stat.descriptive.StorelessUnivariateStatistic;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;

public final class MeasurementAnalysationUtil {

	private static final class DummyStatistic implements StorelessUnivariateStatistic {
      @Override
      public double evaluate(final double[] values, final int begin, final int length) throws MathIllegalArgumentException {
         return 0;
      }

      @Override
      public double evaluate(final double[] values) throws MathIllegalArgumentException {
         return 0;
      }

      @Override
      public void incrementAll(final double[] values, final int start, final int length) throws MathIllegalArgumentException {
         // TODO Auto-generated method stub
         
      }

      @Override
      public void incrementAll(final double[] values) throws MathIllegalArgumentException {
      }

      @Override
      public void increment(final double d) {
      }

      @Override
      public double getResult() {
         return 0;
      }

      @Override
      public long getN() {
         return 0;
      }

      @Override
      public StorelessUnivariateStatistic copy() {
         return null;
      }

      @Override
      public void clear() {
      }
   }

   private MeasurementAnalysationUtil() {

	}

	private static final Logger LOG = LogManager.getLogger(MeasurementAnalysationUtil.class);

	static long earliestVersion = Long.MAX_VALUE;
	static long lastVersion = Long.MIN_VALUE;

	public static final double MIN_NORMED_DISTANCE = 0.5;
	public static final double MIN_ABSOLUTE_PERCENTAGE_DISTANCE = 0.2;

	private static final Random RANDOM = new Random();

	public static Map<File, Kopemedata> getData(final File file) throws JAXBException {
		final Map<File, Kopemedata> data = new HashMap<>();
		LOG.debug("Analysiere: {}", file);
		if (file.isDirectory()) {
			final Collection<File> fileList = FileUtils.listFiles(file, new WildcardFileFilter("*.xml"), FalseFileFilter.INSTANCE);
			for (final File xmlFile : fileList) {
				LOG.trace("Datei: {}", xmlFile);
				final XMLDataLoader xdl = new XMLDataLoader(xmlFile);
				data.put(xmlFile, xdl.getFullData());
			}
		}
		return data;
	}

	public static List<PerformanceChange> analyzeKopemeData(final Kopemedata data) {
		final Map<String, List<Result>> results = new LinkedHashMap<>();
		int maxResultSize = 0;
		final TestcaseType currentTestcase = data.getTestcases().getTestcase().get(0);
		final String clazz = data.getTestcases().getClazz();
		final String method = currentTestcase.getName();
		final List<Datacollector> datacollectors = currentTestcase.getDatacollector();
		if (datacollectors.size() != 1) {
			LOG.warn("Mehr als ein DataCollector bei: {}", method);
		}
		for (final Result result : datacollectors.get(0).getResult()) {
			final String gitversion = result.getVersion().getGitversion();
			if (!results.containsKey(gitversion)) {
				results.put(gitversion, new LinkedList<>());
			}
			results.get(gitversion).add(result);
			if (results.get(gitversion).size() > maxResultSize) {
				maxResultSize = results.get(gitversion).size();
			}
		}

		ConfidenceInterval previous = null;
		String previousVersion = null;
		final List<PerformanceChange> changes = new LinkedList<>();

		final ExecutorService service = Executors.newFixedThreadPool(4);

		for (final Map.Entry<String, List<Result>> entry : results.entrySet()) {
			final double[] values = getAveragesArrayFromResults(entry.getValue());
			final ConfidenceInterval interval = getBootstrapConfidenceInterval(values, 20, 1000, 96);
			LOG.trace("{}-Konfidenzintervall: {} - {}", interval.getPercentage(), interval.getMin(), interval.getMax());
			if (previous != null) {
				final ConfidenceInterval previousConfidenceInterval = previous;
				final String previousVersion2 = previousVersion;
				LOG.trace("Start " + previousVersion2);
				service.execute(new Runnable() {
					@Override
					public void run() {
						final String currentVersion = entry.getValue().get(0).getVersion().getGitversion();
						final PerformanceChange change = new PerformanceChange(previousConfidenceInterval, interval, clazz, method, previousVersion2, currentVersion);
						final boolean isChange = analysePotentialChange(change, previousConfidenceInterval, entry, interval);
						if (isChange) {
							changes.add(change);
						}
					}
				});
			}
			previous = interval;
			previousVersion = entry.getKey();
		}
		try {
			service.shutdown();
			service.awaitTermination(1, TimeUnit.DAYS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
		return changes;
	}

	public static double[] getAveragesArrayFromResults(final List<Result> results) {
		final double[] values = new double[results.size()];
		int i = 0;
		for (final Result result : results) {
			final double value = result.getValue();
			values[i++] = value;
		}
		return values;
	}

	public static List<Result> getResult(final Kopemedata data) {
		return data.getTestcases().getTestcase().get(0).getDatacollector().get(0).getResult();
	}

	private static boolean analysePotentialChange(final PerformanceChange change, final ConfidenceInterval previous, final Map.Entry<String, List<Result>> entry, final ConfidenceInterval interval) {
		LOG.trace("Vergleiche: {} {} Version: {}", change.getTestClass(), change.getTestMethod(), change.getRevisionOld());
		boolean isChange = false;
		final double diff = change.getDifference();
		LOG.debug("Teste: {}:{} - {} vs. vorher {}", change.getRevision(), change.getRevisionOld(), interval, previous);
		if (interval.getMax() < previous.getMin()) {
			if (change.getNormedDifference() > MIN_NORMED_DISTANCE && diff > MIN_ABSOLUTE_PERCENTAGE_DISTANCE * previous.getMax()) {
				LOG.debug("Änderung: {} {} Diff: {}", change.getRevisionOld(), change.getTestMethod(), diff);
				LOG.debug("Ist kleiner geworden: {} vs. vorher {}", interval, previous);
				LOG.trace("Abstand: {} Versionen: {}:{}", diff, change.getRevisionOld(), entry.getKey());
				isChange = true;
			}
		}
		if (interval.getMin() > previous.getMax()) {
			if (change.getNormedDifference() > MIN_NORMED_DISTANCE && diff > MIN_ABSOLUTE_PERCENTAGE_DISTANCE * previous.getMax()) {
				LOG.debug("Änderung: {} {} Diff: {}", change.getRevisionOld(), change.getTestMethod(), diff);
				LOG.debug("Ist größer geworden: {} vs. vorher {}", interval, previous);
				LOG.trace("Abstand: {} Versionen: {}:{}", diff, change.getRevisionOld(), entry.getKey());
				isChange = true;
			}
		}
		return isChange;
	}

	public static ConfidenceInterval getBootstrapConfidenceInterval(final double[] values, final int count, final int repetitions, final int intervalPercentage) {
		LOG.trace("Werte: {}", values);
		final double[] means = getMeanStatistics(values, count, repetitions);

//		LOG.trace("Mean: {}", statistics.getMean());

		final double upperBound = new Percentile(intervalPercentage).evaluate(means);
		final double lowerBound = new Percentile(100 - intervalPercentage).evaluate(means);;
		return new ConfidenceInterval(lowerBound, upperBound, intervalPercentage);
	}

   private static double[] getMeanStatistics(final double[] values, final int count, final int repetitions) {
      double[] meanValues = new double[repetitions];
		for (int i = 0; i < repetitions; i++) {
			final double bootstrapMean = getBootstrappedStatistics(values, count);
			meanValues[i] = bootstrapMean;
		}
      return meanValues;
   }
   
   private static double getBootstrappedStatistics(final double[] values, final int count) {
      final SummaryStatistics st = new SummaryStatistics();
      st.setSumLogImpl(new DummyStatistic());
      for (int i = 0; i < count; i++) {
         final int nextInt = RANDOM.nextInt(values.length);
         // System.out.println(nextInt);
         st.addValue(values[nextInt]);
      }
      return st.getMean();
   } 

//   private static double getBootstrappedStatistics(final double[] values, final int count) {
//      final double[] tempValues = getBootstrapValues(values, count);
//      final SummaryStatistics st = new SummaryStatistics();
//      st.setSumLogImpl(new DummyStatistic());
//      for (double value : tempValues) {
//         st.addValue(value);
//      }
//      return st.getMean();
//   }

//	public static double[] getBootstrapValues(final double[] values, final int count) {
//		final double[] result = new double[count];
//		for (int i = 0; i < count; i++) {
//			final int nextInt = RANDOM.nextInt(values.length);
//			// System.out.println(nextInt);
//			result[i] = values[nextInt];
//		}
//		return result;
//	}
}
