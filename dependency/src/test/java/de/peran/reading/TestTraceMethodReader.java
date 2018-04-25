package de.peran.reading;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.github.javaparser.ParseException;

import de.peran.dependency.analysis.CalledMethodLoader;
import de.peran.dependency.traces.TraceMethodReader;
import de.peran.dependency.traces.TraceWithMethods;
import de.peran.utils.StreamGobbler;

/**
 * Testet, ob bei einem Trace, bei dem eine Methode oder eine Folge von Methoden mehrmals aufgerufen wird, das Lesen des Traces korrekt funktioniert.
 * 
 * Davor muss de.peran.example.Caller mit Kieker-Instrumentierung aufgerufen worden sein.
 * 
 * @author reichelt
 *
 */
public class TestTraceMethodReader {

	private final File tmpFolder = new File("target/kieker_results_test/");
	private final String KOPEME_VERSION = "0.8.1";
	private final String REPO = System.getenv("HOME") + "/.m2/repository";
	private final String KOPEME_JAR = REPO + "/de/dagere/kopeme/kopeme-core/" + KOPEME_VERSION + "/kopeme-core-" + KOPEME_VERSION + ".jar";

	@Before
	public void init() {
		tmpFolder.mkdirs();
		final File[] kiekerFoldersOld = tmpFolder.listFiles((FileFilter) new WildcardFileFilter("kieker*"));
		if (kiekerFoldersOld != null) {
			for (final File kiekerFolderOld : kiekerFoldersOld) {
				try {
					FileUtils.deleteDirectory(kiekerFolderOld);
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Test
	public void testTraceLengthSimpleFor() throws ParseException, IOException {
		final ProcessBuilder builder = new ProcessBuilder("java",
				"-javaagent:" + REPO + "/net/kieker-monitoring/kieker/1.12/kieker-1.12-aspectj.jar",
				"-cp", KOPEME_JAR + ":target/test-classes/",
				"de.peran.example.CallerSimpleFor");
		final Process process = builder.start();

		StreamGobbler.showFullProcess(process);

		final File[] kiekerFolders = tmpFolder.listFiles((FileFilter) new WildcardFileFilter("kieker-*"));

		final File traceFolder = kiekerFolders[0];
		//
		final TraceMethodReader reader = new TraceMethodReader(new CalledMethodLoader(traceFolder, tmpFolder).getShortTrace(""), new File("src/test/java"));
		final TraceWithMethods trace = reader.getTraceWithMethods();

		System.out.println(trace.getWholeTrace());

		/*
		 * Trace: 20 Methodenaufrufe -> 10 sonstige -> 10 mal Schleife callMe2 -> müsste auf 1 Eintrag (callMe2, *10 Aufrufe) zusammenfasst werden -> Ziel: 11 Aufrufe
		 */
		Assert.assertEquals(11, trace.getLength());
	}

	@Test
	public void testTraceLengthLongFor() throws ParseException, IOException {
		final ProcessBuilder builder = new ProcessBuilder("java", "-javaagent:" + REPO + "/net/kieker-monitoring/kieker/1.12/kieker-1.12-aspectj.jar", "-cp",
				KOPEME_JAR + ":target/test-classes/",
				"de.peran.example.CallerLongFor");
		final Process process = builder.start();

		StreamGobbler.showFullProcess(process);
		final File[] kiekerFolders = tmpFolder.listFiles((FileFilter) new WildcardFileFilter("kieker-*"));

		final File traceFolder = kiekerFolders[0];

		final TraceMethodReader reader = new TraceMethodReader(new CalledMethodLoader(traceFolder, tmpFolder).getShortTrace(""), new File("src/test/java"));
		final TraceWithMethods trace = reader.getTraceWithMethods();

		System.out.println(trace.getWholeTrace());

		Assert.assertEquals(7, trace.getLength());
	}
}
