package de.peran.measurement.analysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result;
import de.peran.DependencyStatisticAnalyzer;
import de.peran.dependencyprocessors.VersionComparator;
import de.peran.generated.Versiondependencies;
import de.peran.measurement.analysis.statistics.TestData;
import de.peran.utils.OptionConstants;

/**
 * Compares the fulldata, i.e. the data with all its measurement-executions, using statistical methods. The fulldata-flags in the data need to be present.
 * 
 * @author dagere
 *
 */
public class CompareByFulldata {
	public static final String DATA = "data";
	
	private static final Logger LOG = LogManager.getLogger(CompareByFulldata.class);

	public static void main(final String[] args) throws JAXBException, IOException, ParseException {
		final Options options = OptionConstants.createOptions(OptionConstants.DEPENDENCYFILE);
		final Option dataOption = new Option(CompareByFulldata.DATA, "Data of measurements");
		dataOption.setRequired(true);
		options.addOption(dataOption);

		final CommandLineParser parser = new DefaultParser();
		final CommandLine line = parser.parse(options, args);
		
		if (line.hasOption(OptionConstants.DEPENDENCYFILE.getName())) {
			final File dependencyFile = new File(line.getOptionValue(OptionConstants.DEPENDENCYFILE.getName()));
			Versiondependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
			VersionComparator.setDependencies(dependencies);
		} else {
			LOG.error("No dependencyfile information passed.");
		}

		final File[] files = new File[line.getOptionValues(DATA).length];
		for (int i = 0; i < line.getOptionValues(DATA).length; i++) {
			files[i] = new File(line.getOptionValues(DATA)[i]);
		}
		File fullDataFolder = files[0];
		final Map<String, TestData> data = DataReader.readVersionDataMap(fullDataFolder);
		
		new CompareByFulldata(fullDataFolder).analyzeData(data);
	}

	private final File makeFile;
	private final File plotterFile;
	
	private FileWriter lookAtAllChangesWriter;

	private final File instructions;

	public CompareByFulldata(final File fullDataFolder) {
		makeFile = new File(fullDataFolder, "make.sh");
		plotterFile = new File(fullDataFolder, "plot.plt");

		final File changes = new File(fullDataFolder, "changes");
		final File nochanges = new File(fullDataFolder, "nochanges");
		if (!changes.exists()) {
			changes.mkdir();
		}
		if (!nochanges.exists()) {
			nochanges.mkdir();
		}

		instructions = new File(fullDataFolder, "instructions");
		if (!instructions.exists()) {
			instructions.mkdir();
		}
		
		final File lookAtChangesFile = new File(instructions, "lookAtAllChanges.sh");
		try {
			lookAtAllChangesWriter = new FileWriter(lookAtChangesFile);
			lookAtAllChangesWriter.write("# Calls all change-files");
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		lookAtChangesFile.setExecutable(true);
	}

	public void analyzeData(final Map<String, TestData> data) {
		int changed = 0;
		int unchanged = 0;

		try (FileWriter fw = new FileWriter(makeFile); FileWriter plotWriter = new FileWriter(plotterFile)) {
			plotWriter.write("binwidth=5;");
			plotWriter.write("bin(x,width)=width*floor(x/width)\n");

			int term = 1;
			for (final Map.Entry<String, TestData> entry : data.entrySet()) {
				// System.out.println(entry.getKey());
				final TestData measurementSet = entry.getValue();
				List<Result> previousData = null;
				String previousVersion = null;
				LOG.debug("Prüfe: " + entry.getKey());
			}

		} catch (final Throwable e) {
			e.printStackTrace();
		}

		System.out.println("Verändert: " + changed + " Unverändert: " + unchanged);
	}




}
