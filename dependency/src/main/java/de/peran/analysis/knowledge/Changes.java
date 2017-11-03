package de.peran.analysis.knowledge;

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


import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Saves all changes for one version. For each testcase  it is saved which change has happened with method, difference in percent etc.
 * 
 * @author reichelt
 *
 */
public class Changes {
	private Map<String, List<Change>> testcaseChanges = new LinkedHashMap<>();

	public Map<String, List<Change>> getTestcaseChanges() {
		return testcaseChanges;
	}

	public void setTestcaseChanges(final Map<String, List<Change>> testcaseChanges) {
		this.testcaseChanges = testcaseChanges;
	}

	/**
	 * Adds a change
	 * 
	 * @param testcase Testcase that has changes
	 * @param viewName	view-file where trace-diff should be saved
	 * @param method	Testmethod where performance changed
	 * @param percent	How much the performance was changed
	 * @return	Added Change
	 */
	public Change addChange(final String testcase, final String viewName, final String method, final double percent) {
		Change change = new Change();
		change.setDiff(viewName);
		change.setChangePercent(percent);
		change.setClazz(method);
		List<Change> currentChanges = testcaseChanges.get(testcase);
		if (currentChanges == null) {
			currentChanges = new LinkedList<>();
			testcaseChanges.put(testcase, currentChanges);
		}
		currentChanges.add(change);
		return change;
	}
}
