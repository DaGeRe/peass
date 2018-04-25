package de.peran.traceminimization.pure;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.peran.dependency.traces.requitur.RunLengthEncodingSequitur;
import de.peran.dependency.traces.requitur.Sequitur;
import de.peran.dependency.traces.requitur.TraceStateTester;
import de.peran.dependency.traces.requitur.content.Content;

public class TestRLE {

	@Test
	public void test3AReduction() {
		final Sequitur seg = new Sequitur();
		final List<String> mytrace = new LinkedList<>();
		for (int i = 0; i < 3; i++) {
			mytrace.add("A");
		}
		seg.addElements(mytrace);

		// final List<ReducedTraceElement> trace = new RunLengthEncodingSequitur(seg).getRLETrace();
		// System.out.println(trace);
		// Assert.assertEquals(1, trace.size());
		// Assert.assertEquals(3, trace.get(0).getOccurences());
		final List<Content> unexpandedTrace = seg.getUncompressedTrace();
		System.out.println(unexpandedTrace);
		final List<Content> expandedTrace = TraceStateTester.expandContentTrace(unexpandedTrace, seg.getRules());
		Assert.assertEquals(mytrace, TestSequitur.contentToStringTrace(expandedTrace));
	}

	@Test
	public void testViewExample() {
		final Sequitur seg = new Sequitur();
		final List<String> mytrace = new LinkedList<>();
		mytrace.add("A");
		mytrace.add("B");
		for (int j = 0; j < 5; j++) {
			for (int i = 0; i < 5; i++) {
				mytrace.add("C");
				mytrace.add("D");
			}
			mytrace.add("E");
		}
		mytrace.add("E");
		seg.addElements(mytrace);

		final RunLengthEncodingSequitur runLengthEncodingSequitur = new RunLengthEncodingSequitur(seg);
		runLengthEncodingSequitur.reduce();
		final List<Content> unexpandedTrace = seg.getUncompressedTrace();
		System.out.println(unexpandedTrace);
		System.out.println(seg.getRules());
		final List<Content> expandedTrace = TraceStateTester.expandContentTrace(unexpandedTrace, seg.getRules());
		Assert.assertEquals(mytrace, TestSequitur.contentToStringTrace(expandedTrace));
		
		System.out.println(seg.getTrace());
		System.out.println(seg.getRules());
	}

	@Test
	public void testSimpleTraceReduction() {
		final Sequitur seg = new Sequitur();
		final List<String> mytrace = new LinkedList<>();
		for (int i = 0; i < 2; i++) {
			mytrace.add("A");
			mytrace.add("B");
		}
		seg.addElements(mytrace);

		final RunLengthEncodingSequitur runLengthEncodingSequitur = new RunLengthEncodingSequitur(seg);
		runLengthEncodingSequitur.reduce();
		final List<Content> unexpandedTrace = seg.getUncompressedTrace();
		System.out.println(unexpandedTrace);
		final List<Content> expandedTrace = TraceStateTester.expandContentTrace(unexpandedTrace, seg.getRules());
		Assert.assertEquals(mytrace, TestSequitur.contentToStringTrace(expandedTrace));
	}

	@Test
	public void testTriple() {
		final Sequitur seg = new Sequitur();
		final List<String> mytrace = new LinkedList<>();
		for (int i = 0; i < 2; i++) {
			mytrace.add("A");
			mytrace.add("B");
			mytrace.add("C");
		}
		seg.addElements(mytrace);

		final List<Content> unexpandedTrace = seg.getUncompressedTrace();
		System.out.println(unexpandedTrace);
		final List<Content> expandedTrace = TraceStateTester.expandContentTrace(unexpandedTrace, seg.getRules());
		Assert.assertEquals(mytrace, TestSequitur.contentToStringTrace(expandedTrace));
	}

	@Test
	public void testRuleCompression() {
		final Sequitur seg = new Sequitur();
		final List<String> mytrace = new LinkedList<>();
		for (int i = 0; i < 2; i++) {
			mytrace.add("A");
			mytrace.add("A");
			mytrace.add("C");
		}
		seg.addElements(mytrace);

		new RunLengthEncodingSequitur(seg).reduce();
		final List<Content> unexpandedTrace = seg.getUncompressedTrace();
		System.out.println(unexpandedTrace);
		final List<Content> expandedTrace = TraceStateTester.expandContentTrace(unexpandedTrace, seg.getRules());
		Assert.assertEquals(mytrace, TestSequitur.contentToStringTrace(expandedTrace));
	}

	@Test
	public void testRuleDeletion() {
		final Sequitur seg = new Sequitur();
		final List<String> mytrace = new LinkedList<>();
		for (int j = 0; j < 2; j++) {
			for (int i = 0; i < 2; i++) {
				mytrace.add("A");
				mytrace.add("A");
			}
			mytrace.add("B");
			mytrace.add("B");
			mytrace.add("C");
		}

		seg.addElements(mytrace);

		new RunLengthEncodingSequitur(seg).reduce();
		final List<Content> unexpandedTrace = seg.getUncompressedTrace();
		System.out.println(unexpandedTrace);
		final List<Content> expandedTrace = TraceStateTester.expandContentTrace(unexpandedTrace, seg.getRules());
		Assert.assertEquals(mytrace, TestSequitur.contentToStringTrace(expandedTrace));
	}

	@Test
	public void testRuleOnce() {
		final List<String> mytrace = new LinkedList<>();
		for (int j = 0; j < 2; j++) {
			for (int i = 0; i < 2; i++) {
				mytrace.add("A");
				mytrace.add("B");
				mytrace.add("C");
			}
			mytrace.add("D");
			mytrace.add("E");
			mytrace.add("F");
		}
		mytrace.add("A");
		mytrace.add("B");
		mytrace.add("C");
		final Sequitur seg = new Sequitur();
		seg.addElements(mytrace);

		new RunLengthEncodingSequitur(seg).reduce();
		final List<Content> unexpandedTrace = seg.getUncompressedTrace();
		System.out.println(unexpandedTrace);
		final List<Content> expandedTrace = TraceStateTester.expandContentTrace(unexpandedTrace, seg.getRules());
		Assert.assertEquals(mytrace, TestSequitur.contentToStringTrace(expandedTrace));
	}
}
