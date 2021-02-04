package de.peass.dependency.execution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import de.peass.testtransformation.JUnitTestTransformer;

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

   public static void writeKiekerMonitoringProperties(final File goalFile, final JUnitTestTransformer transformer) throws IOException {
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
            writer.write("kieker.monitoring.writer=kieker.monitoring.writer.filesystem.AggregatedTreeWriter\n");
            writer.write("kieker.monitoring.writer.filesystem.AggregatedTreeWriter.writeInterval=" + transformer.getConfig().getKiekerAggregationInterval() + "\n");
         } else {
            writer.write("kieker.monitoring.writer=kieker.monitoring.writer.filesystem.ChangeableFolderWriter\n");
            writer.write("kieker.monitoring.writer.filesystem.ChangeableFolderWriter.realwriter=FileWriter\n");
         }
         if (transformer.isIgnoreEOIs()) {
            writer.write("kieker.monitoring.writer.filesystem.AggregatedTreeWriter.ignoreEOIs=true\n");
         }
         if (transformer.isAdaptiveExecution()) {
            if (transformer.getConfig().isUseSourceInstrumentation()) {
               writer.write("kieker.monitoring.adaptiveMonitoring.enabled=true\n");
               writer.write("kieker.monitoring.adaptiveMonitoring.configFile=" + MavenTestExecutor.KIEKER_ADAPTIVE_FILENAME + "\n");
               writer.write("kieker.monitoring.adaptiveMonitoring.readInterval=15\n");
            } else {
               writer.write("kieker.monitoring.adaptiveMonitoring.enabled=true\n");
               writer.write("kieker.monitoring.adaptiveMonitoring.configFile=" + MavenTestExecutor.KIEKER_ADAPTIVE_FILENAME + "\n");
               writer.write("kieker.monitoring.adaptiveMonitoring.readInterval=15\n");
            }
         }

         final int queueSize = 10000000;
         writer.write("kieker.monitoring.core.controller.WriterController.RecordQueueSize=" + queueSize + "\n");
         writer.write("kieker.monitoring.writer.filesystem.ChangeableFolderWriter.flush=false\n");
         writer.write("kieker.monitoring.writer.filesystem.FileWriter.logStreamHandler=kieker.monitoring.writer.filesystem.BinaryLogStreamHandler\n");
         writer.flush();
      }
   }
}
