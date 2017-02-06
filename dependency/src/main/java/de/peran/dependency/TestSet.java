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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Represents a set of tests which are executed for one version.
 * @author reichelt
 *
 */
public class TestSet {
	private final Map<String, List<String>> testcases = new HashMap<>();

	public void addTest(final String classname, final String methodname) {
		List<String> methods = testcases.get(classname);
		if (methods == null) {
			methods = new LinkedList<>();
			testcases.put(classname, methods);
		}
		methods.add(methodname);
	}
	
	public Set<Entry<String, List<String>>> entrySet(){
		return testcases.entrySet();
	}
}
