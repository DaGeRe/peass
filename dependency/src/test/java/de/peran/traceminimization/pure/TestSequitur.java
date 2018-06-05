package de.peran.traceminimization.pure;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import de.peran.dependency.traces.requitur.Sequitur;
import de.peran.dependency.traces.requitur.TraceStateTester;
import de.peran.dependency.traces.requitur.content.Content;
import de.peran.dependency.traces.requitur.content.RuleContent;
import de.peran.dependency.traces.requitur.content.StringContent;
/**
 * Tests only sequitur on artificial examples given by manually constructed traces.
 * @author reichelt
 *
 */
public class TestSequitur {

	public static List<String> contentToStringTrace(final List<Content> expandedTrace) {
		return expandedTrace.stream().map(value -> ((StringContent) value).getValue()).collect(Collectors.toList());
	}

	@Test
	public void testBasic() {
		final Sequitur seg = new Sequitur();
		final List<String> mytrace = new LinkedList<>();
		for (int i = 0; i < 2; i++) {
			mytrace.add("A");
			mytrace.add("B");
		}
		seg.addElements(mytrace);

		final List<Content> trace = seg.getUncompressedTrace();
		System.out.println(trace);
		Assert.assertEquals(2, trace.size());
		Assert.assertEquals("#0", ((RuleContent) trace.get(0)).getValue());
		Assert.assertEquals("#0", ((RuleContent) trace.get(1)).getValue());
		final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seg.getRules());
		Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
	}
	
	@Test
   public void testOverlappingPredecessor() {
      final Sequitur seg = new Sequitur();
      final List<String> mytrace = new LinkedList<>();
      for (final String c : new String[] {"f","e","f","e","f","f", "f","e","f","g","h","c","d","f","e","f","f", "x"}) {
         mytrace.add(c);
      }
      seg.addElements(mytrace);

      final List<Content> trace = seg.getUncompressedTrace();
      System.out.println(trace);
      Assert.assertEquals(9, trace.size());
      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seg.getRules());
      Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
   }
	
	@Test
   public void testOverlappingSuccessor() {
      final Sequitur seg = new Sequitur();
      final List<String> mytrace = new LinkedList<>();
      for (final String c : new String[] {"D","E","G","K","I","J","I","J","I","J","X","M","L","N","O","P","T","Q","R","S","R","S","R","S","U","V","W","V","X","M","L","N","O","P","T","Q","R","S"}) {
         mytrace.add(c);
      }
      seg.addElements(mytrace);

      final List<Content> trace = seg.getUncompressedTrace();
      System.out.println(trace);
      Assert.assertEquals(15, trace.size());
      final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seg.getRules());
      Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
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

		final List<Content> trace = seg.getUncompressedTrace();
		System.out.println(trace);
		Assert.assertEquals(8, trace.size());
		final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seg.getRules());
		Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
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

		final List<Content> trace = seg.getUncompressedTrace();
		System.out.println(trace);
		Assert.assertEquals(2, trace.size());
		Assert.assertEquals("#1", ((RuleContent) trace.get(0)).getValue());
		Assert.assertEquals("#1", ((RuleContent) trace.get(1)).getValue());
		final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seg.getRules());
		Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
	}

	@Test
	public void testQuad() {
		final Sequitur seg = new Sequitur();
		final List<String> mytrace = new LinkedList<>();
		for (int i = 0; i < 2; i++) {
			mytrace.add("A");
			mytrace.add("B");
			mytrace.add("C");
			mytrace.add("D");
		}
		seg.addElements(mytrace);

		final List<Content> trace = seg.getUncompressedTrace();
		System.out.println(trace);
		System.out.println(seg.getRules());
		Assert.assertEquals(2, trace.size());
		Assert.assertEquals(1, seg.getRules().size());
		final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seg.getRules());
		Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
	}

	@Test
	public void test6() {
		final Sequitur seg = new Sequitur();
		final List<String> mytrace = new LinkedList<>();
		for (int i = 0; i < 2; i++) {
			mytrace.add("A");
			mytrace.add("B");
			mytrace.add("C");
			mytrace.add("D");
			mytrace.add("E");
			mytrace.add("F");
		}
		seg.addElements(mytrace);

		final List<Content> trace = seg.getUncompressedTrace();
		System.out.println(trace);
		System.out.println(seg.getRules());
		Assert.assertEquals(2, trace.size());
		Assert.assertEquals(1, seg.getRules().size());
		final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seg.getRules());
		Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
	}

	@Test
	public void testNested() {
		final Sequitur seg = new Sequitur();
		final List<String> mytrace = new LinkedList<>();
		for (int j = 0; j < 3; j++) {
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
		mytrace.add("H");
		seg.addElements(mytrace);

		final List<Content> trace = seg.getUncompressedTrace();
		System.out.println(trace);
		System.out.println(seg.getRules());
		Assert.assertEquals(5, trace.size());
		Assert.assertEquals(3, seg.getRules().size());
		final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seg.getRules());
		Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
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

		final List<Content> trace = seg.getUncompressedTrace();
		System.out.println(trace);
		System.out.println(seg.getRules());
		final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seg.getRules());
		Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
	}

	@Test
	public void testMultipleNest() {

		final List<String> mytrace = new LinkedList<>();
		for (int k = 0; k < 2; k++) {
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
			// mytrace.add("B");
			// mytrace.add("H");
		}
		final Sequitur seg = new Sequitur();
		seg.addElements(mytrace);

		final List<Content> trace = seg.getUncompressedTrace();
		System.out.println(trace);
		System.out.println(seg.getRules());
		// Assert.assertEquals(10, trace.size());
		Assert.assertEquals(5, seg.getRules().size());
		final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seg.getRules());
		Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
	}

	@Test
	public void testMoreRuleUsage() {
		final List<String> mytrace = new LinkedList<>();
		final Sequitur seg = new Sequitur();
		for (int i = 0; i < 3; i++) {
			mytrace.add("A");
			mytrace.add("B");
		}
		seg.addElements(mytrace);

		final List<Content> trace = seg.getUncompressedTrace();
		System.out.println(trace);
		System.out.println(seg.getRules());
		Assert.assertEquals(3, trace.size());
		final List<Content> expandedTrace = TraceStateTester.expandContentTrace(trace, seg.getRules());
		Assert.assertEquals(mytrace, contentToStringTrace(expandedTrace));
	}
}
