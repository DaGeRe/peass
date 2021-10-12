package de.dagere.peass.dependency.execution;

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

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.testtransformation.TestTransformer;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;
import net.kieker.sourceinstrumentation.InstrumentationConfiguration;
import net.kieker.sourceinstrumentation.instrument.InstrumentKiekerSource;

public class KiekerEnvironmentPreparer {

   private static final Logger LOG = LogManager.getLogger(KiekerEnvironmentPreparer.class);

   private static final String[] metaInfFolders = new String[] { "src/main/resources/META-INF", "src/java/META-INF", "src/test/resources/META-INF", "src/test/META-INF",
         "target/test-classes/META-INF" };

   private final Set<String> includedMethodPattern;
   private final List<String> existingClasses;
   private final PeassFolders folders;
   private final TestTransformer testTransformer;
   private List<File> modules;

   public KiekerEnvironmentPreparer(final Set<String> includedMethodPattern, final List<String> existingClasses, final PeassFolders folders, final TestTransformer testTransformer, final List<File> modules) {
      this.includedMethodPattern = includedMethodPattern;
      this.existingClasses = existingClasses;
      this.folders = folders;
      this.testTransformer = testTransformer;
      this.modules = modules;
   }

   public void prepareKieker() throws IOException, InterruptedException {
      final MeasurementConfiguration config = testTransformer.getConfig();
      if (config.getKiekerConfig().isUseSourceInstrumentation()) {
         instrumentSources(config);
      } else {
         if (config.getKiekerConfig().isEnableAdaptiveMonitoring()) {
            prepareAdaptiveExecution();
         }
         if (AllowedKiekerRecord.DURATION.equals(config.getKiekerConfig().getRecord())) {
            generateAOPXML(AllowedKiekerRecord.DURATION);
         } else {
            generateAOPXML(AllowedKiekerRecord.OPERATIONEXECUTION);
         }
      }
      generateKiekerMonitoringProperties();
   }

   private void instrumentSources(final MeasurementConfiguration config) throws IOException {
      final InstrumentKiekerSource instrumentKiekerSource;
      LOG.debug("Create default constructor: {}", config.getExecutionConfig().isCreateDefaultConstructor());
      final HashSet<String> excludedPatterns = new HashSet<>();

      buildJettyExclusion(excludedPatterns);

      if (!config.getKiekerConfig().isUseSelectiveInstrumentation()) {
         InstrumentationConfiguration kiekerConfiguration = new InstrumentationConfiguration(config.getKiekerConfig().getRecord(), false, config.getExecutionConfig().isCreateDefaultConstructor(),
               config.getKiekerConfig().isAdaptiveInstrumentation(), includedMethodPattern, excludedPatterns, false, config.getRepetitions(), config.getKiekerConfig().isExtractMethod());
         instrumentKiekerSource = new InstrumentKiekerSource(kiekerConfiguration);
      } else {
         InstrumentationConfiguration kiekerConfiguration = new InstrumentationConfiguration(config.getKiekerConfig().getRecord(), config.getKiekerConfig().isUseAggregation(),
               config.getExecutionConfig().isCreateDefaultConstructor(),
               config.getKiekerConfig().isAdaptiveInstrumentation(), includedMethodPattern, excludedPatterns, true, config.getRepetitions(), 
               config.getKiekerConfig().isExtractMethod());
         instrumentKiekerSource = new InstrumentKiekerSource(kiekerConfiguration);
      }
      instrumentKiekerSource.instrumentProject(folders.getProjectFolder());
      if (config.getKiekerConfig().isEnableAdaptiveMonitoring()) {
         writeConfig();
      }
   }

   private void buildJettyExclusion(final HashSet<String> excludedPatterns) {
      for (String notInstrumenting : new String[] { "org.eclipse.jetty.logging.JettyLevel", "org.eclipse.jetty.logging.JettyLoggerConfiguration",
            "org.eclipse.jetty.logging.JettyLoggingServiceProvider", "org.eclipse.jetty.logging.JettyLoggerFactory", "org.eclipse.jetty.logging.StdErrAppender",
            "org.eclipse.jetty.logging.Timestamp", "org.eclipse.jetty.logging.Timestamp$Tick",
            "org.eclipse.jetty.logging.JettyLogger" }) {
         excludedPatterns.add("new " + notInstrumenting + ".<init>(..)");
         excludedPatterns.add("* " + notInstrumenting + ".*(..)"); // package visibility things from JettyLoggingServiceProvider with any return
         excludedPatterns.add("*[] " + notInstrumenting + ".*(..)");
         excludedPatterns.add("*.* " + notInstrumenting + ".*(..)");
         excludedPatterns.add("*.*.* " + notInstrumenting + ".*(..)");
         excludedPatterns.add("*.*.*.* " + notInstrumenting + ".*(..)");
         excludedPatterns.add("*.*.*.*.* " + notInstrumenting + ".*(..)");
      }
   }

   private void generateKiekerMonitoringProperties() {
      try {
         for (final File module : modules) {
            for (final String potentialReadFolder : metaInfFolders) {
               final File folder = new File(module, potentialReadFolder);
               folder.mkdirs();
               final File propertiesFile = new File(folder, "kieker.monitoring.properties");
               AOPXMLHelper.writeKiekerMonitoringProperties(propertiesFile, testTransformer, folders);
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

   private void generateAOPXML(final AllowedKiekerRecord aspect) {
      try {
         for (final File module : modules) {
            for (final String potentialReadFolder : metaInfFolders) {
               final File folder = new File(module, potentialReadFolder);
               folder.mkdirs();
               final File goalFile2 = new File(folder, "aop.xml");
               final Set<String> clazzes = getClazzSet();
               AOPXMLHelper.writeAOPXMLToFile(new LinkedList<String>(clazzes), goalFile2, aspect);
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   private Set<String> getClazzSet() {
      final Set<String> clazzes = new HashSet<String>();
      if (includedMethodPattern != null) {
         for (String method : includedMethodPattern) {
            final String methodBeforeParameters = method.substring(0, method.indexOf('('));
            final String clazz = methodBeforeParameters.substring(methodBeforeParameters.lastIndexOf(' ') + 1, methodBeforeParameters.lastIndexOf('.'));
            clazzes.add(clazz);
         }
      } else {
         clazzes.addAll(existingClasses);
      }
      return clazzes;
   }
}
