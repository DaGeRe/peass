package net.kieker.sourceinstrumentation.it;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import de.dagere.peass.TestConstants;
import de.dagere.peass.utils.StreamGobbler;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;
import net.kieker.sourceinstrumentation.SourceInstrumentationTestUtil;
import net.kieker.sourceinstrumentation.instrument.InstrumentKiekerSource;

public class SimpleProjectUtil {
   public static File obtainLogs(final String projectFolder) throws IOException {
      SourceInstrumentationTestUtil.initSimpleProject(projectFolder);

      File tempFolder = new File(TestConstants.CURRENT_FOLDER, "results");
      tempFolder.mkdir();

      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(AllowedKiekerRecord.OPERATIONEXECUTION);
      instrumenter.instrumentProject(TestConstants.CURRENT_FOLDER);

      final ProcessBuilder pb = new ProcessBuilder("mvn", "test", 
            "-Djava.io.tmpdir=" + tempFolder.getAbsolutePath());
      pb.directory(TestConstants.CURRENT_FOLDER);

      Process process = pb.start();
      StreamGobbler.showFullProcess(process);
      
      File resultFolder = tempFolder.listFiles()[0];
      File resultFile = resultFolder.listFiles((FileFilter) new WildcardFileFilter("*.dat"))[0];
      return resultFile;
   }
}
