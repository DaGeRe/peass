package de.peass.steadyStateNodewise;

import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

public class PerVMDeviationDeriver {
	
		private final String name;
		private final DescriptiveStatistics vmDeviations = new DescriptiveStatistics();
		private final DescriptiveStatistics valueMean = new DescriptiveStatistics();
		private final DescriptiveStatistics measurementCount = new DescriptiveStatistics();
		private final DescriptiveStatistics repetitionCount = new DescriptiveStatistics();

		public PerVMDeviationDeriver(String name, Collection<List<StatisticalSummary>> values) {
			this.name = name;
			for (List<StatisticalSummary> vmStart : values) {
				int repetitions = 0;
				DescriptiveStatistics vmStartStatistics = new DescriptiveStatistics();
				for (StatisticalSummary vmPart : vmStart) {
					vmStartStatistics.addValue(vmPart.getMean());
					repetitions+=vmPart.getN();
				}
				final double relativeDeviation = vmStartStatistics.getStandardDeviation() / vmStartStatistics.getMean();
//		         System.out.println(relativeDeviation);
				vmDeviations.addValue(relativeDeviation);
				valueMean.addValue(vmStartStatistics.getMean());
				measurementCount.addValue(vmStart.size());
				repetitionCount.addValue(repetitions);
			}
		}

		public void printDeviations() {
			System.out.println(name + " " + vmDeviations.getMean() + " Mean: " + valueMean.getMean() + " " + measurementCount.getMean() + " " + repetitionCount.getMean() + " " + vmDeviations.getN());
		}
	}