package de.peran.dependency.traces.requitur;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

import de.peran.dependency.traces.requitur.content.Content;
import de.peran.dependency.traces.requitur.content.RuleContent;

public class TraceStateTester {

	private static final Logger LOG = LogManager.getLogger(TraceStateTester.class);

	public static void assureCorrectState(final Sequitur sequitur) {
		testTrace(sequitur);

		System.out.println(sequitur.addingElements);

		testDigrams(sequitur);
		testRules(sequitur);
		testRuleUsage(sequitur);
	}

	private static void testRuleUsage(final Sequitur sequitur) {
		for (final Content traceelement : sequitur.getUncompressedTrace()) {
			if (traceelement instanceof RuleContent && !sequitur.rules.containsKey(((RuleContent) traceelement).getValue())) {
				throw new RuntimeException("Rule missing!");
			}
		}

		for (final Rule r : sequitur.getRules().values()) {
			if (r.getElements().size() == 2) {
				final Digram keyDigram = new Digram(new Symbol(sequitur, r.getElements().get(0).getValue()), new Symbol(sequitur, r.getElements().get(1).getValue()));
				final Digram test = sequitur.digrams.get(keyDigram);
				if (test == null) {
					LOG.error(keyDigram);
				}
				if (test.rule == null || test.rule != r) {
					throw new RuntimeException(r.getElements() + " should have  rule " + r + " but has " + test.rule);
				}
			}
		}
	}

	public static void testTrace(final Sequitur sequitur) {
		LOG.debug(sequitur.getUncompressedTrace());
		LOG.debug(sequitur.getRules());
		final List<Content> expandedTrace = expandContentTrace(sequitur.getUncompressedTrace(), sequitur.getRules());
		final List<Content> fullTrace = sequitur.addingElements.subList(0, expandedTrace.size());
		Assert.assertEquals(fullTrace, expandedTrace);
	}

	private static void testRules(final Sequitur sequitur) {
		for (final Rule r : sequitur.rules.values()) {
			// System.out.println(r);
			int usages = 0;
			for (final Content traceElement : sequitur.getUncompressedTrace()) {
				if (traceElement instanceof RuleContent && (((RuleContent) traceElement).getValue().equals(r.getName()))) {
					usages++;
				}
			}
			for (final Rule other : sequitur.rules.values()) {
				for (final ReducedTraceElement otherElement : other.getElements()) {
					if (otherElement.getValue() instanceof RuleContent && ((RuleContent) otherElement.getValue()).getValue().equals(r.getName())) {
						usages++;
					}
				}
			}
			if (usages < 2) {
				throw new RuntimeException("Rule " + r.getName() + " underused: " + usages);
			}
		}
	}

	private static void testDigrams(final Sequitur sequitur) {
		final Set<Digram> currentDigrams = new HashSet<>();
		currentDigrams.addAll(sequitur.digrams.values());
		Content before = null;
		for (final Content trace : sequitur.getUncompressedTrace()) {
			if (before != null) {
				final Digram di = new Digram(new Symbol(sequitur, before), new Symbol(sequitur, trace));
				currentDigrams.remove(di);
			}
			before = trace;
		}

		for (final Rule r : sequitur.rules.values()) {
			before = null;
			if (r.getElements().size() == 1) {
				throw new RuntimeException("Rule consists of only one symbol: " + r);
			}
			for (final ReducedTraceElement trace : r.getElements()) {
				if (before != null) {
					final Digram di = new Digram(new Symbol(sequitur, before), new Symbol(sequitur, trace.getValue()));
					currentDigrams.remove(di);
				}
				before = trace.getValue();
			}
		}

		if (currentDigrams.size() > 0) {
			System.out.println(sequitur.getUncompressedTrace());
			System.out.println(sequitur.rules);
			throw new RuntimeException("Digram not existing but listed: " + currentDigrams);
		}
	}

	public static List<Content> expandReadableTrace(final List<ReducedTraceElement> trace, final Map<String, Rule> rules) {
		final List<Content> result = new LinkedList<>();
		for (final ReducedTraceElement element : trace) {
			for (int i = 0; i < element.getOccurences(); i++) {
				if (element.getValue() instanceof RuleContent) {
					final String value = ((RuleContent) element.getValue()).getValue();
					final Rule rule = rules.get(value);
					result.addAll(expandReadableTrace(rule.getElements(), rules));
				} else {
					result.add(element.getValue());
				}
			}
		}
		return result;
	}
	
	public static List<Content> expandContentTrace(final List<Content> trace, final Map<String, Rule> rules) {
		final List<Content> result = new LinkedList<>();
		for (final Content element : trace) {
			if (element instanceof RuleContent) {
				final String value = ((RuleContent) element).getValue();
				final Rule rule = rules.get(value);
				result.addAll(expandTrace(rule.getElements(), rules));
			} else {
				result.add(element);
			}
		}
		return result;
	}

	public static List<Content> expandTrace(final List<ReducedTraceElement> trace, final Map<String, Rule> rules) {
		final List<Content> result = new LinkedList<>();
		for (final ReducedTraceElement element : trace) {
			for (int i = 0; i < element.getOccurences(); i++){
				if (element.getValue() instanceof RuleContent) {
					final String value = ((RuleContent) element.getValue()).getValue();
					final Rule rule = rules.get(value);
					result.addAll(expandTrace(rule.getElements(), rules));
				} else {
					result.add(element.getValue());
				}
			}
		}
		return result;
	}
}
