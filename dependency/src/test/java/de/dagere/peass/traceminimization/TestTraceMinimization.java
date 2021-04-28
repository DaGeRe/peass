package de.dagere.peass.traceminimization;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.github.javaparser.ParseException;

import de.dagere.peass.dependency.analysis.data.TraceElement;
import de.dagere.peass.dependency.traces.TraceMethodReader;
import de.dagere.peass.dependency.traces.TraceWithMethods;

public class TestTraceMinimization {

	@Test
	public void testStartMethodLoop() throws ParseException, IOException {
		final List<TraceElement> calls = new LinkedList<>();
		calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
		calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
		calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
		calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
		calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
		calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));

		final TraceMethodReader reader = new TraceMethodReader(calls, new File("src/test/java/"));
		final TraceWithMethods trace = reader.getTraceWithMethods();

		Assert.assertEquals("CalleeSimpleFor#methodB", trace.getTraceElement(0).toString());
		Assert.assertEquals(3,trace.getTraceOccurences(0));
		Assert.assertEquals("CalleeSimpleFor#methodC", trace.getTraceElement(1).toString());
		Assert.assertEquals(3,trace.getTraceOccurences(1));
	}

	@Test
	public void testOneMethodLoop() throws ParseException, IOException {
		final List<TraceElement> calls = new LinkedList<>();
		calls.add(new TraceElement("CalleeSimpleFor", "methodA", 0));
		calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
		calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
		calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
		calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
		calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
		calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));

		final TraceMethodReader reader = new TraceMethodReader(calls, new File("src/test/java/"));
		final TraceWithMethods trace = reader.getTraceWithMethods();
		
		System.out.println("Trace");
		System.out.println(trace);
		
		Assert.assertEquals("CalleeSimpleFor#methodA", trace.getTraceElement(0).toString());
		Assert.assertEquals("CalleeSimpleFor#methodB", trace.getTraceElement(1).toString());
		Assert.assertEquals(3, trace.getTraceOccurences(1));
		Assert.assertEquals("CalleeSimpleFor#methodC", trace.getTraceElement(2).toString());
		Assert.assertEquals(3, trace.getTraceOccurences(1));
//		Assert.assertEquals("3 repetitions of 1 calls", trace.getTraceElement(4).getMethod());
	}

	@Test
	public void testTwoMethodLoop() throws ParseException, IOException {
		final List<TraceElement> calls = new LinkedList<>();
		calls.add(new TraceElement("CalleeSimpleFor", "methodA", 0));
		calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
		calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
		calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
		calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
		calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
		calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));

		final TraceMethodReader reader = new TraceMethodReader(calls, new File("src/test/java/"));
		final TraceWithMethods trace = reader.getTraceWithMethods();

		System.out.println();
		System.out.println("Trace");
		System.out.println(trace);

		Assert.assertEquals("CalleeSimpleFor#methodA", trace.getTraceElement(0).toString());
		Assert.assertEquals(" (2)", trace.getTraceElement(1).toString());
//		Assert.assertEquals("#0 (2)", trace.getTraceElement(1).toString());
		Assert.assertEquals("CalleeSimpleFor#methodB", trace.getTraceElement(2).toString());
		Assert.assertEquals("CalleeSimpleFor#methodC", trace.getTraceElement(3).toString());
//		Assert.assertEquals("methodB", trace.getTraceElement(1).getMethod());
//		Assert.assertEquals("methodC", trace.getTraceElement(2).getMethod());
		Assert.assertEquals(3, trace.getTraceOccurences(1));
	}

	@Test
	public void testSimple2NestedLoop() throws ParseException, IOException {
		final List<TraceElement> calls = new LinkedList<>();
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				calls.add(new TraceElement("CalleeSimpleFor", "methodA", 0));
				calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
			}
			calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
		}
		calls.add(new TraceElement("CalleeSimpleFor", "methodD", 0));
		final TraceMethodReader reader = new TraceMethodReader(calls, new File("src/test/java/"));
		final TraceWithMethods trace = reader.getTraceWithMethods();
		System.out.println();
		System.out.println("Trace");
		System.out.println(trace);


		Assert.assertEquals(" (4)", trace.getTraceElement(0).toString());
		Assert.assertEquals(3, trace.getTraceOccurences(0));
		Assert.assertEquals(" (2)", trace.getTraceElement(1).toString());
		Assert.assertEquals(3, trace.getTraceOccurences(1));
		Assert.assertEquals("CalleeSimpleFor#methodA", trace.getTraceElement(2).toString());
		Assert.assertEquals("CalleeSimpleFor#methodB", trace.getTraceElement(3).toString());
		Assert.assertEquals("CalleeSimpleFor#methodC", trace.getTraceElement(4).toString());
		Assert.assertEquals("CalleeSimpleFor#methodD", trace.getTraceElement(5).toString());
	}

	@Test
	public void testSimple3NestedLoop() throws ParseException, IOException {
		final List<TraceElement> calls = new LinkedList<>();
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				calls.add(new TraceElement("CalleeSimpleFor", "methodA", 0));
				calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
				calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
			}
			calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
		}
		final TraceMethodReader reader = new TraceMethodReader(calls, new File("src/test/java/"));
		final TraceWithMethods trace = reader.getTraceWithMethods();
		System.out.println(trace);

//		Assert.assertEquals("#2 (5)", trace.getTraceElement(0).toString());
//		Assert.assertEquals("#1 (3)", trace.getTraceElement(1).toString());
		Assert.assertEquals(" (5)", trace.getTraceElement(0).toString());
      Assert.assertEquals(" (3)", trace.getTraceElement(1).toString());
		Assert.assertEquals("CalleeSimpleFor#methodA", trace.getTraceElement(2).toString());
		Assert.assertEquals("CalleeSimpleFor#methodB", trace.getTraceElement(3).toString());
		Assert.assertEquals("CalleeSimpleFor#methodC", trace.getTraceElement(4).toString());
		Assert.assertEquals("CalleeSimpleFor#methodC", trace.getTraceElement(5).toString());
	}

	@Test
	public void testSimple4NestedLoop() throws ParseException, IOException {
		final List<TraceElement> calls = new LinkedList<>();
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				calls.add(new TraceElement("CalleeSimpleFor", "methodA", 0));
				calls.add(new TraceElement("CalleeSimpleFor", "methodB", 0));
				calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
				calls.add(new TraceElement("CalleeSimpleFor", "methodD", 0));
			}
			calls.add(new TraceElement("CalleeSimpleFor", "methodC", 0));
		}
		final TraceMethodReader reader = new TraceMethodReader(calls, new File("src/test/java/"));
		final TraceWithMethods trace = reader.getTraceWithMethods();
		System.out.println("trace");
		System.out.println(trace);

		Assert.assertEquals(" (6)", trace.getTraceElement(0).toString());
		Assert.assertEquals(3, trace.getTraceOccurences(0));
		Assert.assertEquals(" (4)", trace.getTraceElement(1).toString());
		Assert.assertEquals(3, trace.getTraceOccurences(1));
		Assert.assertEquals("CalleeSimpleFor#methodA", trace.getTraceElement(2).toString());
		Assert.assertEquals("CalleeSimpleFor#methodB", trace.getTraceElement(3).toString());
		Assert.assertEquals("CalleeSimpleFor#methodC", trace.getTraceElement(4).toString());
		Assert.assertEquals("CalleeSimpleFor#methodD", trace.getTraceElement(5).toString());
		Assert.assertEquals("CalleeSimpleFor#methodC", trace.getTraceElement(6).toString());
	}
}
