package de.peran.measurement.analysis;

import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependencyprocessors.VersionComparator;
import de.peran.measurement.analysis.statistics.TestData;

/**
 * Reads measurement data sequentially, reading files after each other to a queue and stops reading if the queue gets too full (currently 10 elements).
 * 
 * @author reichelt
 *
 */
public final class DataReader {
	private static final int MAX_QUEUE_SIZE = 10;

	private static final Logger LOG = LogManager.getLogger(DataReader.class);

	public static final TestData POISON_PILL = new TestData(null);
	private static int size = 0;

	private DataReader() {

	}

	public static LinkedBlockingQueue<TestData> startReadVersionDataMap(final File fullDataFolder) {
		final LinkedBlockingQueue<TestData> myQueue = new LinkedBlockingQueue<>();
		final Thread readerThread = new Thread(new Runnable() {

			@Override
			public void run() {
				size = 0;
				LOG.debug("Starting data-reading");
				readDataToQueue(fullDataFolder, myQueue);
				myQueue.add(POISON_PILL);
				LOG.debug("Finished data-reading, testcase-changes: {}", size);
			}
		});
		readerThread.start();

		return myQueue;
	}

	private static void readDataToQueue(final File fullDataFolder, final LinkedBlockingQueue<TestData> measurements) {
		LOG.trace("Loading folder: {}", fullDataFolder);
		final Set<String> xmlPrefixes = new HashSet<>();
		for (final File file : fullDataFolder.listFiles((FileFilter) new WildcardFileFilter("*"))) {
			if (file.isDirectory()) {
				readDataToQueue(file, measurements);
			} else {
				if (file.getName().contains(".xml")) {
					// LOG.info("Loading: " + file.getAbsolutePath());
					final String prefix = file.getName().substring(0, file.getName().indexOf("_"));
					xmlPrefixes.add(prefix);
				}
			}
		}

		for (final String xmlPrefix : xmlPrefixes) {
			final List<String> versions = getAllMeasuredVersions(fullDataFolder, xmlPrefix);

			Collections.sort(versions, VersionComparator.INSTANCE);

			LOG.trace("Prefix: " + xmlPrefix);
			final Map<String, TestData> currentMeasurement = new HashMap<>();
			for (final File file : fullDataFolder.listFiles((FileFilter) new WildcardFileFilter(xmlPrefix + "_*"))) {
				LOG.debug("Loading: {}", file);
				if (!file.isDirectory()) {
					if (file.getName().endsWith(".xml")) {
						final String name = file.getName().substring(0, file.getName().lastIndexOf("."));
						final String[] parts = name.split("_");
						LOG.trace(name);
						boolean isNew = parts[parts.length - 1].equals("new");
						final String version = parts[parts.length - 1].equals("new") ? parts[parts.length - 2] : parts[parts.length - 1];
						LOG.trace("{}: {}", version, file);
						if (VersionComparator.getVersionIndex(version) == 0 || versions.get(0).equals(version)) {
							isNew = true; // 0th version (or 0th version of current folder) is always counted as new measurement
						}

						try {
							final Kopemedata resultData = new XMLDataLoader(file).getFullData();

							final String testclazz = resultData.getTestcases().getClazz();
							final String testmethod = resultData.getTestcases().getTestcase().get(0).getName();

							TestData testData = currentMeasurement.get(testmethod);
							if (testData == null) {
								testData = new TestData(new TestCase(testclazz, testmethod));
								currentMeasurement.put(testmethod, testData);
							}

							LOG.debug("Adding: " + isNew + " " + version + " " + testmethod);
							testData.addMeasurement(version, resultData, isNew);
						} catch (final JAXBException e) {
							e.printStackTrace();
						}

					}
				}
			}
			for (final TestData data : currentMeasurement.values()) {
				LOG.trace("Add: " + data.getTestClass() + " " + data.getTestMethod());
				while (measurements.size() > MAX_QUEUE_SIZE) {
					try {
						Thread.sleep(1000);
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}
				}
				measurements.add(data);
				size += data.getVersions();
			}
		}

	}

	private static List<String> getAllMeasuredVersions(final File fullDataFolder, final String xmlPrefix) {
		final List<String> versions = new LinkedList<>();
		for (final File file : fullDataFolder.listFiles((FileFilter) new WildcardFileFilter(xmlPrefix + "*"))) {
			final String name = file.getName().substring(0, file.getName().lastIndexOf("."));
			final String[] parts = name.split("_");
			final String version = parts[parts.length - 1].equals("new") ? parts[parts.length - 2] : parts[parts.length - 1];
			if (!versions.contains(version)) {
				versions.add(version);
			}
		}
		return versions;
	}

	/**
	 * Creates a map from the version to the measurement-data of the version
	 * 
	 * @param fullDataFolder
	 * @return
	 * @throws JAXBException
	 */
	public static Map<String, TestData> readVersionDataMap(final File fullDataFolder) throws JAXBException {
		final Map<String, TestData> measurements = new HashMap<>();
		final Set<String> versions = new TreeSet<>();
		LOG.info("Lade Daten..");
		readData(fullDataFolder, measurements, versions);
		LOG.info("Daten geladen");
		return measurements;
	}

	private static void readData(final File fullDataFolder, final Map<String, TestData> measurements, final Set<String> versions) throws JAXBException {
		LOG.debug("Lade Ordner: {}", fullDataFolder);
		for (final File file : fullDataFolder.listFiles((FileFilter) new WildcardFileFilter("*"))) {
			LOG.debug("Lade: {}", file);
			if (file.isDirectory()) {
				readData(file, measurements, versions);
			} else {
				if (file.getName().endsWith(".xml")) {
					final String name = file.getName().substring(0, file.getName().lastIndexOf("."));
					final String[] parts = name.split("_");
					LOG.trace(name);
					final boolean isNew = parts[parts.length - 1].equals("new");
					final String version = parts[parts.length - 1].equals("new") ? parts[parts.length - 2] : parts[parts.length - 1];
					versions.add(version);
					LOG.debug("{}: {}", version, file);

					final Kopemedata resultData = new XMLDataLoader(file).getFullData();

					final String testclazz = resultData.getTestcases().getClazz();
					final String testmethod = resultData.getTestcases().getTestcase().get(0).getName();

					final TestCase testcase = new TestCase(testclazz, testmethod);

					TestData testData = measurements.get(testmethod);
					if (testData == null) {
						testData = new TestData(testcase);
						measurements.put(testmethod, testData);
					}

					testData.addMeasurement(version, resultData, isNew);
				}
			}

		}
	}
}
