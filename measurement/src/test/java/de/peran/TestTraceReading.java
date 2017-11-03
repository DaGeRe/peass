package de.peran;

/*-
 * #%L
 * peran-measurement
 * %%
 * Copyright (C) 2015 - 2017 DaGeRe
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */


import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.github.javaparser.ParseException;

import de.peran.dependency.analysis.data.TraceElement;
import de.peran.dependency.traces.TraceMethodReader;
import de.peran.dependency.traces.TraceWithMethods;

/**
 * Tests whether trace reading for the purpose of experiment result analysis is working corretly.
 * 
 * In order to execute this test, first call de.peran.example.Caller with kieker-aspectj-instrumentation (normaly something like
 * -javaagent:~/.m2/repository/net/kieker-monitoring/kieker/1.12/kieker-1.12-aspectj.jar ) and afterwards copy the results to src/test/resources/kieker-example. In the repo, an recent version of the
 * traces should be contained.
 * 
 * @author reichelt
 *
 */
public class TestTraceReading {

	private static final Logger LOG = LogManager.getLogger(TestTraceReading.class);

	@Test
	public void testSimpleTraceReading() throws ParseException, IOException {
		final TraceMethodReader tmr = new TraceMethodReader(new File("src/test/resources/kieker-example/simple/"));
		// The path to the kieker-trace-data (first argument) and the path to the classes must match
		final TraceWithMethods trace = tmr.getTraceWithMethods(new File("src/test/java"));
		LOG.info("Trace length: " + trace.getLength());
		for (int i = 0; i < trace.getLength(); i++) {
			LOG.info(trace.getTraceElement(i));
			LOG.info(trace.getMethod(i));
		}

		Assert.assertNull(trace.getMethod(1));

		Assert.assertEquals("main", trace.getTraceElement(0).getMethod());
		Assert.assertEquals("main", trace.getTraceElement(5).getMethod());
		Assert.assertEquals("main", trace.getTraceElement(6).getMethod());

		Assert.assertEquals(trace.getMethod(0).substring(0, trace.getMethod(0).indexOf("\n")), "public static void main(final String[] args) {");
		Assert.assertEquals(trace.getMethod(5).substring(0, trace.getMethod(5).indexOf("\n")), "private void main(final int z) {");
		Assert.assertEquals(trace.getMethod(6).substring(0, trace.getMethod(6).indexOf("\n")), "private void main(final String z) {");
	}

	@Test
	public void testLongTraceReading() throws ParseException, IOException {
		final TraceMethodReader tmr = new TraceMethodReader(new File("src/test/resources/kieker-example/long/"));
		// The path to the kieker-trace-data (first argument) and the path to the classes must match
		final TraceWithMethods trace = tmr.getTraceWithMethods(new File("src/test/java"));
		LOG.info("Trace length: " + trace.getLength());
		for (int i = 0; i < trace.getLength(); i++) {
			LOG.info(trace.getTraceElement(i));
			LOG.info(trace.getMethod(i));
		}

		Assert.assertNull(trace.getMethod(1));

		Assert.assertEquals("main", trace.getTraceElement(0).getMethod());
		Assert.assertEquals("callMe", trace.getTraceElement(2).getMethod());
		Assert.assertEquals("callMe4", trace.getTraceElement(5).getMethod());
		Assert.assertEquals("10 repetitions of 3 calls", trace.getTraceElement(6).getMethod());

		Assert.assertEquals("public static void main(final String[] args) {", trace.getMethod(0).substring(0, trace.getMethod(0).indexOf("\n")));
		Assert.assertEquals("public void callMe() {", trace.getMethod(2).substring(0, trace.getMethod(2).indexOf("\n")) );
		Assert.assertEquals("private int callMe4() {", trace.getMethod(5).substring(0, trace.getMethod(5).indexOf("\n")) );
	}
	
	@Test
	public void testOneMethodLoop() throws ParseException, IOException{
		final List<TraceElement> calls = new LinkedList<>();
		calls.add(new TraceElement("de.peran.example.CalleeSimpleFor", "methodA", 0));
		calls.add(new TraceElement("de.peran.example.CalleeSimpleFor", "methodB", 0));
		calls.add(new TraceElement("de.peran.example.CalleeSimpleFor", "methodB", 0));
		calls.add(new TraceElement("de.peran.example.CalleeSimpleFor", "methodB", 0));
		calls.add(new TraceElement("de.peran.example.CalleeSimpleFor", "methodC", 0));
		calls.add(new TraceElement("de.peran.example.CalleeSimpleFor", "methodC", 0));
		calls.add(new TraceElement("de.peran.example.CalleeSimpleFor", "methodC", 0));
		
		final TraceMethodReader reader = new TraceMethodReader(calls);
		final TraceWithMethods trace = reader.getTraceWithMethods(new File("src/test/java/"));
		
		Assert.assertEquals("methodA", trace.getTraceElement(0).getMethod());
		Assert.assertEquals("methodB", trace.getTraceElement(1).getMethod());
		Assert.assertEquals("3 repetitions of 1 calls", trace.getTraceElement(2).getMethod());
		Assert.assertEquals("methodC", trace.getTraceElement(3).getMethod());
		Assert.assertEquals("3 repetitions of 1 calls", trace.getTraceElement(4).getMethod());
	}
	
	@Test
	public void testTwoMethodLoop() throws ParseException, IOException{
		final List<TraceElement> calls = new LinkedList<>();
		calls.add(new TraceElement("de.peran.example.CalleeSimpleFor", "methodA", 0));
		calls.add(new TraceElement("de.peran.example.CalleeSimpleFor", "methodB", 0));
		calls.add(new TraceElement("de.peran.example.CalleeSimpleFor", "methodC", 0));
		calls.add(new TraceElement("de.peran.example.CalleeSimpleFor", "methodB", 0));
		calls.add(new TraceElement("de.peran.example.CalleeSimpleFor", "methodC", 0));
		calls.add(new TraceElement("de.peran.example.CalleeSimpleFor", "methodB", 0));
		calls.add(new TraceElement("de.peran.example.CalleeSimpleFor", "methodC", 0));
		
		final TraceMethodReader reader = new TraceMethodReader(calls);
		final TraceWithMethods trace = reader.getTraceWithMethods(new File("src/test/java/"));
		
		Assert.assertEquals("methodA", trace.getTraceElement(0).getMethod());
		Assert.assertEquals("methodB", trace.getTraceElement(1).getMethod());
		Assert.assertEquals("methodC", trace.getTraceElement(2).getMethod());
		Assert.assertEquals("3 repetitions of 2 calls", trace.getTraceElement(3).getMethod());
	}
}
