package de.peran.tracecomparison;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import kieker.tools.traceAnalysis.systemModel.Execution;

public class ExecutionConsumer {

	private static final Logger LOG = LogManager.getLogger(ExecutionConsumer.class);

	public static void compareExecutions(List<Execution> firstVersion, List<Execution> secondVersion) {
		final DescriptiveStatistics statistics = getStatistics(firstVersion);
		final DescriptiveStatistics statistics2 = getStatistics(secondVersion);
		final String easySignature = firstVersion.get(0).getAllocationComponent().getAssemblyComponent().getType().getFullQualifiedName() + "."
				+ firstVersion.get(0).getOperation().getSignature().getName();

		for (final Execution exec : firstVersion) {
			final String easySignature2 = exec.getAllocationComponent().getAssemblyComponent().getType().getFullQualifiedName() + "." + exec.getOperation().getSignature().getName();
			if (!easySignature.equals(easySignature2)) {
				throw new RuntimeException("Testcase is non-deterministic");
			}
		}

		try {
			final File results = new File("results");
			if (!results.exists()) {
				results.mkdir();
			}
			final File file = new File(results, firstVersion.get(0).getOperation().getSignature().getName());
			final FileWriter fw = new FileWriter(file, true);
			fw.write(statistics.getMean() + ";" + statistics.getStandardDeviation() + ";" +
					statistics2.getMean() + ";" + statistics2.getStandardDeviation() + ";"
					+ statistics.getN() + "\n");
			fw.flush();
			fw.close();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		final double relativeDiff = (statistics.getMean() - statistics2.getMean()) / statistics.getMean();
		final double averageDeviation = (statistics.getStandardDeviation() + statistics2.getStandardDeviation()) / 2;

		// System.out.println(relativeDiff + " " + averageDeviation);
		// if (Math.abs(relativeDiff) > Math.abs(averageDeviation)){
		if (new TTest().tTest(statistics, statistics2, 0.02)) {
			LOG.info("Duration differs: " + relativeDiff + " " + firstVersion.get(0).getOperation().getSignature());
		} else {
			LOG.debug("Duration equal: " + relativeDiff + " " + firstVersion.get(0).getOperation().getSignature());
		}
	}

	public static DescriptiveStatistics getStatistics(List<Execution> executions) {
		final DescriptiveStatistics statistics = new DescriptiveStatistics();
		for (final Execution ex : executions) {
			final long duration = ex.getTout() - ex.getTin();
			statistics.addValue(duration);
		}
		return statistics;
	}

}