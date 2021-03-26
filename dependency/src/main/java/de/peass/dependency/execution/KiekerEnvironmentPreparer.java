package de.peass.dependency.execution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.PeASSFolders;
import de.peass.testtransformation.JUnitTestTransformer;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;
import net.kieker.sourceinstrumentation.InstrumentationConfiguration;
import net.kieker.sourceinstrumentation.instrument.InstrumentKiekerSource;

public class KiekerEnvironmentPreparer {
   
   private static final Logger LOG = LogManager.getLogger(KiekerEnvironmentPreparer.class);

   private final Set<String> includedMethodPattern;
   private final PeASSFolders folders;
   private final JUnitTestTransformer testTransformer;
   private List<File> modules;
   private List<String> existingClasses;

   public KiekerEnvironmentPreparer(final Set<String> includedMethodPattern, final PeASSFolders folders, final JUnitTestTransformer testTransformer, final List<File> modules,
         final List<String> existingClasses) {
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
         LOG.debug("Create default constructor: {}", config.getExecutionConfig().isCreateDefaultConstructor());
         if (!config.isUseSelectiveInstrumentation()) {
            InstrumentationConfiguration kiekerConfiguration = new InstrumentationConfiguration(config.getRecord(), false, config.getExecutionConfig().isCreateDefaultConstructor(),
                  config.isEnableAdaptiveConfig(), includedMethodPattern, false);
            instrumentKiekerSource = new InstrumentKiekerSource(kiekerConfiguration);
         } else {
            InstrumentationConfiguration kiekerConfiguration = new InstrumentationConfiguration(config.getRecord(), config.isUseSampling(), config.getExecutionConfig().isCreateDefaultConstructor(),
                  config.isEnableAdaptiveConfig(), includedMethodPattern, true);
            instrumentKiekerSource = new InstrumentKiekerSource(kiekerConfiguration);
         }
         instrumentKiekerSource.instrumentProject(folders.getProjectFolder());
         if (config.isEnableAdaptiveConfig()) {
            writeConfig();
         }
         generateKiekerMonitoringProperties();
      } else {
         if (config.isEnableAdaptiveConfig()) {
            prepareAdaptiveExecution();
         }
         if (AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION.equals(config.getRecord()) && config.isEnableAdaptiveConfig()) {
            generateAOPXML(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION);
            generateKiekerMonitoringProperties();
         } else {
            generateAOPXML(AllowedKiekerRecord.OPERATIONEXECUTION);
            generateKiekerMonitoringProperties();
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

   private void generateAOPXML(final AllowedKiekerRecord aspect) {
      try {
         for (final File module : modules) {
            for (final String potentialReadFolder : metaInfFolders) {
               final File folder = new File(module, potentialReadFolder);
               folder.mkdirs();
               final File goalFile2 = new File(folder, "aop.xml");
               final Set<String> clazzes = new HashSet<String>();
               for (String method : includedMethodPattern) {
                  final String methodBeforeParameters = method.substring(0, method.indexOf('('));
                  final String clazz = methodBeforeParameters.substring(methodBeforeParameters.lastIndexOf(' ') + 1, methodBeforeParameters.lastIndexOf('.'));
                  clazzes.add(clazz);
               }
               AOPXMLHelper.writeAOPXMLToFile(new LinkedList<String>(clazzes), goalFile2, aspect);
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
