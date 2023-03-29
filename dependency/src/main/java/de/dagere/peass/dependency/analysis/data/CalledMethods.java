package de.dagere.peass.dependency.analysis.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.dagere.nodeDiffDetector.data.MethodCall;

/**
 * Map from called classes to the methods of the classes that are called
 * 
 * @author reichelt
 *
 */
public class CalledMethods {
	private final Map<MethodCall, Set<String>> calledMethods = new HashMap<>();

	public Map<MethodCall, Set<String>> getCalledMethods() {
		return calledMethods;
	}

	public Set<MethodCall> getCalledClasses() {
		return calledMethods.keySet();
	}

	@Override
	public String toString() {
		return calledMethods.toString();
	}
}