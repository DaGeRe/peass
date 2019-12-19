package de.peran.analysis.helper.all;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.ReadProperties;
import de.peass.analysis.properties.ChangeProperties;
import de.peass.analysis.properties.ChangeProperty;
import de.peass.analysis.properties.PropertyProcessor;
import de.peass.analysis.properties.PropertyReadHelper;

public class GroupAllProperties {

   static Map<String, String[]> keywords = new LinkedHashMap<>();

   static {
      keywords.put("control", new String[] {
            "if", "else", "break", "continue", "case", "default", "throw", "throws", "try", "return", "catch", "finally", "switch",
      });
      keywords.put("declare", new String[] {
            "private", "protected", "public", "class", "final", "interface", "static", "abstract", "extends", "implements", "void",
      });
      keywords.put("type", new String[] {
            "char", "boolean", "byte", "double", "int", "long", "false", "true", "null", "float", "short",
      });
      keywords.put("loop", new String[] {
            "do", "while", "for",
      });
      keywords.put("sync", new String[] {
            "synchronized", "transient", "volatile",
      });
      keywords.put("misc", new String[] {
            "goto", "assert", "package", "const", "new", "import", "instanceof", "native", "strictfp", "super", "this",
      });
   }

   static class PropertyGroupCSVGenerator implements PropertyProcessor {

      final BufferedWriter writer;
      final String project;

      public PropertyGroupCSVGenerator(final File folder, final String project) throws IOException {
         writer = new BufferedWriter(new FileWriter(new File(folder, "properties_groupable.csv")));
         this.project = project;
      }

      @Override
      public void process(final String version, final String testcase, final ChangeProperty change, final ChangeProperties changeProperties) {
         if (change.getCalls() != 0 && change.getCallsOld() != 0) {
            String line = project + ";";
            line += change.getDiff() + ";";
            line += change.getChangePercent() + ";";
            line += change.getCalls() + ";";
            line += change.getCallsOld() + ";";
            if (change.getTypes().contains("FUNCTION")) {
               line += "1;FUNCTION";
            } else if (change.getTypes().contains("FIX")) {
               line += "2;FIX";
            } else if (change.getTypes().contains("OPTIM")) {
               line += "3;OPTIM";
            } else if (change.getTypes().contains("UPDATE")) {
               line += "4;UPDATE";
            } else if (change.getTypes().contains("INTERFACE")) {
               line += "5;INTERFACE";
            } else {
               line += "10;NONE";
            }
            line += ";";
            System.out.println(change.getTypes() + " " + change.getDiff());
            for (final Map.Entry<String, String[]> keywordType : keywords.entrySet()) {
               final int words = getAddCount(change, keywordType.getValue());
               line += words + ";";
               final int wordsR = getRemoveCount(change, keywordType.getValue());
               line += wordsR + ";";
            }

            try {
               writer.write(line + "\n");
               writer.flush();
            } catch (final IOException e) {
               e.printStackTrace();
            }
         }
      }

      private int getAddCount(final ChangeProperty change, final String[] controlWords2) {
         int count = 0;
         for (final Entry<String, Integer> keyword : change.getAddedMap().entrySet()) {
            if (Arrays.asList(controlWords2).contains(keyword.getKey())) {
               count += keyword.getValue();
            }
         }
         return count;
      }

      private int getRemoveCount(final ChangeProperty change, final String[] controlWords2) {
         int count = 0;
         for (final Entry<String, Integer> keyword : change.getAddedMap().entrySet()) {
            if (Arrays.asList(controlWords2).contains(keyword.getKey())) {
               count += keyword.getValue();
            }
         }
         return count;
      }

   }

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      // for (String project : new String[] { "commons-csv", "commons-compress", "commons-dbcp", "commons-fileupload", "commons-imaging", "commons-io",
      // "commons-text" }) {
      // File folder = new File(CleanAll.defaultDataFolder, "results/" + project);
      // File propertiesFile = new File(folder, "properties.json");
      // VersionChangeProperties properties = FolderSearcher.MAPPER.readValue(propertiesFile, VersionChangeProperties.class);
      // properties.executeProcessor(new PropertyGroupCSVGenerator(folder, project));
      // }

      final File csvFile = new File("/home/reichelt/daten/diss/ergebnisse/normaltest/v26_symbolicComplete/results/props_all.csv");
      final File csvOutFile = new File("/home/reichelt/daten/diss/ergebnisse/normaltest/v26_symbolicComplete/results/props_all2.csv");

