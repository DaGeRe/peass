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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.peran.generated.Versiondependencies.Versions.Version.Dependency;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency.Testcase;

import java.util.Set;
import java.util.TreeMap;

/**
 * Represents a set of tests which are executed for one version by its class and its list of methods.
 * 
 * @author reichelt
 *
 */
public class TestSet {
	private final Map<String, List<String>> testcases = new TreeMap<>();

	public TestSet() {

	}

	public TestSet(List<Dependency> dependencies) {
		for (Dependency dependency : dependencies) {
			for (Testcase testcase : dependency.getTestcase()) {
				String clazz = testcase.getClazz();
				for (String method : testcase.getMethod()) {
					addTest(clazz, method);
				}
			}
		}
	}

	/**
	 * Adds a test to the TestSet. If the method is null, and no further method
	 * is added, the TestSet contains the all methods of the test; if one method
	 * is added, only the method (and perhaps future added methods) are
	 * included.
	 * 
	 * @param classname
	 * @param methodname
	 */
	public void addTest(final String classname, final String methodname) {
		List<String> methods = testcases.get(classname);
		if (methods == null) {
			methods = new LinkedList<>();
			testcases.put(classname.intern(), methods);
		}
		if (methodname != null) {
			String internalizedMethodName = methodname.intern();
			if (!methods.contains(internalizedMethodName)) {
				methods.add(internalizedMethodName);
			}
		}
	}

	public void addTestSet(final TestSet testSet) {
		for (final Map.Entry<String, List<String>> newTestEntry : testSet.entrySet()) {
			List<String> methods = testcases.get(newTestEntry.getKey());
			if (methods == null) {
				methods = new LinkedList<>();
				testcases.put(newTestEntry.getKey().intern(), methods);
			}
			if (newTestEntry.getValue().size() != 0 && methods.size() != 0) {
				methods.addAll(newTestEntry.getValue());
			} else {
				// If List is empty, all methods are changed -> Should be
				// remembered
				methods.clear();
			}
		}
	}

	public Set<Entry<String, List<String>>> entrySet() {
		return testcases.entrySet();
	}

	public int size() {
		return testcases.size();
	}

	public Set<String> getClasses() {
		return testcases.keySet();
	}

	public Map<String, List<String>> getTestcases() {
		return testcases;
	}

	public void removeTest(final String testClassName, final String testMethodName) {
		testcases.get(testClassName).remove(testMethodName);
	}

	@Override
	public String toString() {
		return testcases.toString();
	}

	public List<String> getMethods(String clazz) {
		return testcases.get(clazz);
	}
}
