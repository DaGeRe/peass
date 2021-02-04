package de.peass.ci;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.OutputStreamAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class LogRedirector implements AutoCloseable {

   public static final String PATTERN = "%d{HH:mm:ss.SSS} [%t] %-5level %logger{36}:%L - %msg%n";

   private final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(LogManager.class.getClassLoader(), false);

   private final PrintStream oldOut;
   private final PrintStream oldErr;
   private final OutputStreamAppender fa;
   private final Map<String, Appender> appenders = new HashMap<String, Appender>();

   public LogRedirector(final File file) throws FileNotFoundException {
      oldOut = System.out;
      oldErr = System.err;
      
      final PrintStream changedLog = new PrintStream(file);
      
      fa = OutputStreamAppender.newBuilder()
            .setName("logger" + file.getName())
            .setTarget(changedLog)
            .setLayout(PatternLayout.newBuilder().withPattern("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36}:%L - %msg%n")
                  .build())
            .setConfiguration(loggerContext.getConfiguration()).build();
      fa.start();
      
      clearOldAppenders();

      loggerContext.getConfiguration().addAppender(fa);
      loggerContext.getRootLogger().addAppender(loggerContext.getConfiguration().getAppender(fa.getName()));
      
      loggerContext.updateLoggers();
      
      System.setOut(changedLog);
      System.setErr(changedLog);
   }

   private void clearOldAppenders() {
      appenders.putAll(loggerContext.getRootLogger().getAppenders());
      for (Appender appender : appenders.values()) {
         loggerContext.getRootLogger().removeAppender(appender);
      }
   }

   @Override
   public void close() {
      System.setOut(oldOut);
      System.setErr(oldErr);
      
      fa.stop();
      loggerContext.getConfiguration().getAppenders().remove(fa.getName());
      loggerContext.getRootLogger().removeAppender(fa);
      
      for (Appender appender : appenders.values()) {
         loggerContext.getRootLogger().addAppender(appender);
      }
      loggerContext.updateLoggers();
   }
}
