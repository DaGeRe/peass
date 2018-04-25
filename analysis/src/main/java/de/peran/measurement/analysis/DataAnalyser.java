package de.peran.measurement.analysis;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peran.measurement.analysis.statistics.TestData;

public abstract class DataAnalyser {
	private static final Logger LOG = LogManager.getLogger(AnalyseFullData.class);
	
	public void analyseFolder(final File fullDataFolder) throws InterruptedException {
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

	public abstract void processTestdata(TestData measurementEntry);
}
