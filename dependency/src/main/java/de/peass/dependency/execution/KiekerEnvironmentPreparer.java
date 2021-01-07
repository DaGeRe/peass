package de.peass.dependency.execution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.PeASSFolders;
import de.peass.kiekerInstrument.InstrumentKiekerSource;
import de.peass.testtransformation.JUnitTestTransformer;

public class KiekerEnvironmentPreparer {
   
   private final Set<String> includedMethodPattern;
   private final PeASSFolders folders;
   private final JUnitTestTransformer testTransformer;
   private List<File> modules;
   private List<String> existingClasses;

   public KiekerEnvironmentPreparer(Set<String> includedMethodPattern, PeASSFolders folders, JUnitTestTransformer testTransformer, List<File> modules,
         List<String> existingClasses) {
      this.includedMethodPattern = includedMethodPattern;
      this.folders = folders;
      this.testTransformer = testTransformer;
      this.modules = modules;
      this.existingClasses = existingClasses;
   }

   public void prepareKieker() throws IOException, InterruptedException {
      final MeasurementConfiguration config = testTransformer.getConfig();
      if (config.isUseSourceInstrumentation()) {
         final InstrumentKiekerSource instrumentKiekerSource;
         if (!config.isUseSelectiveInstrumentation()) {
            instrumentKiekerSource = new InstrumentKiekerSource(config.getRecord());
         } else {
            instrumentKiekerSource = new InstrumentKiekerSource(config.getRecord(), includedMethodPattern, config.isUseSampling());
         }
         instrumentKiekerSource.instrumentProject(folders.getProjectFolder());
         if (testTransformer.isAdaptiveExecution()) {
            writeConfig();
         }
         generateKiekerMonitoringProperties();
      } else {
         if (testTransformer.isAdaptiveExecution()) {
            prepareAdaptiveExecution();
         }
         if (AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION.equals(config.getRecord()) && testTransformer.isAdaptiveExecution()) {
            generateAOPXML(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION);
            generateKiekerMonitoringProperties();
         } else {
            generateAOPXML(AllowedKiekerRecord.OPERATIONEXECUTION);
            generateKiekerMonitoringProperties();
         }
         if (testTransformer.isAggregatedWriter()) {

         }
      }
   }

   private void generateKiekerMonitoringProperties() {
      try {
         for (final File module : modules) {
            for (final String potentialReadFolder : metaInfFolders) {
               final File folder = new File(module, potentialReadFolder);
               folder.mkdirs();
               final File propertiesFile = new File(folder, "kieker.monitoring.properties");
               AOPXMLHelper.writeKiekerMonitoringProperties(propertiesFile, testTransformer);
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   private void prepareAdaptiveExecution() throws IOException, InterruptedException {
      if (!MavenTestExecutor.KIEKER_ASPECTJ_JAR.exists()) {
         // This can be removed if Kieker 1.14 is released
         throw new RuntimeException("Tweaked Kieker " + MavenTestExecutor.KIEKER_ASPECTJ_JAR + " needs to exist - git clone https://github.com/DaGeRe/kieker -b 1_13_tweak "
               + "and install manually!");
      }
      writeConfig();
   }

   private void writeConfig() throws IOException {
      final File configFolder = new File(folders.getProjectFolder(), "config");
      configFolder.mkdir();

      final File adaptiveFile = new File(folders.getProjectFolder(), MavenTestExecutor.KIEKER_ADAPTIVE_FILENAME);
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(adaptiveFile))) {
         writer.write("- *\n");
         for (final String includedMethod : includedMethodPattern) {
            writer.write("+ " + includedMethod + "\n");
         }

         writer.flush();
      }
   }

   private static final String[] metaInfFolders = new String[] { "src/main/resources/META-INF", "src/java/META-INF", "src/test/resources/META-INF", "src/test/META-INF",
         "target/test-classes/META-INF" };

   private void generateAOPXML(AllowedKiekerRecord aspect) {
      try {
         for (final File module : modules) {
            for (final String potentialReadFolder : metaInfFolders) {
               final File folder = new File(module, potentialReadFolder);
               folder.mkdirs();
               final File goalFile2 = new File(folder, "aop.xml");
               AOPXMLHelper.writeAOPXMLToFile(existingClasses, goalFile2, aspect);
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
