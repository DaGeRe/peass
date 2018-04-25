package de.peran;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peran.measurement.analysis.CompareByFulldata;
import de.peran.measurement.analysis.DataAnalyser;
import de.peran.measurement.analysis.statistics.ConfidenceIntervalInterpretion;
import de.peran.measurement.analysis.statistics.EvaluationPair;
import de.peran.measurement.analysis.statistics.TestData;
import de.peran.utils.OptionConstants;

/**
 * Analyzes data from all subfolders of one folder. It is assumed that the typical PeASS-folder-structure is given.
 * 
 * @author reichelt
 *
 */
public class DeviationAnalyser extends DataAnalyser {
	
	private static final NumberFormat format = NumberFormat.getInstance();
	
	private static final Logger LOG = LogManager.getLogger(DeviationAnalyser.class);

	public static final Option DATAOPTION = Option.builder(CompareByFulldata.DATA).required(true).hasArgs()
			.desc("Daten der zu analysierenden Ergebnisdaten bzw. Ergebnisdateien-Ordner").build();

	public static void main(final String[] args) throws ParseException, JAXBException, InterruptedException {
		final Options options = OptionConstants.createOptions(OptionConstants.DEPENDENCYFILE);
		options.addOption(DATAOPTION);

		final CommandLineParser parser = new DefaultParser();
		final CommandLine line = parser.parse(options, args);

		AnalyseOneTest.loadDependencies(line);

		for (int i = 0; i < line.getOptionValues(CompareByFulldata.DATA).length; i++) {
			final File folder = new File(line.getOptionValues(CompareByFulldata.DATA)[i]);
			LOG.info("Searching in " + folder);
			processFolder(folder);
		}
	}

	private static DeviationAnalyser deviationAnalyser;

	static {
		try {
			deviationAnalyser = new DeviationAnalyser();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private final BufferedWriter writer;

	public DeviationAnalyser() throws IOException {
		final File goal = new File("deviations.csv");
		writer = new BufferedWriter(new FileWriter(goal));
	}

	/**
	 * Process a found folder, i.e. a folder containing measurements.
	 * 
	 * @param folder Folder to process
	 */
	private static void processFolder(final File folder) {
		for (final File file : folder.listFiles()) {
			if (file.isDirectory()) {
				if (file.getName().equals("measurements")) {
					LOG.info("Analysing: {}", file.getAbsolutePath());
					try {
						deviationAnalyser.analyseFolder(file);
					} catch (final InterruptedException e) {
						e.printStackTrace();
					} catch (final RuntimeException e) {
						e.printStackTrace();
					}
				} else {
					processFolder(file);
				}
			}
		}
	}

	@Override
	public void processTestdata(TestData measurementEntry) {
		try {
			for (final Map.Entry<String, EvaluationPair> entry : measurementEntry.getMeasurements().entrySet()) {
				final DescriptiveStatistics statistics1 = ConfidenceIntervalInterpretion.getStatistics(entry.getValue().getPrevius());
				writer.write(format.format(statistics1.getMean()) + ";" + format.format(statistics1.getStandardDeviation()) + ";" + format.format(statistics1.getStandardDeviation() / statistics1.getMean()));
				writer.write("\n");
				final DescriptiveStatistics statistics2 = ConfidenceIntervalInterpretion.getStatistics(entry.getValue().getCurrent());
				writer.write(format.format(statistics2.getMean()) + ";" + format.format(statistics2.getStandardDeviation()) + ";" + format.format(statistics2.getStandardDeviation() / statistics2.getMean()));
				writer.write("\n");
			}
			writer.flush();
		} catch (final IOException e) {
			e.printStackTrace();
		}

	}
}
