package de.dagere.peass.ci.logHandling;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class RedirectionPrintStream extends PrintStream {

   public static final PrintStream ORIGINAL_OUT = System.out;
   public static final PrintStream ORIGINAL_ERR = System.err;

   private static final Map<Thread, PrintStream> printStreams = new HashMap<>();

   private static final Map<Thread, Stack<PrintStream>> overwrittenStreams = new HashMap<>();

   public RedirectionPrintStream(final OutputStream out) {
      super(out);
   }

   public synchronized void addRedirection(final Thread thread, final PrintStream stream) {
      PrintStream oldRedirection = printStreams.get(thread);
      if (oldRedirection != null) {
         Stack<PrintStream> overwriteList = overwrittenStreams.get(thread);
         if (overwriteList == null) {
            overwriteList = new Stack<>();
            overwrittenStreams.put(thread, overwriteList);
         }
         overwriteList.add(oldRedirection);
      }
      printStreams.put(thread, stream);
   }

   public synchronized void removeRedirection(final Thread thread) {
      printStreams.remove(thread);
      if (overwrittenStreams.get(thread) != null) {
         Stack<PrintStream> overwriteList = overwrittenStreams.get(thread);
         PrintStream lastStream = overwriteList.pop();
         printStreams.put(thread, lastStream);
         if (overwriteList.size() == 0) {
            overwrittenStreams.remove(thread);
         }
      }
   }

   public int redirectionCount() {
      return printStreams.size();
   }

   @Override
   public void println(final String content) {
      ORIGINAL_OUT.println("Writing " + Thread.currentThread() + " " + content);
      PrintStream printStream = printStreams.get(Thread.currentThread());
      if (printStream != null) {
         printStream.println(content);
      } else {
         super.println(content);
      }
   }

   @Override
   public void write(final byte[] buf, final int off, final int len) {
      PrintStream printStream = printStreams.get(Thread.currentThread());
      if (printStream != null) {
         printStream.write(buf, off, len);
      } else {
         super.write(buf, off, len);
      }
   }

}