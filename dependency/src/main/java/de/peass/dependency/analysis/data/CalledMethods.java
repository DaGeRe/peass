package de.peass.dependency.analysis.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Map from called classes to the methods of the classes that are called
 * 
 * @author reichelt
 *
 */
public class CalledMethods {
	private final Map<ChangedEntity, Set<String>> calledMethods = new HashMap<>();

	public Map<ChangedEntity, Set<String>> getCalledMethods() {
		return calledMethods;
	}

	public Set<ChangedEntity> getCalledClasses() {
		return calledMethods.keySet();
	}

	@Override
	public String toString() {
		return calledMethods.toString();
	}
}