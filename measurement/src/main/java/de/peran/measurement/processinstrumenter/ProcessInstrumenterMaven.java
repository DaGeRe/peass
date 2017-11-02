package de.peran.measurement.processinstrumenter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peran.dependency.analysis.data.TestSet;
import de.peran.dependency.execution.MavenKiekerTestExecutor;
import de.peran.testtransformation.JUnitTestTransformer;

/**
 * Instruments a maven project in order to execute its unit tests as performance tests.
 * 
 * @author reichelt
 *
 */
public class ProcessInstrumenterMaven implements ProcessInstrumenter {

	private static final int DEFAULT_TIMEOUT = 30 * 60 * 1000;

	private static final Logger LOG = LogManager.getLogger(ProcessInstrumenterMaven.class);

	private final File projectFolder;
	private final File resultFolder;
	private final JUnitTestTransformer atg;
	private int timeout = DEFAULT_TIMEOUT; // default 30 Minutes timeout

	public ProcessInstrumenterMaven(final JUnitTestTransformer atg, final File resultFolder) {
		this.projectFolder = atg.getProjectFolder();
		this.atg = atg;
		this.resultFolder = resultFolder;
	}

	/**
	 * Sets timeout in milliseconds
	 * 
	 * @param timeout timeout in milliseconds
	 * @return
	 */
	public void setTimeout(final int timeout) {
		this.timeout = timeout;
	}

	public File getResultFolder() {
		return resultFolder;
	}

	public void generateTests(final String additionalArgLine) throws IOException, InterruptedException {
		enhancePom(additionalArgLine);
		atg.transformTests();
	}

	public void enhancePom(final String additionalArgLine) {
		final IOFileFilter pomFilter = new WildcardFileFilter("**pom.xml");
		for (final File file : FileUtils.listFiles(projectFolder, pomFilter, TrueFileFilter.INSTANCE)) {
			LOG.debug("Bearbeite pom: " + file + " " + file.exists());
			Charset encoding = MavenKiekerTestExecutor.rewriteSurefire(file, additionalArgLine);
			atg.setEncoding(encoding);
		}
	}

	public void executeTests(final String name, final File logFile) {
		executeTestsWithAdditionalCommandline(logFile, "-Dtest=" + name);
	}

	/**
	 * Runs the changed tests, i.e. builds -Dtest=Class#methoda+methodb-like strings and calls maven with the given strings. Saves the heap space by not saving the stdout-result.
	 * 
	 * @param instrumenterMaven
	 * @param testsToRun
	 * @return
	 */
	@Override
	public void executeTests(final TestSet testset, final File logFolder) {
		for (final Entry<String, List<String>> entry : testset.entrySet()) {
			final File clazzFolder = new File(logFolder, entry.getKey());
			clazzFolder.mkdir();
			for (final String methodname : entry.getValue()) {
				executeTests(entry.getKey() + "#" + methodname, new File(clazzFolder, "log_" + methodname));
			}
		}
	}

	private final void executeTestsWithAdditionalCommandline(final File logFile, final String... commandLineAddition) {
		LOG.trace("Führe Tests aus");
		try {
			final Process process = buildProcess(logFile, commandLineAddition);

			final Thread currentHook = new Thread(new Runnable() {

				@Override
				public void run() {
					LOG.debug("Prozess wird abgebrochen..");
					process.destroy();
				}
			});
			Runtime.getRuntime().addShutdownHook(currentHook);

			if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
				try {
					final Field f = process.getClass().getDeclaredField("pid");
					f.setAccessible(true);
					final int pid = f.getInt(process);
					LOG.debug("PID: " + pid);
				} catch (final Throwable e) {
				}
			}

			try {
				final boolean result = process.waitFor(timeout, TimeUnit.MILLISECONDS);
				if (!result) {
					savelyKillProcess(process);
				}
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}

			LOG.debug("Ausführung beendet");
			Runtime.getRuntime().removeShutdownHook(currentHook);
			LOG.trace("Shutdown-Hook entfernt");

		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private void savelyKillProcess(final Process process) {
		if (process.isAlive()) {
			for (int i = 0; i < 10 && process.isAlive(); i++) {
				LOG.info("Prozess: {} {}", i, process.isAlive());
				try {
					Thread.sleep(1000);
				} catch (final InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (process.isAlive()) {
				LOG.error("Prozess wurde nicht beendet!");
				process.destroyForcibly();
			}
		}
	}

	private Process buildProcess(final File logFile, final String... commandLineAddition) throws IOException {
		final String[] originals = new String[] { "mvn", "clean", "test", 
				"-fn", 
				"--no-snapshot-updates", 
				"-Dcheckstyle.skip=true",
				"-Dmaven.compiler.source=1.7",
				"-Dmaven.compiler.target=1.7",
				"-Dmaven.javadoc.skip=true",
				"-Denforcer.skip=true",
				"-Drat.skip=true" };
		final String[] vars = new String[commandLineAddition.length + originals.length];
		for (int i = 0; i < originals.length; i++) {
			vars[i] = originals[i];
		}
		for (int i = 0; i < commandLineAddition.length; i++) {
			vars[originals.length + i] = commandLineAddition[i];
		}

		final Process process = executeProcess(logFile, vars, projectFolder);
		return process;
	}

	public Process executeProcess(final File logFile, final String[] vars, final File directory) throws IOException {
		final ProcessBuilder pb = new ProcessBuilder(vars);
		pb.environment().put("KOPEME_HOME", resultFolder.getAbsolutePath());
		if (System.getenv("MAVEN_OPTS") != null) {
			pb.environment().put("MAVEN_OPTS", System.getenv("MAVEN_OPTS"));
		}
		pb.directory(directory);
		if (logFile != null) {
			pb.redirectOutput(logFile);
			pb.redirectError(logFile);
		}

		LOG.debug("Ergebnisordner: {}", resultFolder);
		LOG.debug("Kommando: {} Ordner: {}", Arrays.toString(pb.command().toArray()), directory);

		final Process process = pb.start();
		return process;
	}
}
