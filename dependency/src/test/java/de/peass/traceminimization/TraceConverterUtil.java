package de.peass.traceminimization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts a real trace in a more readable trace, converting every method call to a letter. Helps if a problem occurs in a big, real trace, and debugging is necessary.
 * 
 * @author reichelt
 *
 */
public class TraceConverterUtil {
   public static void main(String[] args) throws IOException {
      final File traceFile = new File(args[0]);
      final File newTraceFile = new File(traceFile.getParentFile(), traceFile.getName() + "_clean");

      try (final BufferedReader reader = new BufferedReader(new FileReader(traceFile))){
         final BufferedWriter writer = new BufferedWriter(new FileWriter(newTraceFile));

         final Map<String, String> methodIndexMap = new HashMap<>();
         int index = 65; // char 'a'

         String line;
         while ((line = reader.readLine()) != null) {
            final String splitted[] = line.split(";");
            if (splitted[0].equals("$1")) {
               final String callName = splitted[2];
               String letter = methodIndexMap.get(callName);
               if (letter == null) {
                  final char currentLetter = (char) index;
                  System.out.println("New char assigned: " + currentLetter);
                  letter = String.valueOf(currentLetter);
                  methodIndexMap.put(callName, letter);
                  index++;
                  if (index == 91) {
                     index = 97;
                  }
               }
               final String writeLine = line.replace(callName, letter);
               writer.write(writeLine + "\n");
            } else {
               writer.write(line + "\n");
            }
            writer.flush();
         }
         writer.close();
      }
      
   }
}
