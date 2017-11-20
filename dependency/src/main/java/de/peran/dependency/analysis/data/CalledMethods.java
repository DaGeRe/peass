package de.peran.dependency.analysis.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Map from called classes to the methods of the classes that are called
 * @author reichelt
 *
 */
public class CalledMethods{
	Map<String, Set<String>> calledMethods = new HashMap<>();

	public Map<String, Set<String>> getCalledMethods() {
		return calledMethods;
	}

	public void setCalledMethods(final Map<String, Set<String>> calledMethods) {
		this.calledMethods = calledMethods;
	}
	
	public Set<String> getCalledClasses(){
		return calledMethods.keySet();
	}
	
	@Override
	public String toString() {
		return calledMethods.toString();
	}
}