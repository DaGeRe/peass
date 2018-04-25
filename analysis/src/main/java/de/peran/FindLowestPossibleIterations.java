package de.peran;

import java.io.File;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peran.analysis.helper.MinimalVMDeterminer;
import de.peran.dependencyprocessors.VersionComparator;
import de.peran.generated.Versiondependencies;
import de.peran.measurement.analysis.CompareByFulldata;
import de.peran.measurement.analysis.DataReader;
import de.peran.measurement.analysis.MultipleVMTestUtil;
import de.peran.measurement.analysis.statistics.TestData;
import de.peran.statistics.DependencyStatisticAnalyzer;
import de.peran.utils.OptionConstants;

public class FindLowestPossibleIterations {
	private static final Logger LOG = LogManager.getLogger(FindLowestPossibleIterations.class);

	public static void main(final String[] args) throws InterruptedException, ParseException, JAXBException {
		final Options options = OptionConstants.createOptions(OptionConstants.DEPENDENCYFILE);

		final Option dataOption = new Option(CompareByFulldata.DATA, "Data of measurements");
		dataOption.setRequired(true);
		options.addOption(dataOption);

		final CommandLineParser parser = new DefaultParser();
		final CommandLine line = parser.parse(options, args);

		if (line.hasOption(OptionConstants.DEPENDENCYFILE.getName())) {
			final File dependencyFile = new File(line.getOptionValue(OptionConstants.DEPENDENCYFILE.getName()));
			final Versiondependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
			VersionComparator.setDependencies(dependencies);
		} else {
			LOG.error("No dependencyfile information passed.");
		}

		final File[] files = new File[line.getOptionValues(CompareByFulldata.DATA).length];
		for (int i = 0; i < line.getOptionValues(CompareByFulldata.DATA).length; i++) {
			final File folder = new File(line.getOptionValues(CompareByFulldata.DATA)[i]);
			final File measurementFolder = new File(folder, "measurements");
			if (measurementFolder.exists()) {
				files[i] = measurementFolder;
			} else {
				files[i] = folder;
			}
		}

		for (int i = 0; i < files.length; i++) {
			final File fullDataFolder = files[i];
			LOG.info("Loading: {}", fullDataFolder);

			if (!fullDataFolder.exists()) {
				LOG.error("Ordner existiert nicht!");
				System.exit(1);
			}

			final LinkedBlockingQueue<TestData> measurements = DataReader.startReadVersionDataMap(fullDataFolder);

			TestData measurementEntry = measurements.take();

			while (measurementEntry != DataReader.POISON_PILL) {
				processTestdata(measurementEntry);
				measurementEntry = measurements.take();
			}
		}

		LOG.debug("Final minimal VM executions for same result: " + vmDeterminer.getMinNeccessaryValue() + " Average: " + ((double) vmDeterminer.getSum() / vmDeterminer.getCount()));
		LOG.debug(vmDeterminer.getValues());

		// LOG.debug("Final minimal measurement executions for same result: " + exDeterminer.minNeccessaryValue + " Average: " + ((double) exDeterminer.sum / exDeterminer.count));
		// LOG.debug(exDeterminer.values);
	}

	public static int fileindex = 0;

	static MinimalVMDeterminer vmDeterminer = new MinimalVMDeterminer();
	// static MinimalExecutionDeterminer exDeterminer = new MinimalExecutionDeterminer();

	private static void processTestdata(final TestData measurementEntry) {
		vmDeterminer.processTestdata(measurementEntry);
		// exDeterminer.processTestdata(measurementEntry);
	}

	public static boolean isStillSignificant(final List<Double> before, final List<Double> after, final int oldResult) {
		final int result = MultipleVMTestUtil.compareDouble(before, after);
		if (result == oldResult) {
			return true;
		} else {
			return false;
		}
	}
}
