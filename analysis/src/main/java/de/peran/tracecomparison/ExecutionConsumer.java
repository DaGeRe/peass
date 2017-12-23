package de.peran.tracecomparison;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peran.CompareTraces;
import kieker.tools.traceAnalysis.systemModel.Execution;

public class ExecutionConsumer {

	private static final Logger LOG = LogManager.getLogger(ExecutionConsumer.class);

//	private final List<Thread> threads;
//	private final Map<Integer, ExecutionData> executions;
//	private final FileWriter destWriter;
//
//	public ExecutionConsumer(List<Thread> threads, Map<Integer, ExecutionData> executions, File destFile) throws IOException {
//		this.threads = threads;
//		this.executions = executions;
//		destWriter = new FileWriter(destFile);
//	}

//	@Override
//	public void run() {
//		ExecutionData list1 = executions.get(0);
//		ExecutionData list2 = executions.get(1);
//		boolean isAlive = true;
//		boolean executionsExisting = true;
//		while (isAlive || executionsExisting) {
//			isAlive = false;
//			for (Thread t : threads) {
//				isAlive = isAlive || t.isAlive();
//			}
//			executionsExisting = false;
//			for (ExecutionData data : executions.values()) {
//				executionsExisting = executionsExisting || data.hasValues();
//			}
//			LOG.debug("IsAlive: {} Execution existing: {}", isAlive, executionsExisting);
//			synchronized (list1) {
//				synchronized (list2) {
//					int count = Math.min(list1.getPoppableExecutions(), list2.getPoppableExecutions());
//					// LOG.debug("Remaining: " + count);
//					while (count > 0) {
//						List<Execution> firstVersion = list1.popExecution();
//						List<Execution> secondVersion = list2.popExecution();
//						compareExecutions(firstVersion, secondVersion);
//						count--;
//					}
//				}
//			}
//
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	}

	public static void compareExecutions(List<Execution> firstVersion, List<Execution> secondVersion) {
		DescriptiveStatistics statistics = getStatistics(firstVersion);
		DescriptiveStatistics statistics2 = getStatistics(secondVersion);
		String easySignature = firstVersion.get(0).getAllocationComponent().getAssemblyComponent().getType().getFullQualifiedName() + "."
				+ firstVersion.get(0).getOperation().getSignature().getName();

		for (Execution exec : firstVersion) {
			String easySignature2 = exec.getAllocationComponent().getAssemblyComponent().getType().getFullQualifiedName() + "." + exec.getOperation().getSignature().getName();
			if (!easySignature.equals(easySignature2)) {
				throw new RuntimeException("Testcase is non-deterministic");
			}
		}

		try {
			File results = new File("results");
			if (!results.exists()) {
				results.mkdir();
			}
			File file = new File(results, firstVersion.get(0).getOperation().getSignature().getName());
			FileWriter fw = new FileWriter(file, true);
			fw.write(statistics.getMean() + ";" + statistics.getStandardDeviation() + ";" +
					statistics2.getMean() + ";" + statistics2.getStandardDeviation() + ";"
					+ statistics.getN() + "\n");
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		double relativeDiff = ((double) statistics.getMean() - statistics2.getMean()) / statistics.getMean();
		double averageDeviation = ((double) statistics.getStandardDeviation() + statistics2.getStandardDeviation()) / 2;

		// System.out.println(relativeDiff + " " + averageDeviation);
		// if (Math.abs(relativeDiff) > Math.abs(averageDeviation)){
		if (new TTest().tTest(statistics, statistics2, 0.02)) {
			LOG.info("Duration differs: " + relativeDiff + " " + firstVersion.get(0).getOperation().getSignature());
		} else {
			LOG.debug("Duration equal: " + relativeDiff + " " + firstVersion.get(0).getOperation().getSignature());
		}
	}

	public static DescriptiveStatistics getStatistics(List<Execution> executions) {
		DescriptiveStatistics statistics = new DescriptiveStatistics();
		for (Execution ex : executions) {
			long duration = ex.getTout() - ex.getTin();
			statistics.addValue(duration);
		}
		return statistics;
	}

}