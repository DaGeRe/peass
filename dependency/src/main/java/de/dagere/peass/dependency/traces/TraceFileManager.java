package de.dagere.peass.dependency.traces;

import java.io.File;

import de.dagere.peass.config.TestSelectionConfig;

/**
 * Saves the trace files as .zip, if it is activated, or as .txt otherwise. If no ending is present (which is the case for old traces, also in published trace analysis data), .txt
 * is assumed.
 * 
 * @author DaGeRe
 *
 */
public class TraceFileManager {

   public static final String TXT_ENDING = ".txt";
   public static final String ZIP_ENDING = ".zip";

   private final File methodDir;
   private final String shortVersion;
   private final TestSelectionConfig config;

   public TraceFileManager(File methodDir, String shortVersion, TestSelectionConfig config) {
      this.methodDir = methodDir;
      this.shortVersion = shortVersion;
      this.config = config;
   }

   public File getWholeTraceFile() {
      return getTraceFile("");
   }

   public File getNocommentTraceFile() {
      return getTraceFile(OneTraceGenerator.NOCOMMENT);
   }
   
   public File getMethodTraceFile() {
      return getTraceFile(OneTraceGenerator.METHOD);
   }
   
   public File getMethodExpandedTraceFile() {
      return getTraceFile(OneTraceGenerator.METHOD_EXPANDED);
   }

   private File getTraceFile(String variant) {
      File zipCandidate = new File(methodDir, shortVersion + variant + ZIP_ENDING);
      if (zipCandidate.exists()) {
         return zipCandidate;
      }
      File txtCandidate = new File(methodDir, shortVersion + variant + TXT_ENDING);
      if (txtCandidate.exists()) {
         return txtCandidate;
      }
      File noEndingCandidate = new File(methodDir, shortVersion + variant);
      if (noEndingCandidate.exists()) {
         return noEndingCandidate;
      }
      if (config.isWriteAsZip()) {
         return zipCandidate;
      } else {
         return txtCandidate;
      }
   }
}
