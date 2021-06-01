package de.peran;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.measurement.analysis.AnalyseFullData;

public class AnalyseOneTest {

	private static final Logger LOG = LogManager.getLogger(AnalyseFullData.class);

//	public static File RESULTFOLDER;
//	public static File DIFFFOLDER;

//	static {
//		setResultFolder(new File("results"));
//	}
	
//	public static void setResultFolder(final File folder){
//		RESULTFOLDER = folder;
//		DIFFFOLDER = new File(RESULTFOLDER, "diff");
//		DIFFFOLDER.mkdirs();
//	}

//	public static void main(final String[] args) throws InterruptedException, IOException, ParseException, JAXBException {
//		final Options options = OptionConstants.createOptions(OptionConstants.DEPENDENCYFILE);
//		options.addOption(FolderSearcher.DATAOPTION);
//
//		final CommandLineParser parser = new DefaultParser();
//		final CommandLine line = parser.parse(options, args);
//
//		Cleaner.loadDependencies(line);
//
//		final File fullDataFolder = new File(line.getOptionValues(FolderSearcher.DATA)[0]);
//
//		final LinkedBlockingQueue<TestData> measurements = DataReader.startReadVersionDataMap(fullDataFolder);
//
//		TestData measurementEntry = measurements.take();
//		while (measurementEntry != DataReader.POISON_PILL) {
//			processTestdata(measurementEntry);
//			measurementEntry = measurements.take();
//		}
//	}

//	private static void processTestdata(final TestData measurementEntry) throws IOException {
//		LOG.debug("Reading: {}#{}", measurementEntry.getTestClass(), measurementEntry.getTestMethod());
//		for (final Map.Entry<String, EvaluationPair> entry : measurementEntry.getMeasurements().entrySet()) {
//			final File resultFile = new File(DIFFFOLDER, measurementEntry.getTestClass() + "_" + measurementEntry.getTestMethod() + "_average.csv");
//			try (BufferedWriter bw = new BufferedWriter(new FileWriter(resultFile))) {
//				final Iterator<Result> iterator = entry.getValue().getPrevius().iterator();
//				for (final Result result : entry.getValue().getCurrent()) {
//					final Result previus = iterator.next();
//					bw.write(result.getValue() + ";" + previus.getValue() + "\n");
//				}
//				bw.flush();
//			}
//
//			final MeanCoVData current = new MeanCoVData(measurementEntry.getTestMethod(), entry.getValue().getCurrent());
//			current.printAverages(DIFFFOLDER, measurementEntry.getTestClass());
//
//			final MeanCoVData prev = new MeanCoVData(measurementEntry.getTestMethod(), entry.getValue().getPrevius());
//			prev.printAverages(DIFFFOLDER, measurementEntry.getTestClass() + "_prev");
//		}
//	}
}
