/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.peran.dependency.analysis.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents information about the tests and their dependencies, i.e. the classes they call.
 * 
 * @author reichelt
 *
 */
public class TestDependencies {
	private static final Logger LOG = LogManager.getLogger(TestDependencies.class);
	
	/**
	 * Map from testcase (package.clazz.method) to dependent class to the list of called methods of this class
	 */
	private final Map<String, CalledMethods> dependencyMap = new HashMap<>();
	
	public TestDependencies(){
		
	}

	public Map<String, CalledMethods> getDependencyMap() {
		return dependencyMap;
	}

	/**
	 * Gets the dependencies for a test, i.e. the used classes. If the test is not known yet, an empty Set is returned.
	 * 
	 * @param test
	 */
	public Map<String, Set<String>> getDependenciesForTest(final String test) {
		CalledMethods tests = dependencyMap.get(test);
		if (tests == null) {
			tests = new CalledMethods();
			dependencyMap.put(test, tests);
		}
		return tests.getCalledMethods();
	}

	public void removeTest(String clazz, String method) {
		dependencyMap.remove(clazz +"."+ method);
	}

	public int size() {
		return dependencyMap.size();
	}

	public Map<String, Map<String, Set<String>>> getCopiedDependencies() {
		final Map<String, Map<String, Set<String>>> copy = new HashMap<>();
		for (final Map.Entry<String, CalledMethods> entry : dependencyMap.entrySet()) {
			final Map<String, Set<String>> dependencies = new HashMap<>();
			for (final Map.Entry<String, Set<String>> testcase : entry.getValue().getCalledMethods().entrySet()) {
				final Set<String> copiedMethods = new HashSet<>();
				copiedMethods.addAll(testcase.getValue());
				dependencies.put(entry.getKey(), copiedMethods);
			}
			copy.put(entry.getKey(), dependencies);
		}
		return copy;
	}

	@Override
	public String toString() {
		return dependencyMap.toString();
	}

}
