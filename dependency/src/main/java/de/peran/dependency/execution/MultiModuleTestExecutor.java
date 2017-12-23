package de.peran.dependency.execution;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peran.utils.StreamGobbler;

/**
 * Executes maven-multi-module projects by only executing one module
 * 
 * @author reichelt
 *
 */
public class MultiModuleTestExecutor extends MavenKiekerTestExecutor {

	private static final Logger LOG = LogManager.getLogger(MultiModuleTestExecutor.class);

	public MultiModuleTestExecutor(final File projectFolder, final File moduleFolder, final File resultsFolder) {
		super(projectFolder, moduleFolder, resultsFolder, true);
	}

	@Override
	public boolean isVersionRunning() {
		final File rootPom = new File(projectFolder, "pom.xml");
		final File potentialPom = new File(moduleFolder, "pom.xml");
		final File testFolder = new File(moduleFolder, "src/test");
		LOG.debug(potentialPom);
		boolean isRunning = false;
		if (potentialPom.exists() && rootPom.exists()) {
			if (testFolder.exists()) {
				isRunning = testVersion(potentialPom) && testVersion(rootPom);
				if (isRunning) {
					LOG.debug("pom.xml existing");
					isRunning = testRunning();
					if (isRunning) {
						jdk_version = 8;
					} // TODO If multi-module-java-6 should work, it needs to be
						// implemented here
				}
			}
		}
		return isRunning;
	}

	@Override
	protected boolean compileVersion(final File logFile) {
		return compileVersion(logFile, "mvn",
				"clean",
				"install",
				"-DskipITs",
				"-DskipTests",
				"--am",
				"-Dmaven.test.skip.exec",
				"--pl", moduleFolder.getName(),
				"-Drat.skip=true",
				"-Dlicense.skip=true",
				"-Dpmd.skip=true");
	}

	@Override
	protected boolean testRunning() {
		boolean isRunning;
		final ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "test-compile", "-Drat.skip=true", "--am", "-Dmaven.test.skip.exec", "--pl", moduleFolder.getName());
		pb.directory(projectFolder);
		try {
			LOG.debug(pb.command());
			final Process process = pb.start();
			final String result = StreamGobbler.getFullProcess(process, true, 20000);
			System.out.println(result);
			process.waitFor();
			final int returncode = process.exitValue();
			if (returncode != 0) {
				isRunning = false;
			} else {
				isRunning = true;
			}
		} catch (final IOException | InterruptedException e) {
			e.printStackTrace();
			isRunning = false;
		}
		return isRunning;
	}

	@Override
	public void preparePom() {
		preparePom(false);
	}

}
