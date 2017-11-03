package de.peran.dependency.analysis.data;

/*-
 * #%L
 * peran-dependency
 * %%
 * Copyright (C) 2017 DaGeRe
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */


import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Map from changed classes fqn to testcases that might have changed
 * @author reichelt
 *
 */
public class ChangeTestMapping {
	private final Map<String, Set<String>> changes = new TreeMap<>();
//	private final List<String> addedTestClasses = new LinkedList<>();

	public Map<String, Set<String>> getChanges() {
		return changes;
	}
	
//	public List<String> getAddedTestClasses() {
//		return addedTestClasses;
//	}
//
//	public void addAddedTest(String changedClass) {
//		addedTestClasses.add(changedClass);
//	}
}
