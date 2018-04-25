package de.peran;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result;
import de.peran.dependencyprocessors.VersionComparator;
import de.peran.generated.Versiondependencies;
import de.peran.measurement.analysis.AnalyseFullData;
import de.peran.measurement.analysis.CompareByFulldata;
import de.peran.measurement.analysis.DataReader;
import de.peran.measurement.analysis.statistics.EvaluationPair;
import de.peran.measurement.analysis.statistics.MeanCoVData;
import de.peran.measurement.analysis.statistics.TestData;
import de.peran.statistics.DependencyStatisticAnalyzer;
import de.peran.utils.OptionConstants;

public class AnalyseOneTest {

	private static final Logger LOG = LogManager.getLogger(AnalyseFullData.class);

	public final static File RESULTFOLDER = new File("results");
	public final static File DIFFFOLDER = new File(RESULTFOLDER, "diff");

	static {
		DIFFFOLDER.mkdir();
		DIFFFOLDER.mkdirs();
	}

	public static void main(final String[] args) throws InterruptedException, IOException, ParseException, JAXBException {
		final Options options = OptionConstants.createOptions(OptionConstants.DEPENDENCYFILE);
		options.addOption(FolderSearcher.DATAOPTION);

		final CommandLineParser parser = new DefaultParser();
		final CommandLine line = parser.parse(options, args);

		loadDependencies(line);

		final File fullDataFolder = new File(line.getOptionValues(CompareByFulldata.DATA)[0]);

		final LinkedBlockingQueue<TestData> measurements = DataReader.startReadVersionDataMap(fullDataFolder);

		TestData measurementEntry = measurements.take();
		while (measurementEntry != DataReader.POISON_PILL) {
			processTestdata(measurementEntry);
			measurementEntry = measurements.take();
		}
	}

	public static void loadDependencies(final CommandLine line) throws JAXBException {
		if (line.hasOption(OptionConstants.DEPENDENCYFILE.getName())) {
			final File dependencyFile = new File(line.getOptionValue(OptionConstants.DEPENDENCYFILE.getName()));
			final Versiondependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
			VersionComparator.setDependencies(dependencies);
		} else {
			LOG.error("No dependencyfile information passed.");
		}
	}

	private static void processTestdata(final TestData measurementEntry) throws IOException {
		LOG.debug("Reading: {}#{}", measurementEntry.getTestClass(), measurementEntry.getTestMethod());
		for (final Map.Entry<String, EvaluationPair> entry : measurementEntry.getMeasurements().entrySet()) {
			final File resultFile = new File(DIFFFOLDER, measurementEntry.getTestClass() + "_" + measurementEntry.getTestMethod() + "_average.csv");
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(resultFile))) {
				final Iterator<Result> iterator = entry.getValue().getPrevius().iterator();
				for (final Result result : entry.getValue().getCurrent()) {
					final Result previus = iterator.next();
					bw.write(result.getValue() + ";" + previus.getValue() + "\n");
				}
				bw.flush();
			}

			final MeanCoVData current = new MeanCoVData(measurementEntry.getTestMethod(), entry.getValue().getCurrent());
			current.printAverages(DIFFFOLDER, measurementEntry.getTestClass());

			final MeanCoVData prev = new MeanCoVData(measurementEntry.getTestMethod(), entry.getValue().getPrevius());
			prev.printAverages(DIFFFOLDER, measurementEntry.getTestClass() + "_prev");
		}
	}
}
