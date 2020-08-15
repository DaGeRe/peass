package de.peass.debug;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

class MeasuringWriter extends Writer {

   private final Writer writer;

   private long charactersWritten;

   public MeasuringWriter(final Writer writer) {
      this.writer = writer;
   }

   public long getCharactersWritten() {
      return this.charactersWritten;
   }

   @Override
   public void write(final char[] cbuf, final int off, final int len) throws IOException {
      this.writer.write(cbuf, off, len);
      this.charactersWritten += len;
   }

   @Override
   public void flush() throws IOException {
      this.writer.flush();
   }

   @Override
   public void close() throws IOException {
      this.writer.close();
   }

}

public class JavaIOPerformance {
   public static void main(String[] args) throws IOException {

      System.out.println("Start");

      File newFile = new File("test");
      if (newFile.exists()) {
         newFile.delete();
      }

      File folder = new File("testFolder");

      PrintWriter currentFileWriter = getKiekerWriter(newFile, folder);

      for (int gb = 0; gb < 1000; gb++) {
         long start = System.currentTimeMillis();
         for (int mb = 0; mb < 10000; mb++) {
//            StringBuffer all = new StringBuffer();
            for (int j = 0; j < 512; j++) {
//               all.append(j);
                currentFileWriter.write(j);
            }
//            currentFileWriter.write(all.toString());
         }
         System.out.println(System.currentTimeMillis() - start);
      }

      currentFileWriter.close();

      // File test = new File("testFile");

      // try (PrintStream printer = new PrintStream(test)) {
      // for (int mb = 0; mb < 1000000; mb++) {
      // for (int j = 0; j < 512; j++) {
      // printer.write(j);
      // }
      // }
      // }

      // try (PrintWriter printer = new PrintWriter(test)) {
      // for (int mb = 0; mb < 1000000; mb++) {
      // for (int j = 0; j < 512; j++) {
      // printer.write(j);
      // }
      // }
      // }

      // try (BufferedWriter writer = new BufferedWriter(new FileWriter(test))) {
      // for (int mb = 0; mb < 1000000; mb++) {
      // for (int j = 0; j < 512; j++) {
      // writer.write(j);
      // }
      // }
      // }
      // System.out.println("Finished");
   }

   private static PrintWriter getKiekerWriter(File newFile, File folder) throws IOException {
      Files.createDirectories(folder.toPath());

      // use CREATE_NEW to fail if the file already exists
      OutputStream outputStream = Files.newOutputStream(newFile.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

      Writer writer = new OutputStreamWriter(outputStream);
      writer = new BufferedWriter(writer);
      MeasuringWriter currentMeasuringWriter = new MeasuringWriter(writer);
      PrintWriter currentFileWriter = new PrintWriter(currentMeasuringWriter);
      return currentFileWriter;
   }

}
