package de.dagere.peass.breaksearch.minimalvalues;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.measurement.dataloading.MultipleVMTestUtil;
import de.dagere.peass.measurement.statistics.data.EvaluationPair;
import de.dagere.peass.measurement.statistics.data.TestData;

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

	abstract int getSize(List<VMResult> results);

	abstract int getMin(List<VMResult> results); 

	abstract int getChange(List<VMResult> results);
 
	abstract int analyzeMeasurement(int oldResult, List<VMResult> current, List<VMResult> before);

	public void processTestdata(final TestData measurementEntry) {
		LOG.debug(measurementEntry.getTestCase());
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

	List<Double> getValues(final List<VMResult> current) {
		final List<Double> values = new LinkedList<>();
		for (final VMResult r : current) {
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