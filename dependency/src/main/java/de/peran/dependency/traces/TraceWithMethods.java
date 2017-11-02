package de.peran.dependency.traces;

import java.util.ArrayList;
import java.util.List;

import de.peran.dependency.analysis.data.TraceElement;

/**
 * Represents a trace, i.e. a sorted list of all calls of a testcase, and the source code of the calls.
 * @author reichelt
 *
 */
public class TraceWithMethods {

	private final List<TraceElement> elements = new ArrayList<>();
	private final List<String> methods = new ArrayList<>();

	public void addElement(final TraceElement traceElement, final String method) {
		elements.add(traceElement);
		methods.add(method);
	}

	public TraceElement getTraceElement(final int position) {
		return elements.get(position);
	}

	public String getMethod(final int position) {
		return methods.get(position);
	}

	public int getLength() {
		return elements.size();
	}

	public String getWholeTrace() {
		String result = "";
		for (int i = 0; i < elements.size(); i++) {
			result += elements.get(i) != null ? elements.get(i) + "\n" : "";
			result += methods.get(i) + "\n";
		}
		return result;
	}

	public void removeElement(int removeIndex) {
		elements.remove(removeIndex);
		methods.remove(removeIndex);
	}

	public String getTraceMethods() {
		String result = "";
		for (int i = 0; i < elements.size(); i++) {
			for (int spaceCount = 0; spaceCount < elements.get(i).getDepth(); spaceCount++) {
				result += " ";
			}
			result += elements.get(i).getClazz() + "." + elements.get(i).getMethod() + "\n";
		}
		return result;
	}
	
	public String toString(){
		return elements.toString();
	}
}
