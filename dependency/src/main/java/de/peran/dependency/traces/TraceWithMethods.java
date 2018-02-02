package de.peran.dependency.traces;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.peran.dependency.traces.requitur.ReducedTraceElement;
import de.peran.dependency.traces.requitur.content.Content;
import de.peran.dependency.traces.requitur.content.RuleContent;
import de.peran.dependency.traces.requitur.content.TraceElementContent;

/**
 * Represents a trace, i.e. a sorted list of all calls of a testcase, and the source code of the calls.
 * 
 * @author reichelt
 *
 */
public class TraceWithMethods {

	private final List<ReducedTraceElement> elements;
	private final Map<TraceElementContent, String> methods = new HashMap<>();

	public TraceWithMethods(final List<ReducedTraceElement> rleTrace) {
		this.elements = rleTrace;
	}

	public void setElementSource(final TraceElementContent traceElement, final String method) {
		methods.put(traceElement, method);
	}

	public Content getTraceElement(final int position) {
		final ReducedTraceElement reducedTraceElement = elements.get(position);
		return reducedTraceElement.getValue();
	}

	public int getTraceOccurences(final int position) {
		final ReducedTraceElement reducedTraceElement = elements.get(position);
		return reducedTraceElement.getOccurences();
	}

	public String getMethod(final int position) {
		final ReducedTraceElement method = elements.get(position);
		return methods.get(method.getValue());
	}

	public int getLength() {
		return elements.size();
	}

	public String getWholeTrace() {
		String result = "";
		List<Integer> currentDepth = new LinkedList<>();
		for (final ReducedTraceElement te : elements) {
			final List<Integer> newDepth = new LinkedList<>();
			String spaceString = "";
			for (final Integer depth : currentDepth) {
				spaceString += " ";
				if (depth > 1) {
					newDepth.add(depth - 1);
				}
			}
			result += spaceString;
			currentDepth = newDepth;
			if (te.getOccurences() != 1) {
				result += te.getOccurences() + " x ";
			}
			result += te.getValue().toString() + "\n";
			if (te.getValue() instanceof RuleContent) {
				final int count = ((RuleContent) te.getValue()).getCount();
				currentDepth.add(count);
			} else if (te.getValue() instanceof TraceElementContent) {
				final TraceElementContent traceContent = (TraceElementContent) te.getValue();
				final String source = methods.get(traceContent);
				if (source != null) {
					result += spaceString + source.replaceAll("\n", "\n" + spaceString) + "\n";
				}
			}
		}
		return result;
	}

	public String getTraceMethods() {
		return toString();
	}

	@Override
	public String toString() {
		String result = "";
		List<Integer> currentDepth = new LinkedList<>();
		for (final ReducedTraceElement te : elements) {
			final List<Integer> newDepth = new LinkedList<>();
			for (final Integer depth : currentDepth) {
				result += " ";
				if (depth > 1) {
					newDepth.add(depth - 1);
				}
			}
			currentDepth = newDepth;
			if (te.getOccurences() != 1) {
				result += te.getOccurences() + " x ";
			}
			result += te.getValue().toString() + "\n";
			if (te.getValue() instanceof RuleContent) {
				final int count = ((RuleContent) te.getValue()).getCount();
				currentDepth.add(count);
			}
		}
		return result;
	}
}
