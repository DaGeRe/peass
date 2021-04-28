package de.peran;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.measurement.analysis.Cleaner;
import de.dagere.peass.measurement.analysis.DataAnalyser;
import de.dagere.peass.measurement.analysis.statistics.EvaluationPair;
import de.dagere.peass.measurement.analysis.statistics.TestData;
import de.dagere.peass.statistics.ConfidenceIntervalInterpretion;
import de.dagere.peass.utils.OptionConstants;
import de.peass.measurement.analysis.statistics.MeanCoVData;

/**
 * Analyzes data from all subfolders of one folder. It is assumed that the typical PeASS-folder-structure is given.
 * 
 * @author reichelt
 *
 */
public class DeviationAnalyser extends DataAnalyser {
	
	private static final Logger LOG = LogManager.getLogger(DeviationAnalyser.class);

	public static void main(final String[] args) throws ParseException, JAXBException, InterruptedException, JsonParseException, JsonMappingException, IOException {
		final Options options = OptionConstants.createOptions(OptionConstants.DEPENDENCYFILE);
		options.addOption(FolderSearcher.DATAOPTION);

		final CommandLineParser parser = new DefaultParser();
		final CommandLine line = parser.parse(options, args);

		Cleaner.loadDependencies(line);

		for (int i = 0; i < line.getOptionValues(FolderSearcher.DATA).length; i++) {
			final File folder = new File(line.getOptionValues(FolderSearcher.DATA)[i]);
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
	public void processTestdata(final TestData measurementEntry) {
		try {
			for (final Map.Entry<String, EvaluationPair> entry : measurementEntry.getMeasurements().entrySet()) {
				final DescriptiveStatistics statistics1 = ConfidenceIntervalInterpretion.getStatistics(entry.getValue().getPrevius());
				writer.write(MeanCoVData.FORMAT.format(statistics1.getMean()) + ";" + MeanCoVData.FORMAT.format(statistics1.getStandardDeviation()) + ";" + MeanCoVData.FORMAT.format(statistics1.getStandardDeviation() / statistics1.getMean()));
				writer.write("\n");
				final DescriptiveStatistics statistics2 = ConfidenceIntervalInterpretion.getStatistics(entry.getValue().getCurrent());
				writer.write(MeanCoVData.FORMAT.format(statistics2.getMean()) + ";" + MeanCoVData.FORMAT.format(statistics2.getStandardDeviation()) + ";" + MeanCoVData.FORMAT.format(statistics2.getStandardDeviation() / statistics2.getMean()));
				writer.write("\n");
			}
			writer.flush();
		} catch (final IOException e) {
			e.printStackTrace();
		}

	}
}
