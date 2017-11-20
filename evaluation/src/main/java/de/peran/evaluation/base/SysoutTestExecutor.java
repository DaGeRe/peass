package de.peran.evaluation.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peran.dependency.analysis.data.TestSet;
import de.peran.dependency.execution.TestExecutor;

/**
 * Executes the tests of a maven project in order to read the amount of executed tests in the process sysout
 * 
 * @author reichelt
 *
 */
public class SysoutTestExecutor extends TestExecutor {

	private static final int SECOND = 1000;

	private static final int DEFAULT_TIMEOUT = 5;

	private static final Logger LOG = LogManager.getLogger(SysoutTestExecutor.class);

	public static final String ORG_APACHE_MAVEN_PLUGINS = "org.apache.maven.plugins";
	public static final String SUREFIRE_ARTIFACTID = "maven-surefire-plugin";
	public static final String COMPILER_ARTIFACTID = "maven-compiler-plugin";

	public SysoutTestExecutor(final File projectFolder) {
		super(projectFolder, projectFolder, new File(projectFolder.getParent(), projectFolder.getName() + "_sysout"));
	}

	private Process buildProcess(final File logFile, final String... commandLineAddition) throws IOException {
		final String[] originals = new String[] { "mvn", "clean", "test", "-fn", "-Dcheckstyle.skip=true",
				"-Dmaven.compiler.source=1.7", "-Dmaven.compiler.target=1.7", "-Dmaven.javadoc.skip=true" };
		final String[] vars = new String[commandLineAddition.length + originals.length];
		for (int i = 0; i < originals.length; i++) {
			vars[i] = originals[i];
		}
		for (int i = 0; i < commandLineAddition.length; i++) {
			vars[originals.length + i] = commandLineAddition[i];
		}

		final ProcessBuilder pb = new ProcessBuilder(vars);

		pb.directory(projectFolder);
		if (logFile != null) {
			pb.redirectOutput(Redirect.appendTo(logFile));
			pb.redirectError(Redirect.appendTo(logFile));
		}

		final Process process = pb.start();
		return process;
	}

	@Override
	public void executeAllTests(final File logFile) {
		try {
			final Process process = buildProcess(logFile);
			waitForProcess(process);
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void executeTests(final File logFile, final String testname) {
		try {
			final Process process = buildProcess(logFile, "-Dtest=" + testname);
			waitForProcess(process);
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

	}

	private void waitForProcess(final Process process) throws InterruptedException {
		LOG.info("Starting Process");
		process.waitFor(DEFAULT_TIMEOUT, TimeUnit.MINUTES);
		if (process.isAlive()) {
			process.destroy();
			while (process.isAlive()) {
				process.destroyForcibly();
				Thread.sleep(SECOND);
			}
		}
	}

	public void preparePom() {
		final MavenXpp3Reader reader = new MavenXpp3Reader();
		try {
			final File pomFile = new File(projectFolder, "pom.xml");
			final Model model = reader.read(new FileInputStream(pomFile));
			if (model.getBuild() == null) {
				model.setBuild(new Build());
			}

			final Plugin ekstazi = new Plugin();
			ekstazi.setGroupId("org.ekstazi");
			ekstazi.setArtifactId("ekstazi-maven-plugin");
			ekstazi.setVersion("4.6.3");
			final PluginExecution ekstaziExecution = new PluginExecution();
			ekstaziExecution.setGoals(Arrays.asList(new String[] { "select" }));
			final List<PluginExecution> ekstaziExecutions = new LinkedList<>();
			ekstaziExecutions.add(ekstaziExecution);
			ekstazi.setExecutions(ekstaziExecutions);

			model.getBuild().getPlugins().add(ekstazi);

			final MavenXpp3Writer writer = new MavenXpp3Writer();
			writer.write(new FileWriter(pomFile), model);
		} catch (IOException | XmlPullParserException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void executeTests(TestSet tests, File logFolder) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public boolean isVersionRunning() {
		throw new RuntimeException("Not implemented yet");
	}

}
