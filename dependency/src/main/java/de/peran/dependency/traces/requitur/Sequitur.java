package de.peran.dependency.traces.requitur;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peran.dependency.analysis.data.TraceElement;
import de.peran.dependency.traces.requitur.content.Content;
import de.peran.dependency.traces.requitur.content.StringContent;
import de.peran.dependency.traces.requitur.content.TraceElementContent;

public class Sequitur {

	private static final Logger LOG = LogManager.getLogger(Sequitur.class);

	Map<Digram, Digram> digrams = new HashMap<>();
	Map<String, Rule> rules = new HashMap<>();
	List<Rule> ununsedRules = new LinkedList<>();
	private Symbol startSymbol = new Symbol(this, (StringContent) null);
	Symbol lastSymbol = startSymbol;
	private int ruleindex = 0;

	Digram link(final Symbol start, final Symbol end) {
		start.setSucessor(end);
		end.setPredecessor(start);
		if (start.getValue() != null && end.getValue() != null) {
			final Digram newDigram = new Digram(start, end);
			handleDigram(newDigram);
			return newDigram;
		} else {
			return null;
		}
	}

	public void addElement(final Symbol symbol) {
		if (startSymbol == null) {
			startSymbol = symbol;
			lastSymbol = symbol;
		} else {
			lastSymbol.setSucessor(symbol);
			symbol.setPredecessor(lastSymbol);
			lastSymbol = symbol;
			if (symbol.getPredecessor().getValue() != null) {
				final Digram digram = new Digram(symbol.getPredecessor(), symbol);
				handleDigram(digram);
			}
		}
//		TraceStateTester.assureCorrectState(this);
	}

	void handleDigram(final Digram digram) {
		final Digram existing = digrams.get(digram);
		if (existing != null) {
			if (digram.getStart() != existing.getEnd()) {
				if (existing.rule != null) {
					existing.rule.use(digram);
				} else {
					Rule rule;
					if (ununsedRules.size() > 0) {
						rule = ununsedRules.remove(0);
						rule.setDigram(existing);
					} else {
						rule = new Rule(this, ruleindex, existing);
						ruleindex++;
					}
					rules.put(rule.getName(), rule);

					rule.use(digram);
				}
			}
		} else {
			digrams.put(digram, digram);
		}
	}

	public List<Content> getTrace() {
		Symbol iterator = startSymbol.getSucessor();
		final List<Content> trace = new LinkedList<>();
		while (iterator != null) {
			trace.add(iterator.getValue());
			iterator = iterator.getSucessor();
		}
		return trace;
	}

	public List<Content> getUncompressedTrace() {
		Symbol iterator = startSymbol.getSucessor();
		final List<Content> trace = new LinkedList<>();
		while (iterator != null) {
			for (int i = 0; i < iterator.getOccurences(); i++) {
				trace.add(iterator.getValue());
			}
			iterator = iterator.getSucessor();
		}
		return trace;
	}

	public Map<String, Rule> getRules() {
		return rules;
	}

	@Override
	public String toString() {
		return getTrace().toString();
	}

	List<Content> addingElements;

	public void addElements(final List<String> mytrace) {
		addingElements = new LinkedList<>();
		for (final String element : mytrace) {
			addingElements.add(new StringContent(element));
			final Symbol symbol = new Symbol(this, new StringContent(element));
			addElement(symbol);
		}
	}

	public void addTraceElements(final List<TraceElement> calls2) {
		addingElements = new LinkedList<>();
		for (final TraceElement element : calls2) {
			final TraceElementContent content = new TraceElementContent(element);
			addingElements.add(content);
			final Symbol symbol = new Symbol(this, content);
			addElement(symbol);
		}
	}

	public Symbol getStartSymbol() {
		return startSymbol;
	}

}
