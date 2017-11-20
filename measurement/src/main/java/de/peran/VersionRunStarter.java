package de.peran;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;

import de.peran.dependencyprocessors.VersionProcessor;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.utils.StreamGobbler;
import de.peran.vcs.GitUtils;

/**
 * Starts running every version just for downloading the dependencies. After all dependencies have been downloaded, most operations can be run locally (as long as the version control system can
 * operate locally).
 * 
 * @author reichelt
 *
 */
public class VersionRunStarter extends VersionProcessor {

	VersionRunStarter(final String[] args) throws ParseException, JAXBException {
		super(args);
	}

	@Override
	protected void processVersion(final Version version) {
		GitUtils.goToTag(version.getVersion(), projectFolder);
		try {
			final Process p = Runtime.getRuntime().exec("mvn clean package -DskipTests=true", null, projectFolder);
			StreamGobbler.showFullProcess(p);
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(final String[] args) throws ParseException, JAXBException {
		final VersionRunStarter vr = new VersionRunStarter(args);
		vr.processCommandline();
	}
}
