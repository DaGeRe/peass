package de.peran.measurement.analysis.statistics;

/*-
 * #%L
 * peran-analysis
 * %%
 * Copyright (C) 2015 - 2017 DaGeRe
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result.Fulldata.Value;

public class MeanCoVData {

	private int avgCount = 200;

	final List<DescriptiveStatistics> allMeans = new LinkedList<>();
	final List<DescriptiveStatistics> allCoVs = new LinkedList<>();

	public List<DescriptiveStatistics> getAllMeans() {
		return allMeans;
	}

	public List<DescriptiveStatistics> getAllCoVs() {
		return allCoVs;
	}

	final String name;
	final List<Result> results;
	// final TestcaseType testcase;

	public MeanCoVData(final TestcaseType testcase, int avg_count) {
		this.name = testcase.getName();
		this.results = testcase.getDatacollector().get(0).getResult();
		this.avgCount = avg_count;
		// this.testcase = testcase;
		addTestcaseData();
	}

	public MeanCoVData(final String name, final List<Result> results) {
		this.name = name;
		this.results = results;
		addTestcaseData();
	}

	private void addTestcaseData() {
		for (final Result result : results) {
			DescriptiveStatistics statistics = new DescriptiveStatistics();
			int index = 0;
			for (final Value value : result.getFulldata().getValue()) {
				// writer.write(value.getValue() + "\n");
				statistics.addValue(Double.parseDouble(value.getValue()));
				if (statistics.getValues().length == avgCount) {
					final double cov = statistics.getVariance() / statistics.getMean();
					addValue(index, statistics.getMean(), allMeans);
					addValue(index, cov, allCoVs);
					index++;
					statistics = new DescriptiveStatistics();
				}
			}
		}
	}

	public void printTestcaseData(final File folder) throws IOException {
		for (final Result result : results) {
			final File csvFile = new File(folder, "result_" + name + "_" + result.getDate() + ".csv");
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
				DescriptiveStatistics statistics = new DescriptiveStatistics();
				for (final Value value : result.getFulldata().getValue()) {
					// writer.write(value.getValue() + "\n");
					statistics.addValue(Double.parseDouble(value.getValue()));
					if (statistics.getValues().length == avgCount) {
						final double cov = statistics.getVariance() / statistics.getMean();
						writer.write(statistics.getMean() + ";" + cov + "\n");
						statistics = new DescriptiveStatistics();
					}
				}
				writer.flush();
				// System.out.println("set title 'Mean and Coefficient of Variation for " + clazzname + "." + testcase.getName() + "'");
				// System.out.println("set y2range [0:5]");
				// System.out.println("set y2tics");
				// System.out.println("set datafile separator ';'");
				// System.out.println("plot '" + csvFile.getName() + "' u ($0*" + AVG_COUNT + "):1 title 'Mean', '" + csvFile.getName() + "' u ($0*" + AVG_COUNT + "):2 title 'CoV' axes x1y2");
				System.out.print(", '" + csvFile.getName() + "' u ($0*" + avgCount + "):1 title 'Mean'");
				// System.out.println();
			}
		}
		System.out.println();
	}

	/**
	 * Writes the average values to a csv-file in the given folder and returns the filename.
	 * @param folder	Destination folder for the result-csv
	 * @param clazzname	Name of the class
	 * @return	Written CSV
	 * @throws IOException
	 */
	public File printAverages(final File folder, final String clazzname) throws IOException {
		final File summaryFile = new File(folder, "result_" + clazzname + "_" + name + "_all.csv");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(summaryFile))) {
			for (int i = 0; i < allMeans.size(); i++) {
				writer.write(allMeans.get(i).getMean() + ";" + allCoVs.get(i).getMean() + "\n");
			}
			writer.flush();

			System.out.println("set title 'Mean Mean and Mean Coefficient of Variation for " + clazzname + "." + name + "'");
			System.out.println("plot '" + summaryFile.getName() + "' u ($0*" + avgCount + "):1 title 'Mean', '" + summaryFile.getName() + "' u ($0*" + avgCount + "):2 title 'CoV' axes x1y2");
			System.out.println();
		}
		return summaryFile;
	}

	private void addValue(final int index, final double value, final List<DescriptiveStatistics> statistics) {
		DescriptiveStatistics meanSummary;
		if (statistics.size() <= index) {
			meanSummary = new DescriptiveStatistics();
			statistics.add(meanSummary);
		} else {
			meanSummary = statistics.get(index);
		}
		meanSummary.addValue(value);
	}
}
