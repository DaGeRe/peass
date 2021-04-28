package de.dagere.peass;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.persistence.Version;
import de.peass.dependencyprocessors.VersionProcessor;
import de.peass.utils.StreamGobbler;
import de.peass.vcs.GitUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Starts running every version just for downloading the dependencies. After all dependencies have been downloaded, most operations can be run locally (as long as the version
 * control system can operate locally).
 * 
 * @author reichelt
 *
 */
@Command(description = "Starts running every version just for downloading the dependencies. After all dependencies have been downloaded, most operations can be run locally (as long as the version" + 
      " control system can operate locally).", name = "downloadDependencies")
public class VersionRunStarter extends VersionProcessor {

   public VersionRunStarter() throws ParseException, JAXBException, JsonParseException, JsonMappingException, IOException {
      super();
   }

   @Override
   protected void processVersion(String version, final Version versioninfo) {
      GitUtils.goToTag(version, folders.getProjectFolder());
      try {
         final Process p = Runtime.getRuntime().exec("mvn clean package -DskipTests=true", null, folders.getProjectFolder());
         StreamGobbler.showFullProcess(p);
      } catch (final IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

   }

   public static void main(final String[] args) throws ParseException, JAXBException, JsonParseException, JsonMappingException, IOException {
      VersionRunStarter command = new VersionRunStarter();
      CommandLine commandLine = new CommandLine(command);
      commandLine.execute(args);
      command.processCommandline();
   }
}
