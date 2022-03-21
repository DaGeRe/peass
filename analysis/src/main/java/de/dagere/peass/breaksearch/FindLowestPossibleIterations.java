package de.dagere.peass.breaksearch;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.breaksearch.minimalvalues.MinimalVMDeterminer;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.measurement.dataloading.DataReader;
import de.dagere.peass.measurement.dataloading.MultipleVMTestUtil;
import de.dagere.peass.measurement.statistics.data.TestData;
import de.dagere.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class FindLowestPossibleIterations implements Callable<Void> {
	private static final Logger LOG = LogManager.getLogger(FindLowestPossibleIterations.class);

	@Option(names = { "-dependencyFile", "--dependencyFile" }, description = "Internal only")
   private File dependencyFile;
   
   @Option(names = { "-data", "--data" }, description = "Internal only")
   private File[] data;
	
	public static void main(final String[] args) throws InterruptedException,  JAXBException, JsonParseException, JsonMappingException, IOException {
	   try {
         final CommandLine commandLine = new CommandLine(new FindLowestPossibleIterations());
         commandLine.execute(args);
      } catch (final Throwable t) {
         t.printStackTrace();
      }
	}

   @Override
   public Void call() throws InterruptedException, StreamReadException, DatabindException, IOException {
      final StaticTestSelection dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, StaticTestSelection.class);
      VersionComparator.setDependencies(dependencies);
      
//      for (File folder : data) {
//			final File measurementFolder = new File(folder, "measurements");
//			if (measurementFolder.exists()) {
//				files[i] = measurementFolder;
//			} else {
//				files[i] = folder;
//			}
//		}

      for (File fullDataFolder : data) {
			LOG.info("Loading: {}", fullDataFolder);

			if (!fullDataFolder.exists()) {
				LOG.error("Ordner existiert nicht!");
				System.exit(1);
			}

			final LinkedBlockingQueue<TestData> measurements = new LinkedBlockingQueue<>();
			DataReader.startReadVersionDataMap(fullDataFolder, measurements);

			TestData measurementEntry = measurements.take();

			while (measurementEntry != DataReader.POISON_PILL) {
				processTestdata(measurementEntry);
				measurementEntry = measurements.take();
			}
		}

		LOG.debug("Final minimal VM executions for same result: " + vmDeterminer.getMinNeccessaryValue() + " Average: " + ((double) vmDeterminer.getSum() / vmDeterminer.getCount()));
		LOG.debug(vmDeterminer.getValues());
      return null;

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
