package de.dagere.peass.dependency.kiekerTemp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import kieker.analysis.source.ISourceCompositeStage;
import kieker.analysis.source.file.DirectoryScannerStage;
import kieker.common.configuration.Configuration;
import kieker.common.record.IMonitoringRecord;
import teetime.framework.CompositeStage;
import teetime.framework.OutputPort;

/**
 * Copy of Kiekers stage; will be removed as soon as Kieker publishes a bugfix version
 */
public class LogsReaderCompositeStage extends CompositeStage implements ISourceCompositeStage {

   public static final String PREFIX = LogsReaderCompositeStage.class.getCanonicalName() + ".";
   public static final String LOG_DIRECTORIES = PREFIX + "logDirectories";
   public static final String DATA_BUFFER_SIZE = PREFIX + "bufferSize";
   public static final String VERBOSE = PREFIX + "verbose";

   private static final int DEFAULT_BUFFER_SIZE = 8192;

   private final DirectoryScannerStage directoryScannerStage; // NOPMD this stays here for documentation purposes
   private final DirectoryReaderStage directoryReaderStage;

   /**
    * Creates a composite stage to scan and read a set of Kieker log directories.
    *
    * @param configuration
    *            configuration for the enclosed filters
    */
   public LogsReaderCompositeStage(final Configuration configuration) {
      final String[] directoryNames = configuration.getStringArrayProperty(LOG_DIRECTORIES, ":");
      final List<File> directories = new ArrayList<>(directoryNames.length);

      for (final String name : directoryNames) {
         directories.add(new File(name));
      }

      final int dataBufferSize = configuration.getIntProperty(DATA_BUFFER_SIZE, DEFAULT_BUFFER_SIZE);
      final boolean verbose = configuration.getBooleanProperty(VERBOSE, false);

      this.directoryScannerStage = new DirectoryScannerStage(directories);
      this.directoryReaderStage = new DirectoryReaderStage(verbose, dataBufferSize);

      this.connectPorts(this.directoryScannerStage.getOutputPort(), this.directoryReaderStage.getInputPort());
   }

   /**
    * Creates a composite stage to scan and read a set of Kieker log directories.
    *
    * @param directories
    *            list of directories to read
    * @param verbose
    *            report on every read log file
    * @param dataBufferSize
    *            buffer size of the data file reader (null == use default setting)
    */
   public LogsReaderCompositeStage(final List<File> directories, final boolean verbose, final Integer dataBufferSize) {
      this.directoryScannerStage = new DirectoryScannerStage(directories);
      this.directoryReaderStage = new DirectoryReaderStage(verbose, dataBufferSize == null ? DEFAULT_BUFFER_SIZE : dataBufferSize); // NOCS inline conditional

      this.connectPorts(this.directoryScannerStage.getOutputPort(), this.directoryReaderStage.getInputPort());
   }

   @Override
   public OutputPort<IMonitoringRecord> getOutputPort() {
      return this.directoryReaderStage.getOutputPort();
   }

}
