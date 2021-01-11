package de.peass.ci;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class LogRedirector implements AutoCloseable {

   public static final String PATTERN = "%d{HH:mm:ss.SSS} [%t] %-5level %logger{36}:%L - %msg%n";

   private static final Logger LOG = LogManager.getLogger(LogRedirector.class);

   /**
    * For (manual) testing purposes
    * 
    * @param args
    * @throws Exception
    * @throws FileNotFoundException
    */
   public static void main(String[] args) throws FileNotFoundException, Exception {
      File logFile = new File("test.txt");

//      redirectLogToFile(logFile);
//
//      LOG.debug("test - should go to file");
//      System.out.println("File 1");
//
//      Configurator.reconfigure();
//      LOG.debug("test - should go to console");
//      System.out.println("File 2");

      try (LogRedirector director = new LogRedirector(logFile)) {
         System.out.println("Should go to file");
         LOG.debug("test - should go to file");
      }
      LOG.debug("test - should go to console");
      System.out.println("Should go to console");
   }

   private static void redirectLogToFile(File logFile) {
      ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

      builder.setStatusLevel(Level.INFO);
      // naming the logger configuration
      builder.setConfigurationName("DefaultLogger");

      // create a console appender
      AppenderComponentBuilder appenderBuilder = builder.newAppender("ToFile", "File")
            .addAttribute("fileName", logFile.getAbsolutePath());
      // add a layout like pattern, json etc
      appenderBuilder.add(builder.newLayout("PatternLayout")
            .addAttribute("pattern", PATTERN));
      RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.DEBUG);
      rootLogger.add(builder.newAppenderRef("ToFile"));

      builder.add(appenderBuilder);
      builder.add(rootLogger);
      Configurator.reconfigure(builder.build());
   }

   private final PrintStream oldOut;
   private final PrintStream oldErr;

   public LogRedirector(File file) throws FileNotFoundException {

      oldOut = System.out;
      oldErr = System.err;
      redirectLogToFile(file);
      System.setOut(new PrintStream(file));
      System.setErr(new PrintStream(file));
   }

   @Override
   public void close() {
      System.setOut(oldOut);
      System.setErr(oldErr);
      Configurator.reconfigure();
   }
}
