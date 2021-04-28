package de.dagere.peass.dependency.traces;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.dagere.peass.dependency.traces.requitur.ReducedTraceElement;
import de.dagere.peass.dependency.traces.requitur.content.Content;
import de.dagere.peass.dependency.traces.requitur.content.RuleContent;
import de.dagere.peass.dependency.traces.requitur.content.TraceElementContent;

/**
 * Represents a trace, i.e. a sorted list of all calls of a testcase, and the source code of the calls.
 * 
 * @author reichelt
 *
 */
public class TraceWithMethods {

	private final List<ReducedTraceElement> elements;
	private final Map<TraceElementContent, String> methods = new HashMap<>();
	private final Map<TraceElementContent, String> methodsWithoutComment = new HashMap<>();

	public TraceWithMethods(final List<ReducedTraceElement> rleTrace) {
		this.elements = rleTrace;
	}

	public void setElementSource(final TraceElementContent traceElement, final String method) {
		methods.put(traceElement, method);
	}

	public void setElementSourceNoComment(final TraceElementContent traceElement, final String method) {
		methodsWithoutComment.put(traceElement, method);
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

	public String getCommentlessTrace() {
		final Map<TraceElementContent, String> currentSource = methodsWithoutComment;
		return getTraceString(currentSource);
	}
	
	public String getWholeTrace() {
		final Map<TraceElementContent, String> currentSource = methods;
		return getTraceString(currentSource);
	}

	private String getTraceString(final Map<TraceElementContent, String> currentSource) {
		final StringBuilder result = new StringBuilder();
		List<Integer> currentDepth = new LinkedList<>();
		for (final ReducedTraceElement te : elements) {
			final List<Integer> newDepth = new LinkedList<>();
			String spaceString = getSpaceString(currentDepth, newDepth);
			result.append(spaceString);
			currentDepth = newDepth;
			if (te.getOccurences() != 1) {
				result.append(te.getOccurences());
				result.append(" x ");
			}
			result.append(te.getValue().toString());
			result.append("\n");
			if (te.getValue() instanceof RuleContent) {
				final int count = ((RuleContent) te.getValue()).getCount();
				currentDepth.add(count);
			} else if (te.getValue() instanceof TraceElementContent && currentSource != null) {
				writeMethodSource(currentSource, result, te, spaceString);
			}
		}
		return result.toString();
	}

   private void writeMethodSource(final Map<TraceElementContent, String> currentSource, final StringBuilder result, final ReducedTraceElement te, String spaceString) {
      final TraceElementContent traceContent = (TraceElementContent) te.getValue();
      final String source = currentSource.get(traceContent);
      if (source != null) {
      	result.append(spaceString);
      	result.append(source.replaceAll("\n", "\n" + spaceString));
      	result.append("\n");
      }
   }

   private String getSpaceString(List<Integer> currentDepth, final List<Integer> newDepth) {
      String spaceString = "";
      for (final Integer depth : currentDepth) {
      	spaceString += " ";
      	if (depth > 1) {
      		newDepth.add(depth - 1);
      	}
      }
      return spaceString;
   }

	public String getTraceMethods() {
		return toString();
	}

	@Override
	public String toString() {
		return getTraceString(null);
	}
}
