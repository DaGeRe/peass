package de.peran;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.Assert;
import org.junit.Test;

import com.github.javaparser.ParseException;

import de.peran.dependency.analysis.CalledMethodLoader;
import de.peran.measurement.traces.TraceMethodReader;
import de.peran.measurement.traces.TraceWithMethods;
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

	@Test
	public void testTraceLengthSimpleFor() throws ParseException, IOException {
		final File tmpFolder = new File("target/kieker_results_test/");
		tmpFolder.mkdirs();
		final File[] kiekerFoldersOld = tmpFolder.listFiles((FileFilter) new WildcardFileFilter("kieker*"));
		if (kiekerFoldersOld != null) {
			for (final File kiekerFolderOld : kiekerFoldersOld) {
				kiekerFolderOld.delete();
			}
		}

		final String repo = System.getenv("HOME") + "/.m2/repository";
		final ProcessBuilder builder = new ProcessBuilder("java", "-javaagent:" + repo + "/net/kieker-monitoring/kieker/1.12/kieker-1.12-aspectj.jar", "-cp",
				repo + "/de/dagere/kopeme/kopeme-core/0.11/kopeme-core-0.11.jar:target/test-classes/",
				"de.peran.example.CallerSimpleFor");
		final Process process = builder.start();

		StreamGobbler.showFullProcess(process);

		final File[] kiekerFolders = tmpFolder.listFiles((FileFilter) new WildcardFileFilter("kieker-*"));

		final File traceFolder = kiekerFolders[0];
//
		final TraceMethodReader reader = new TraceMethodReader(new CalledMethodLoader(traceFolder).getShortTrace( ""));
		final TraceWithMethods trace = reader.getTraceWithMethods(new File("src/test/java"));

		System.out.println(trace.getWholeTrace());

		/*
		 * Trace: 20 Methodenaufrufe -> 10 sonstige -> 10 mal Schleife callMe2 -> müsste auf 2 Einträge (callMe2, *10 Aufrufe) zusammenfasst werden -> Ziel: 12 Aufrufe
		 */
		Assert.assertEquals(12, trace.getLength());
	}
	
//	public void 

	// @Test
	// public void testTraceLengthLongFor() throws ParseException, IOException {
	// final ProcessBuilder builder = new ProcessBuilder("java", "-javaagent:/home/reichelt/.m2/repository/net/kieker-monitoring/kieker/1.12/kieker-1.12-aspectj.jar", "-cp", "target/test-classes/",
	// "de.peran.example.CallerLongFor");
	// final Process process = builder.start();
	//
	// StreamGobbler.showFullProcess(process);
	//
	// final File tmpFolder = new File("/tmp/");
	// final File[] kiekerFolders = tmpFolder.listFiles((FileFilter) new WildcardFileFilter("kieker-*peran-test"));
	//
	// final File traceFolder = kiekerFolders[0];
	//
	// final TraceMethodReader reader = new TraceMethodReader();
	// final TraceWithMethods trace = reader.getTraceWithMethods(traceFolder, "", new File("src/test/java"));
	//
	// System.out.println(trace.getWholeTrace());
	//
	// /*
	// * Trace: 20 Methodenaufrufe
	// * -> 10 sonstige
	// * -> 10 mal Schleife callMe2 -> müsste auf 2 Einträge (callMe2, *10 Aufrufe) zusammenfasst werden
	// * -> Ziel: 12 Aufrufe
	// */
	// Assert.assertEquals(12, trace.getLength());
	// }
}
