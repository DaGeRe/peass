/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the Affero GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     Affero GNU General Public License for more details.
 *
 *     You should have received a copy of the Affero GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.peran.dependency;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Represents information about the tests and their dependencies, i.e. the classes they call.
 * @author reichelt
 *
 */
public class TestDependencies {
	
	/**
	 * Map from testclass to dependent class to the list of called methods of this class
	 */
	private final Map<String, Map<String, Set<String>>> dependencyMap = new HashMap<>();

	public Map<String, Map<String, Set<String>>> getDependencyMap() {
		return dependencyMap;
	}

	/**
	 * Gets the dependencies for a test, i.e. the used classes. If the test is not known yet, an empty Set is returned.
	 * 
	 * @param test
	 */
	public Map<String, Set<String>> getDependenciesForTest(final String test) {
		Map<String, Set<String>> tests = dependencyMap.get(test);
		if (tests == null) {
			tests = new HashMap<>();
			dependencyMap.put(test, tests);
		}
		return tests;
	}

	public int size() {
		return dependencyMap.size();
	}

	public Map<String, Map<String, Set<String>>> getCopiedDependencies() {
		final Map<String, Map<String, Set<String>>> oldDepdendencies = new HashMap<>();
		for (final Map.Entry<String, Map<String, Set<String>>> entry : dependencyMap.entrySet()) {
			final Map<String, Set<String>> dependencies = new HashMap<>();
			for (final Map.Entry<String, Set<String>> testcase : entry.getValue().entrySet()){
				final Set<String> copiedMethods = new HashSet<>();
				copiedMethods.addAll(testcase.getValue());
				dependencies.put(entry.getKey(), copiedMethods);
			}
			oldDepdendencies.put(entry.getKey(), dependencies);
		}
		return oldDepdendencies;
	}
	
	@Override
	public String toString(){
		return dependencyMap.toString();
	}

}
