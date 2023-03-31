package de.dagere.peass.dependency.analysis.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.dagere.nodeDiffDetector.data.Type;

/**
 * Map from called classes to the methods of the classes that are called
 * 
 * @author reichelt
 *
 */
public class CalledMethods {
	private final Map<Type, Set<String>> calledMethods = new HashMap<>();

	public Map<Type, Set<String>> getCalledMethods() {
		return calledMethods;
	}

	public Set<Type> getCalledClasses() {
		return calledMethods.keySet();
	}

	@Override
	public String toString() {
		return calledMethods.toString();
	}
}