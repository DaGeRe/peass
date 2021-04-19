package de.peass.visualization;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class RCAFolderSearcher {

   private File[] data;

   public RCAFolderSearcher(final File data) {
      this.data = new File[] { data };
   }

   public RCAFolderSearcher(final File[] data) {
      this.data = data;
   }

   public List<File> searchPeassFiles() throws IOException, JsonParseException, JsonMappingException, JsonProcessingException, FileNotFoundException {
      List<File> peassFilesToHandle = new LinkedList<>();
      for (final File source : data) {
         if (source.isDirectory()) {
            if (source.getName().endsWith("_peass")) {
               peassFilesToHandle.add(source);
            }
         }
      }
      return peassFilesToHandle;
   }

   public List<File> searchRCAFiles() throws IOException, JsonParseException, JsonMappingException, JsonProcessingException, FileNotFoundException {
      List<File> rcaFilesToHandle = new LinkedList<>();
      for (final File source : data) {
         if (source.isDirectory()) {
            if (source.getName().endsWith("_peass")) {
               rcaFilesToHandle.addAll(handlePeassFolder(source));
            } else if (source.getName().equals("galaxy") || source.getParentFile().getName().contains("galaxy")) {
               rcaFilesToHandle.addAll(handleSlurmFolder(source));
            } else {
               boolean containsSlurmChild = false;
               for (final File child : source.listFiles()) {
                  if (child.getName().matches("[0-9]+_[0-9]+")) {
                     containsSlurmChild = true;
                  }
               }
               if (containsSlurmChild) {
                  rcaFilesToHandle.addAll(handleSlurmFolder(source));
               } else {
                  rcaFilesToHandle.addAll(handleSimpleFolder(source));
               }
            }
         } else {
            rcaFilesToHandle.add(source);
         }
      }
      return rcaFilesToHandle;
   }

   private List<File> handleSimpleFolder(final File source) throws IOException, JsonParseException, JsonMappingException, JsonProcessingException, FileNotFoundException {
      List<File> rcaFiles = new LinkedList<>();
      for (final File treeFile : source.listFiles()) {
         if (treeFile.getName().endsWith(".json")) {
            rcaFiles.add(treeFile);
         }
      }
      return rcaFiles;
   }

   private List<File> handleSlurmFolder(final File source) throws IOException, JsonParseException, JsonMappingException, JsonProcessingException, FileNotFoundException {
      List<File> rcaFiles = new LinkedList<>();
      for (final File job : source.listFiles()) {
         if (job.isDirectory()) {
            final File peassFolder = new File(job, "peass");
            rcaFiles.addAll(handlePeassFolder(peassFolder));
         }
      }
      return rcaFiles;
   }

   private List<File> handlePeassFolder(final File source) throws IOException, JsonParseException, JsonMappingException, JsonProcessingException, FileNotFoundException {
      List<File> rcaFiles = new LinkedList<>();
      final File rcaFolder = new File(source, "rca" + File.separator + "tree");
      if (rcaFolder.exists()) {
         for (final File versionFolder : rcaFolder.listFiles()) {
            for (final File testcaseFolder : versionFolder.listFiles()) {
               for (final File treeFile : testcaseFolder.listFiles()) {
                  if (treeFile.getName().endsWith(".json")) {
                     rcaFiles.add(treeFile);
                  }
               }
            }
         }
      }
      return rcaFiles;
   }
}
