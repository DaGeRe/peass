package de.peran.analysis.helper;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result;
import de.peran.measurement.analysis.MultipleVMTestUtil;
import de.peran.measurement.analysis.statistics.EvaluationPair;
import de.peran.measurement.analysis.statistics.TestData;

/**
 * Determines the minimal value of a parameter, which keeps the same result of statistical analysis, i.e.
 * if the analysis says that the result was getting greater, it determines the minimal value where the analysis
 * still says that the result is getting greater.
 * @author reichelt
 *
 */
abstract class MinimalValueDeterminer {
	private static final Logger LOG = LogManager.getLogger(MinimalValueDeterminer.class);

	private int count;
	private int sum;
	private int minNeccessaryValue;
	private final List<Integer> values = new LinkedList<>();

	abstract int getSize(List<Result> results);

	abstract int getMin(List<Result> results); 

	abstract int getChange(List<Result> results);
 
	abstract int analyzeMeasurement(int oldResult, List<Result> current, List<Result> before);

	public void processTestdata(final TestData measurementEntry) {
		LOG.debug(measurementEntry.getTestClass() + " " + measurementEntry.getTestMethod());
		for (final Map.Entry<String, EvaluationPair> entry : measurementEntry.getMeasurements().entrySet()) {
			if (entry.getValue().getCurrent().size() == entry.getValue().getPrevius().size()) {
				final List<Double> allStatistics = getValues(entry.getValue().getCurrent());
				final List<Double> allStatisticsBefore = getValues(entry.getValue().getPrevius());

				LOG.debug(allStatistics.size() + " " + allStatisticsBefore.size());
				final int oldResult = MultipleVMTestUtil.compareDouble(allStatistics, allStatisticsBefore);

				final int localMinValue = analyzeMeasurement(oldResult, entry.getValue().getCurrent(), entry.getValue().getPrevius());

				LOG.debug("Min value: " + localMinValue);

				if (localMinValue > getMinNeccessaryValue()) {
					setMinNeccessaryValue(localMinValue);
				}
				getValues().add(localMinValue);
				setSum(getSum() + localMinValue);
				setCount(getCount() + 1);
			}
		}
	}

	List<Double> getValues(final List<Result> current) {
		final List<Double> values = new LinkedList<>();
		for (final Result r : current) {
			values.add(r.getValue());
		}
		return values;
	}

	public int getSum() {
		return sum;
	}

	public void setSum(final int sum) {
		this.sum = sum;
	}

	public int getMinNeccessaryValue() {
		return minNeccessaryValue;
	}

	public void setMinNeccessaryValue(final int minNeccessaryValue) {
		this.minNeccessaryValue = minNeccessaryValue;
	}

	public List<Integer> getValues() {
		return values;
	}

	public int getCount() {
		return count;
	}

	public void setCount(final int count) {
		this.count = count;
	}
}