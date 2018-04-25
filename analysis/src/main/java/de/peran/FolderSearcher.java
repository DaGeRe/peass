package de.peran;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peran.analysis.knowledge.Change;
import de.peran.analysis.knowledge.Changes;
import de.peran.measurement.analysis.AnalyseFullData;
import de.peran.measurement.analysis.CompareByFulldata;
import de.peran.utils.OptionConstants;

/**
 * Analyzes data from all subfolders of one folder. It is assumed that the typical PeASS-folder-structure is given.
 * @author reichelt
 *
 */
public class FolderSearcher {
	private static final Logger LOG = LogManager.getLogger(FolderSearcher.class);

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
		LOG.info("Versions: {} Testcases: {} Changes: {}", AnalyseFullData.versions.size(), AnalyseFullData.testcases,
				AnalyseFullData.changes);

		for (final Entry<String, Changes> entry : AnalyseFullData.oldKnowledge.getVersionChanges().entrySet()) {
			final Changes newChanges = AnalyseFullData.knowledge.getVersion(entry.getKey());
			if (newChanges == null) {
			} else {
				for (final Entry<String, List<Change>> changeTests : entry.getValue().getTestcaseChanges().entrySet()) {
					final List<Change> clazzChanges = newChanges.getTestcaseChanges().get(changeTests.getKey());
					if (clazzChanges == null) {
						LOG.debug("Test not found: {}", changeTests.getKey());
					} else {
						for (final Change change : changeTests.getValue()) {
							boolean found = false;
							for (final Change newChange : clazzChanges) {
								if (newChange.getDiff().equals(change.getDiff())) {
									found = true;
								}
							}

							if (!found) {
								LOG.debug("Entry not found: {}", change.getDiff());
							}else{
								LOG.debug("Entry found: {}", change.getDiff());
							}
						}
					}
				}
			}
		}

	}

	/**
	 * Process a found folder, i.e. a folder containing measurements.
	 * @param folder Folder to process
	 */
	private static void processFolder(final File folder) {
		for (final File measurementFolder : folder.listFiles()) {
			if (measurementFolder.isDirectory()) {
				if (measurementFolder.getName().equals("measurements")) {
					LOG.info("Analysing: {}", measurementFolder.getAbsolutePath());
					try {
						new AnalyseFullData().analyseFolder(measurementFolder);
					} catch (final InterruptedException e) {
						e.printStackTrace();
					} catch (final RuntimeException e) {
						e.printStackTrace();
						// Show exception, but continue - exception may be
						// caused by long-running testcases..
					}
				} else {
					processFolder(measurementFolder);
				}
			}
		}
	}
}
