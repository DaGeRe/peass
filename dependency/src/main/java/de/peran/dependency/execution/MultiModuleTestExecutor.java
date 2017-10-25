package de.peran.dependency.execution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peran.utils.StreamGobbler;

public class MultiModuleTestExecutor extends MavenKiekerTestExecutor {

	private static final Logger LOG = LogManager.getLogger(MultiModuleTestExecutor.class);

	public MultiModuleTestExecutor(File projectFolder, File moduleFolder, File resultsFolder) {
		super(projectFolder, moduleFolder, resultsFolder);
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
				if (isRunning){
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
	
	protected void compileVersion(final File logFile){
		final ProcessBuilder pb = new ProcessBuilder("mvn", 
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
//		final ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "install", "--am", "--pl", moduleFolder.getName(), "-Denforcer.skip", "-Drat.skip", );

		pb.directory(projectFolder);
		if (logFile != null) {
			pb.redirectOutput(Redirect.appendTo(logFile));
			pb.redirectError(Redirect.appendTo(logFile));
		}

		try {
			Process process = pb.start();
			int result = process.waitFor();
			if (result != 0){
				throw new RuntimeException("Compilation failed, see" + logFile.getAbsolutePath());
			}
			
			generateAOPXML();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Compilation failed, see" + logFile.getAbsolutePath());
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new RuntimeException("Compilation failed, see" + logFile.getAbsolutePath());
		}
	}

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

	public void preparePom() {
		final MavenXpp3Reader reader = new MavenXpp3Reader();
		try {
			File pomFile = new File(moduleFolder, "pom.xml");
			final Model model = reader.read(new FileInputStream(pomFile));
			if (model.getBuild() == null) {
				model.setBuild(new Build());
			}
			final Plugin surefire = findPlugin(model, SUREFIRE_ARTIFACTID, ORG_APACHE_MAVEN_PLUGINS);
			
			final Path tempFiles = Files.createTempDirectory("kiekerTemp");
			lastTmpFile = tempFiles.toFile();
			final String argline = KIEKER_ARG_LINE + " -Djava.io.tmpdir=" + tempFiles.toString() + " -Dorg.aspectj.weaver.loadtime.configuration=file:target/test-classes/META-INF/aop.xml";
			extendSurefire(argline, surefire, false);
			extendDependencies(model);

			setJDK(model);

			final MavenXpp3Writer writer = new MavenXpp3Writer();
			writer.write(new FileWriter(pomFile), model);
			
			lastEncoding = getEncoding(model);
		} catch (IOException | XmlPullParserException e) {
			e.printStackTrace();
		}
	}

}
