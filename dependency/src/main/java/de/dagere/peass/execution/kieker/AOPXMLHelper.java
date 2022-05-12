package de.dagere.peass.execution.kieker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import de.dagere.kopeme.kieker.writer.AggregatedTreeWriter;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.execution.maven.pom.MavenTestExecutor;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.TestTransformer;
import kieker.monitoring.writer.filesystem.BinaryLogStreamHandler;

public class AOPXMLHelper {

   public static void writeAOPXMLToFile(final List<String> allClasses, final File goalFile, final String aspectName) throws IOException {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(goalFile))) {
         writer.write("<!DOCTYPE aspectj PUBLIC \"-//AspectJ//DTD//EN\" \"http://www.aspectj.org/dtd/aspectj_1_5_0.dtd\">\n");
         writer.write("<aspectj>\n");
         writer.write(" <weaver options=\"-verbose\">\n");
         writer.write("   <include within=\"de.peass.generated.GeneratedTest\" />\n");
         for (final String clazz : allClasses) {
            if (!clazz.contains("$")) { // Fix: Kieker 1.12 is not able to read inner-class-entries
               writer.write("   <include within=\"" + clazz + "\" />\n");
            }
         }
         writer.write(" </weaver>\n");
         writer.write(" <aspects>");
         writer.write("    <aspect ");
         writer.write("name=\"" + aspectName + "\" />");
         writer.write(" </aspects>\n");
         writer.write("</aspectj>");
         writer.flush();
      }
   }

   public static final String AGGREGATED_WRITER = "de.dagere.kopeme.kieker.writer.AggregatedTreeWriter";
   public static final String CHANGEABLE_WRITER = "de.dagere.kopeme.kieker.writer.ChangeableFolderWriter";
   public static final String ONE_CALL_WRITER = "de.dagere.kopeme.kieker.writer.onecall.OneCallWriter";

   public static void writeKiekerMonitoringProperties(final File goalFile, final TestTransformer transformer, final PeassFolders folders) throws IOException {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(goalFile))) {
         writer.write("kieker.monitoring.name=KIEKER-KoPeMe\n");
         KiekerConfig kiekerConfig = transformer.getConfig().getKiekerConfig();
         if (kiekerConfig.isUseCircularQueue()) {
            writer.write("kieker.monitoring.core.controller.WriterController.RecordQueueFQN=de.dagere.kopeme.collections.SynchronizedCircularFifoQueue\n");
            writer.write("kieker.monitoring.core.controller.WriterController.QueuePutStrategy=kieker.monitoring.queue.putstrategy.YieldPutStrategy\n");
            writer.write("kieker.monitoring.core.controller.WriterController.QueueTakeStrategy=kieker.monitoring.queue.takestrategy.YieldTakeStrategy\n");
         } else {
            writer.write("kieker.monitoring.core.controller.WriterController.RecordQueueFQN=java.util.concurrent.LinkedBlockingQueue\n");
         }
         if (kiekerConfig.isOnlyOneCallRecording()) {
            writer.write("kieker.monitoring.writer=" + ONE_CALL_WRITER + "\n");
         } else if (kiekerConfig.isUseAggregation()) {
            writer.write("kieker.monitoring.writer=" + AGGREGATED_WRITER + "\n");
            writer.write(AggregatedTreeWriter.CONFIG_WRITE_INTERVAL + "=" + kiekerConfig.getKiekerAggregationInterval() + "\n");
         } else {
            writer.write("kieker.monitoring.writer=" + CHANGEABLE_WRITER + "\n");
            writer.write(CHANGEABLE_WRITER + ".realwriter=FileWriter\n");
         }
         if (transformer.isIgnoreEOIs()) {
            writer.write(AggregatedTreeWriter.CONFIG_IGNORE_EOIS + "=true\n");
         }
         if (kiekerConfig.isEnableAdaptiveMonitoring()) {
            writer.write("kieker.monitoring.adaptiveMonitoring.enabled=true\n");
            writer.write("kieker.monitoring.adaptiveMonitoring.configFile=" + MavenTestExecutor.KIEKER_ADAPTIVE_FILENAME + "\n");
            writer.write("kieker.monitoring.adaptiveMonitoring.readInterval=15\n");
         }
         if (kiekerConfig.isUseAggregation()) {
            String tempFolderPath = folders.getTempMeasurementFolder().getAbsolutePath();
            if (EnvironmentVariables.isWindows()) {
               // To avoid problems with escape signs on windows, also use / on windows for the path
               tempFolderPath = tempFolderPath.replace('\\', '/');
            }
            writer.write(AggregatedTreeWriter.CONFIG_PATH + "=" + tempFolderPath + "\n");
         }

         writer.write("kieker.monitoring.core.controller.WriterController.RecordQueueSize=" + kiekerConfig.getKiekerQueueSize() + "\n");
         writer.write(CHANGEABLE_WRITER + ".flush=false\n");
         if (!kiekerConfig.isUseAggregation()) {
            writer.write("kieker.monitoring.writer.filesystem.FileWriter.logStreamHandler=kieker.monitoring.writer.filesystem.BinaryLogStreamHandler\n");
         }
         writer.flush();
      }
   }
}
