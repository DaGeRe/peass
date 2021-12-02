package de.dagere.peass.dependency.execution.gradle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.execution.ProjectModules;

public class SettingsFileParser {

   private static final Logger LOG = LogManager.getLogger(SettingsFileParser.class);

   private String prefix = "";
   private String suffix = "";

   private void readPrefixAndSuffix(final File settingsFile) {
      try (BufferedReader reader = new BufferedReader(new FileReader(settingsFile))) {
         String line;
         boolean inRootProjectDefinition = false;
         while ((line = reader.readLine()) != null) {
            if (line.startsWith("rootProject.children.each {")) {
               inRootProjectDefinition = true;
            }
            if (inRootProjectDefinition) {
               String trimmedLined = line.trim();
               if (trimmedLined.startsWith("subproject.projectDir")) {
                  String fileDefinition = trimmedLined.substring(trimmedLined.indexOf("=") + 1).trim();
                  if (fileDefinition.startsWith("file(")) {
                     String withoutFile = fileDefinition.substring("file(".length()).trim();
                     if (withoutFile.startsWith("\"")) {
                        String withoutLeadingQuotation = withoutFile.substring(1);
                        prefix = withoutLeadingQuotation.substring(0, withoutLeadingQuotation.indexOf("\""));
                        withoutFile = withoutLeadingQuotation.substring(withoutLeadingQuotation.indexOf("\"") + 1);
                     }
                     if (withoutFile.contains("\"")) {
                        suffix = withoutFile.substring(withoutFile.indexOf("\"") + 1, withoutFile.lastIndexOf("\""));
                     }
                     LOG.info("prefix: " + prefix + " Suffix: " + suffix);
                  }

               }
            }

            if (line.trim().equals("}") && inRootProjectDefinition) {
               inRootProjectDefinition = false;
            }
         }
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public static ProjectModules getModules(final File projectFolder) {
      final File settingsFile = new File(projectFolder, "settings.gradle");
      final List<File> modules = new LinkedList<>();
      if (settingsFile.exists()) {
         SettingsFileParser parser = new SettingsFileParser();
         parser.readPrefixAndSuffix(settingsFile);
         parser.readModules(projectFolder, settingsFile, modules);
      } else {
         LOG.debug("settings-file {} not found", settingsFile);
      }
      modules.add(projectFolder);
      return new ProjectModules(modules);
   }

   private void readModules(final File projectFolder, final File settingsFile, final List<File> modules) {
      try (BufferedReader reader = new BufferedReader(new FileReader(settingsFile))) {
         String line;
         while ((line = reader.readLine()) != null) {
            parseModuleLine(projectFolder, modules, line);
         }
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private void parseModuleLine(final File projectFolder, final List<File> modules, final String line) {
      final String[] splitted = line.replaceAll("[ ,]+", " ").split(" ");
      if (splitted[0].equals("include")) {
         for (int candidateIndex = 1; candidateIndex < splitted.length; candidateIndex++) {
            final String candidate = splitted[candidateIndex].substring(1, splitted[candidateIndex].length() - 1);
            String folderName = prefix + candidate.replace(':', File.separatorChar) + suffix;
            final File module = new File(projectFolder, folderName);
            if (module.exists()) {
               modules.add(module);
            } else {
               LOG.error(line + " not found! Was looking in " + module.getAbsolutePath());
            }
         }
      }
   }
}
