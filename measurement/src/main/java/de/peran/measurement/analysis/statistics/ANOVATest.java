package de.peran.measurement.analysis.statistics;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result;

public class ANOVATest {

	private static final Logger LOG = LogManager.getLogger(ANOVATest.class);

	private ANOVATest() {

	}

	public static List<Double> getAverages(final List<Result> before) {
		return before.stream()
				.mapToDouble(beforeVal -> beforeVal.getFulldata().getValue().stream()
						.mapToDouble(val -> Double.parseDouble(val.getValue())).sum()
						/ beforeVal.getFulldata().getValue().size())
				.boxed().sorted().collect(Collectors.toList());
	}
	
	public static SummaryStatistics getStatistic(List<Result> results){
		SummaryStatistics statistisc = new SummaryStatistics();
		results.forEach(result -> statistisc.addValue(result.getValue()));
		return statistisc;
	}

	public static int compareDouble(final List<Double> before, final List<Double> after, final double difference) {
		if (before.size() != after.size()) {
			throw new RuntimeException("Unexpected: Compared distributions should be equally long, are: "
					+ before.size() + " " + after.size() + " long.");
		}
		LOG.trace("Size: {} {}", before.size(), after.size());
		LOG.trace(before);
		LOG.trace(after);

		final double sum_before = before.stream().mapToDouble(val -> val).sum();
		final double sum_after = after.stream().mapToDouble(val -> val).sum();

		final double mean_before = sum_before / before.size();
		final double mean_after = sum_after / after.size();

		final double mean = (sum_before + sum_after) / (before.size() + after.size());

		final double sum_squares_alternatives = before.size()
				* (Math.pow(mean - mean_before, 2) + Math.pow(mean - mean_after, 2));

		final double errors_before = before.stream().mapToDouble(d -> d).map(value -> Math.pow(mean_before - value, 2))
				.sum();
		final double errors_after = after.stream().mapToDouble(d -> d).map(value -> Math.pow(mean_after - value, 2))
				.sum();
		final double sum_squares_errors = errors_before + errors_after;

		final double sum_squares_total = sum_squares_alternatives + sum_squares_errors;

		LOG.debug("E: {} {}", errors_before, errors_after);
		LOG.debug("SSE: {} SSA: {} Version-Intern Error: {}% Version-Alternative Error: {}%", sum_squares_errors,
				sum_squares_alternatives, Math.round(10000 * sum_squares_errors / sum_squares_total) / 100d,
				Math.round(10000 * sum_squares_alternatives / sum_squares_total) / 100d);

//		double factor = 1d/(before.size() - 1);
//		System.out.println(factor + " " + before.size() );
//		double potential_f = sum_squares_alternatives / (factor * sum_squares_errors);
//		System.out.println("F: " + potential_f);

		if (sum_squares_alternatives > sum_squares_errors * difference) {
			if (mean_before < mean_after)
				return -1;
			else
				return 1;
		} else {
			return 0;
		}
	}

	public static double getDeviation(final List<Double> before, final List<Double> after) {
		if (before.size() != after.size()) {
			throw new RuntimeException("Unexpected: Compared distributions should be equally long, are: "
					+ before.size() + " " + after.size() + " long.");
		}
		LOG.trace("Size: {} {}", before.size(), after.size());
		LOG.trace(before);
		LOG.trace(after);

		final double sum_before = before.stream().mapToDouble(val -> val).sum();
		final double sum_after = after.stream().mapToDouble(val -> val).sum();

		final double mean_before = sum_before / before.size();
		final double mean_after = sum_after / after.size();

		final double mean = (sum_before + sum_after) / (before.size() + after.size());

		final double sum_squares_alternatives = before.size()
				* (Math.pow(mean - mean_before, 2) + Math.pow(mean - mean_after, 2));

		final double errors_before = before.stream().mapToDouble(d -> d).map(value -> Math.pow(mean_before - value, 2))
				.sum();
		final double errors_after = after.stream().mapToDouble(d -> d).map(value -> Math.pow(mean_after - value, 2))
				.sum();
		final double sum_squares_errors = errors_before + errors_after;

		final double sum_squares_total = sum_squares_alternatives + sum_squares_errors;

		LOG.debug("E: {} {}", errors_before, errors_after);
		LOG.debug("SSE: {} SSA: {} Version-Intern Error: {}% Version-Alternative Error: {}%", sum_squares_errors,
				sum_squares_alternatives, Math.round(10000 * sum_squares_errors / sum_squares_total) / 100d,
				Math.round(10000 * sum_squares_alternatives / sum_squares_total) / 100d);

		if (mean_before > mean_after) {
			return Math.round(10000 * sum_squares_alternatives / sum_squares_total) / 100d;
		} else {
			return -Math.round(10000 * sum_squares_alternatives / sum_squares_total) / 100d;
		}
	}

	public static int compareDouble(final List<Double> before, final List<Double> after) {
		return compareDouble(before, after, 0.95);
	}
}