      // summarize(csvFile, csvOutFile);
      discretize(csvFile, csvOutFile);

      // File folder = new File("results/commons-io/");
      // File changesFile = new File(folder, "changes.json");

   }

   private static void summarize(final File csvFile, final File csvOutFile) throws IOException, FileNotFoundException {
      final BufferedWriter writer = new BufferedWriter(new FileWriter(csvOutFile));
      try (BufferedReader fileReader = new BufferedReader(new FileReader(csvFile))) {
         final Set<String> ids = new HashSet<>();

         String line = "#Diff" + ";" + "method" + ";" + "intensity" + ";" + "testChange;" + "traceChange" + ";";
         line += "calls" + ";" + "callsOld" + ";" + "oldTime" + ";";
         for (final String keyword : PropertyReadHelper.keywords) {
            line += keyword + "Added;";
         }
         writer.write(line.substring(0, line.length() - 1) + "\n");
         writer.flush();

         while ((line = fileReader.readLine()) != null) {

            final String[] splitted = line.split(";");
            if (!ids.contains(splitted[0])) {
               ids.add(splitted[0]);
               String outLine = "";
               for (int i = 0; i < splitted.length; i++) {
                  if (i == 2) {
                     final double val = Double.parseDouble(splitted[i]);
                     if (val < -40) {
                        outLine += "-4";
                     } else if (val < -30) {
                        outLine += "-3";
                     } else if (val < -20) {
                        outLine += "-2";
                     } else if (val < 0) {
                        outLine += "-1";
                     } else if (val < 20) {
                        outLine += "1";
                     } else {
                        outLine += "2";
                     }
                  }
                  if (i > 9) {
                     final int val = Integer.parseInt(splitted[i]) - Integer.parseInt(splitted[i + 1]);
                     i++;
                     if (val > 4) {
                        outLine += "2";
                     } else if (val > 0) {
                        outLine += "1";
                     } else if (val == 0) {
                        outLine += "0";
                     } else if (val < -3) {
                        outLine += "-2";
                     } else {
                        outLine += "-1";
                     }
                  } else {
                     outLine += splitted[i];
                  }
                  outLine += ";";
               }
               writer.write(outLine.substring(0, outLine.length() - 1) + "\n");
            }

         }
         writer.flush();
      }

   }

   private static void discretize(final File csvFile, final File csvOutFile) throws IOException, FileNotFoundException {
      final BufferedWriter writer = new BufferedWriter(new FileWriter(csvOutFile));
      try (BufferedReader fileReader = new BufferedReader(new FileReader(csvFile))) {
         final Set<String> ids = new HashSet<>();
         String line;

         ReadProperties.writeCSVHeadline(writer);

         while ((line = fileReader.readLine()) != null) {
            if (!line.startsWith("#")) {
               final String[] splitted = line.split(";");
               if (!ids.contains(splitted[0])) {
                  ids.add(splitted[0]);
                  String outLine = "";
                  for (int i = 0; i < splitted.length; i++) {
                     if (i == 2) {
                        outLine += "projectUnknown;";
                     }
                     if (i == 2) {
                        final double val = Double.parseDouble(splitted[i]);
                        if (val < -30) {
                           outLine += "-3";
                        } else if (val < 0) {
                           outLine += "-1";
                        } else if (val < 30) {
                           outLine += "1";
                        } else {
                           outLine += "3";
                        }
                     } else if (i == 5 || i == 6) {
                        final int val = Integer.parseInt(splitted[i]);
                        if (val > 100) {
                           outLine += "3";
                        } else if (val > 10) {
                           outLine += "2";
                        } else if (val > 5) {
                           outLine += "1";
                        } else {
                           outLine += "0";
                        }
                     } else if (splitted[i].matches("[0-9]*")) {
                        final int val = Integer.parseInt(splitted[i]);
                        if (val > 4) {
                           outLine += "2";
                        } else if (val > 0) {
                           outLine += "1";
                        } else {
                           outLine += "0";
                        }

                     } else {
                        outLine += splitted[i];
                     }
                     outLine += ";";
                  }
                  writer.write(outLine.substring(0, outLine.length() - 1) + "\n");
               }

            }
            writer.flush();
         }
      }

   }
}
