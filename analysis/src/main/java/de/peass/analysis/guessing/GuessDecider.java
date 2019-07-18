package de.peass.analysis.guessing;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

public class GuessDecider {

   // public static final String COMPLEX_ALGORITHM = "COMPLEX_ALGORITHM";
   // public static final String INT_LONG = "INT_LONG";
   // public static final String EFFICIENT_STRING = "EFFICIENT_STRING";
   // public static final String STREAM = "STREAM";
   // public static final String EXCEPTION_CHANGE = "EXCEPTION_CHANGE";
   // public static final String REUSE_BUFFER = "REUSE_BUFFER";
   // public static final String ADITIONAL_PROCESSING = "ADITIONAL_PROCESSING";
   // public static final String API_USAGE = "API_USAGE";

   private final File versionFolder;

   public GuessDecider(File folder) {
      this.versionFolder = folder;
   }

   interface ConditionChecker {
      boolean check(String line);
   }

   public Guess guess(Set<String> methods) throws IOException {
      Guess currentGuess = new Guess();
      for (String method : methods) {
         if (method.contains("#")) {
            Patch<String> patch = getDiff(method);

            for (Guesser guesser : Guesser.allGuessers) {
               for (Delta<String> delta : patch.getDeltas()) {
                  if (guesser.isGuessTrue(delta)) {
                     currentGuess.add(guesser);
                  }
               }
            }
         }
      }
      return currentGuess;
   }

   public Patch<String> getDiff(String method) throws IOException {
      String realMethod;
      if (method.contains("(")) {
         realMethod = method.substring(method.lastIndexOf('.') + 1, method.indexOf('('));
      } else {
         realMethod = method.substring(method.lastIndexOf('.') + 1);
      }

      String source = getSource(realMethod + "_slow.txt");
      String sourceOld = getSource(realMethod + "_fast.txt");
      Patch<String> patch = DiffUtils.diff(Arrays.asList(source.split("\n")), Arrays.asList(sourceOld.split("\n")));
      return patch;
   }

   private String getSource(String method) throws IOException {
      File sourceFile = new File(versionFolder, method);
      if (sourceFile.exists()) {
         String source = FileUtils.readFileToString(sourceFile, Charset.defaultCharset());
         return source;
      } else {
         return "";
      }
   }
}