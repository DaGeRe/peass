package de.peass;

import java.io.File;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.analysis.changes.ChangeReader;
import de.peass.dependency.reader.DependencyReaderUtil;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.OptionConstants;

public class GetChanges {

	private static final Logger LOG = LogManager.getLogger(GetChanges.class);

	public static void main(final String[] args) throws JAXBException, ParseException {
		final Options options = OptionConstants.createOptions(OptionConstants.OUT, OptionConstants.URL, OptionConstants.DATA, OptionConstants.DEPENDENCYFILE, OptionConstants.EXECUTIONFILE, OptionConstants.CONFIDENCE);

		final CommandLineParser parser = new DefaultParser();
		final CommandLine line = parser.parse(options, args);

		if (!line.hasOption(OptionConstants.URL.getName()) && !line.hasOption(OptionConstants.DEPENDENCYFILE.getName())) {
			LOG.error(
					"You should pass either an URL or an dependencyfile, since the cleaner needs to know the commits order. If the project is contained in the default URLs, it will also work.");
		}

		if (line.hasOption(OptionConstants.DEPENDENCYFILE.getName())) {
			DependencyReaderUtil.loadDependencies(line);
		}

		final File resultFolder = new File(line.getOptionValue(OptionConstants.OUT.getName(), "results"));
		if (!resultFolder.exists()) {
			resultFolder.mkdirs();
		}
		final File statisticFolder = new File(resultFolder, "statistics");
		if (!statisticFolder.exists()) {
			statisticFolder.mkdir();
		}

		final ChangeReader reader = new ChangeReader(statisticFolder, VersionComparator.getProjectName());
		if (line.hasOption(OptionConstants.CONFIDENCE.getName())) {
		   final String confidenceValue = line.getOptionValue(OptionConstants.CONFIDENCE.getName());
         reader.setConfidence(Double.parseDouble(confidenceValue));
		}

		final File measurementFolder = new File(line.getOptionValue(OptionConstants.DATA.getName()));
		reader.readFile(measurementFolder);
	}
}
