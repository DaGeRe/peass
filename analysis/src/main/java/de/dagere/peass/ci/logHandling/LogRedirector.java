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

/**
 * This log redirector redirects the output logs of the *current thread* to a given file. Both, the System.out/err-logs and the log4j-logs will be redirected. This is 
 * Autoclosable, so on close, the redirection will be removed.
 * 
 * If multiple redirections appear, the *most recent* redirection of the *current thread* is used - i.e. if ThreadA redirects to File1 and then to File2, everything will *only* 
 * go to File2 until this redirection if closed, and then to File1, until this redirection is also closed. Logs of ThreadB should not be changed at all. 
 * @author reichelt
 *
 */
public class LogRedirector implements AutoCloseable {
   
   private static final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(LogManager.class.getClassLoader(), false);
   
   private static final RedirectionPrintStream threadRedirectionStream = new RedirectionPrintStream(System.out);
   
   private static final OutputStreamAppender redirectionAppender;
   
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

   public LogRedirector(final PrintStream redirectToStream) {
      threadRedirectionStream.addRedirection(Thread.currentThread(), redirectToStream);

      if (!redirectionAppender.isStarted()) {
         useLog4jRedirection();
      }

      System.setOut(threadRedirectionStream);
      System.setErr(threadRedirectionStream);
   }
   
   public LogRedirector(final File file) throws FileNotFoundException {
      this(new PrintStream(file));
   }

   @Override
   public void close() {
      threadRedirectionStream.removeRedirection(Thread.currentThread());
      if (threadRedirectionStream.redirectionCount() == 0) {
         System.setOut(RedirectionPrintStream.ORIGINAL_OUT);
         System.setErr(RedirectionPrintStream.ORIGINAL_ERR);
         
         unsetLog4jRedirection();
      }
   }
   
   private void useLog4jRedirection() {
      redirectionAppender.start();
      
      clearOldAppenders();

      loggerContext.getConfiguration().addAppender(redirectionAppender);
      loggerContext.getRootLogger().addAppender(loggerContext.getConfiguration().getAppender(redirectionAppender.getName()));
      
      loggerContext.updateLoggers();
   }

   private void unsetLog4jRedirection() {
      redirectionAppender.stop();
      loggerContext.getConfiguration().getAppenders().remove(redirectionAppender.getName());
      loggerContext.getRootLogger().removeAppender(redirectionAppender);

      addOldAppenders();
      loggerContext.updateLoggers();
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
