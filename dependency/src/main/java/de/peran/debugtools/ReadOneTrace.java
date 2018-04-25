package de.peran.debugtools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peran.ViewPrintStarter;
import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependencyprocessors.ViewNotFoundException;

public class ReadOneTrace extends ViewPrintStarter {

   public ReadOneTrace(String[] args) throws ParseException, JAXBException, JsonParseException, JsonMappingException, IOException {
      super(args);
   }

   private static final Logger LOG = LogManager.getLogger(ReadOneTrace.class);

   public static void main(String[] args) {
      final TestCase testcase = new TestCase("org.apache.commons.io.output.CountingOutputStreamTest", "testLargeFiles_IO84");

      try {
         new ReadOneTrace(args).analyse(testcase);
      } catch (IOException | InterruptedException | com.github.javaparser.ParseException | ViewNotFoundException | XmlPullParserException | ParseException | JAXBException e) {
         e.printStackTrace();
      }
   }

   public void analyse(TestCase testcase) throws IOException, InterruptedException, com.github.javaparser.ParseException, ViewNotFoundException, XmlPullParserException {
      final String versionOld = "9561dfcf4949e5b69231e99e38572803632121ae";
      final String version = "68a55d56ae853fc2fdce829a6042163bdc02deb4";

      final File tmpResultFolder = Files.createTempDirectory("tmp").toFile();
      final File diffFolder = new File(tmpResultFolder, "diffs");
      diffFolder.mkdir();

      System.out.println("Folder : " + tmpResultFolder.getAbsolutePath());

      final Map<String, List<File>> traceFileMap = new HashMap<>();

      final boolean tracesWorked = generateTraces(version, testcase, versionOld, tmpResultFolder, traceFileMap);

      if (tracesWorked) {
         LOG.debug("Generating Diff " + testcase.getClazz() + "#" + testcase.getMethod() + " " + versionOld + ".." + version);
         final boolean somethingChanged = generateDiffFiles(testcase, diffFolder, traceFileMap);

         if (somethingChanged) {
            LOG.info("Call should be executed: " + version + " " + testcase);
         }
      }
   }
}
