package de.peran.measurement.analysis;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.peran.DependencyStatisticAnalyzer;
import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependencyprocessors.PairProcessor;
import de.peran.dependencyprocessors.VersionComparator;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.measurement.analysis.statistics.TestData;
import de.peran.utils.OptionConstants;

public class VersionDataAnalyzer extends PairProcessor {

	private static final Logger LOG = LogManager.getLogger(VersionDataAnalyzer.class);

	public VersionDataAnalyzer(final String[] args) throws ParseException, JAXBException {
		super(args, false);
	} 

	@Override
	protected void processVersion(final Version versioninfo) {
		LOG.info("Analysiere: {}", versioninfo.getVersion());
		final Set<TestCase> testcases = findTestcases(versioninfo);

		for (final TestCase testcase : testcases) {
			if (lastTestcaseCalls.containsKey(testcase)) {
				final String versionOld = lastTestcaseCalls.get(testcase);
				executeCompareTests(versioninfo.getVersion(), versionOld, testcase);
			}
			lastTestcaseCalls.put(testcase, versioninfo.getVersion());
		}
	}

	private void executeCompareTests(final String revision, final String versionOld, final TestCase testcase) {
		LOG.trace("Lade: {} {}", revision, versionOld);
		final List<MeasurementFile> currentFiles = fileData.get(revision);
		final List<MeasurementFile> oldFiles = fileData.get(versionOld);

		if (currentFiles == null) {
			return;
		}

		final Map<String, TestData> myData = new HashMap<>();
		final TestData myTestdata = new TestData(new TestCase(testcase.getClazz(), testcase.getMethod()));
		addData(revision, testcase, currentFiles, myTestdata);
		addData(versionOld, testcase, oldFiles, myTestdata);
		myData.put(revision, myTestdata);

		final CompareByFulldata comperator = new CompareByFulldata(projectFolder);
		comperator.analyzeData(myData);
	}

	private TestData addData(final String revision, final TestCase testcase, final List<MeasurementFile> currentFiles, final TestData pairTestData) {
		for (final MeasurementFile file : currentFiles) {
			if (file.getClazz().equals(testcase.getClazz()) && file.getMethod().equals(testcase.getMethod())
					&& file.getVersion().equals(revision)) {
				try {
					final Kopemedata resultData = new XMLDataLoader(file.getFile()).getFullData();
					
//					pairTestData.addMeasurement(revision, resultData);
				} catch (final JAXBException e) {
					e.printStackTrace();
				}
			}
		}
		return pairTestData;
	}

	static Map<String, List<MeasurementFile>> fileData;

	public static void main(final String[] args) throws ParseException, JAXBException {
		final VersionDataAnalyzer analyzer = new VersionDataAnalyzer(args);

		final File fullDataFolder = new File(analyzer.getLine().getOptionValue(OptionConstants.FOLDER.getName()));
		fileData = new TreeMap<>();
		readData(fullDataFolder, fileData);

		final File dependencyFile = new File(analyzer.getLine().getOptionValue(OptionConstants.DEPENDENCYFILE.getName()));
		final Versiondependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
		VersionComparator.setDependencies(dependencies);

		analyzer.processCommandline();
	}

	private static void readData(final File fullDataFolder, final Map<String, List<MeasurementFile>> data) throws JAXBException {
		for (final File file : fullDataFolder.listFiles((FileFilter) new WildcardFileFilter("*"))) {
			LOG.trace("Lade: {}", file);
			if (file.isDirectory()) {
				readData(file, data);
			} else {
				if (file.getName().endsWith(".xml")) {
					final String name = file.getName().substring(0, file.getName().lastIndexOf("."));
					final String parts[] = name.split("_");
					// final String test = parts[0];
					LOG.trace(name);
					if (parts[parts.length - 1].equals("new")) {
						continue; // Workaround, bei 2 Messausführungen, um gleiche Datenlängen zu erreichen
					}
					final String version = parts[parts.length - 1].equals("new") ? parts[parts.length - 2] : parts[parts.length - 1];
					LOG.trace("{}: {}", version, file);

					final Kopemedata resultData = new XMLDataLoader(file).getFullData();

					final String testclazz = resultData.getTestcases().getClazz();
					final String testmethod = resultData.getTestcases().getTestcase().get(0).getName();

					final MeasurementFile info = new MeasurementFile(file, version, testclazz, testmethod);

					List<MeasurementFile> files = data.get(version);
					if (files == null) {
						LOG.info("Neue Version: {}", version);
						files = new LinkedList<>();
						data.put(version, files);
					}
					files.add(info);
				}
			}
		}
	}
}
