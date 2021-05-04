package de.dagere.peass.dependency.execution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import de.dagere.peass.testtransformation.TestTransformer;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;

public class AOPXMLHelper {

   public static void writeAOPXMLToFile(final List<String> allClasses, final File goalFile, final AllowedKiekerRecord record) throws IOException {
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
         writer.write("name=\"" + record.getFullName() + "\" />");
         writer.write(" </aspects>\n");
         writer.write("</aspectj>");
         writer.flush();
      }
   }

   public static final String AGGREGATED_WRITER = "de.dagere.kopeme.kieker.writer.AggregatedTreeWriter";
   public static final String CHANGEABLE_WRITER = "de.dagere.kopeme.kieker.writer.ChangeableFolderWriter";

   public static void writeKiekerMonitoringProperties(final File goalFile, final TestTransformer transformer) throws IOException {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(goalFile))) {
         writer.write("kieker.monitoring.name=KIEKER-KoPeMe\n");
         if (transformer.getConfig().isUseCircularQueue()) {
            writer.write("kieker.monitoring.core.controller.WriterController.RecordQueueFQN=de.dagere.kopeme.collections.SynchronizedCircularFifoQueue\n");
            writer.write("kieker.monitoring.core.controller.WriterController.QueuePutStrategy=kieker.monitoring.queue.putstrategy.YieldPutStrategy\n");
            writer.write("kieker.monitoring.core.controller.WriterController.QueueTakeStrategy=kieker.monitoring.queue.takestrategy.YieldTakeStrategy\n");
         } else {
            writer.write("kieker.monitoring.core.controller.WriterController.RecordQueueFQN=java.util.concurrent.LinkedBlockingQueue\n");
         }
         if (transformer.isAggregatedWriter()) {
            writer.write("kieker.monitoring.writer=" + AGGREGATED_WRITER + "\n");
            writer.write(AGGREGATED_WRITER + ".writeInterval=" + transformer.getConfig().getKiekerAggregationInterval() + "\n");
         } else {
            writer.write("kieker.monitoring.writer=" + CHANGEABLE_WRITER + "\n");
            writer.write(CHANGEABLE_WRITER + ".realwriter=FileWriter\n");
         }
         if (transformer.isIgnoreEOIs()) {
            writer.write(AGGREGATED_WRITER + ".ignoreEOIs=true\n");
         }
         if (transformer.getConfig().isEnableAdaptiveConfig()) {
            writer.write("kieker.monitoring.adaptiveMonitoring.enabled=true\n");
            writer.write("kieker.monitoring.adaptiveMonitoring.configFile=" + MavenTestExecutor.KIEKER_ADAPTIVE_FILENAME + "\n");
            writer.write("kieker.monitoring.adaptiveMonitoring.readInterval=15\n");
         }

         final int queueSize = 10000000;
         writer.write("kieker.monitoring.core.controller.WriterController.RecordQueueSize=" + queueSize + "\n");
         writer.write(CHANGEABLE_WRITER + ".flush=false\n");
         // writer.write("kieker.monitoring.writer.filesystem.FileWriter.logStreamHandler=kieker.monitoring.writer.filesystem.BinaryLogStreamHandler\n");
         writer.flush();
      }
   }
}
