package de.dagere.peass.ci.logHandling;

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
   
   private static final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(LogManager.class.getClassLoader(), false);
   
   private static final RedirectionPrintStream threadRedirectionStream = new RedirectionPrintStream(System.out);
   
   private static OutputStreamAppender redirectionAppender;
   
   static {
      redirectionAppender = OutputStreamAppender.newBuilder()
            .setName("peass-redirection-logger")
            .setTarget(threadRedirectionStream)
            .setLayout(PatternLayout.newBuilder().withPattern("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36}:%L - %msg%n")
                  .build())
            .setConfiguration(loggerContext.getConfiguration()).build();
   }

   public static final String PATTERN = "%d{HH:mm:ss.SSS} [%t] %-5level %logger{36}:%L - %msg%n";

   private final Map<String, Appender> savedAppenders = new HashMap<String, Appender>();

   public LogRedirector(final File file) throws FileNotFoundException {
      final PrintStream changedLog = new PrintStream(file);
      threadRedirectionStream.addRedirection(Thread.currentThread(), changedLog);

      if (!redirectionAppender.isStarted()) {
         redirectionAppender.start();
         
         clearOldAppenders();

         loggerContext.getConfiguration().addAppender(redirectionAppender);
         loggerContext.getRootLogger().addAppender(loggerContext.getConfiguration().getAppender(redirectionAppender.getName()));
      }

      loggerContext.updateLoggers();

      System.setOut(threadRedirectionStream);
      System.setErr(threadRedirectionStream);
   }

   @Override
   public void close() {
      threadRedirectionStream.removeRedirection(Thread.currentThread());
      if (threadRedirectionStream.redirectionCount() == 0) {
         System.setOut(RedirectionPrintStream.ORIGINAL_OUT);
         System.setErr(RedirectionPrintStream.ORIGINAL_ERR);
         
         redirectionAppender.stop();
         loggerContext.getConfiguration().getAppenders().remove(redirectionAppender.getName());
         loggerContext.getRootLogger().removeAppender(redirectionAppender);

         addOldAppenders();
         loggerContext.updateLoggers();
      }
   }

   private void clearOldAppenders() {
      savedAppenders.putAll(loggerContext.getRootLogger().getAppenders());
      for (Appender appender : savedAppenders.values()) {
         loggerContext.getRootLogger().removeAppender(appender);
      }
   }
   
   private void addOldAppenders() {
      for (Appender appender : savedAppenders.values()) {
         loggerContext.getRootLogger().addAppender(appender);
      }
   }
}
