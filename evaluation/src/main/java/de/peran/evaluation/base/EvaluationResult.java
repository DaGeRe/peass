package de.peran.evaluation.base;

/*-
 * #%L
 * peran-evaluation
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


/**
 * Result of one evaluation process with the methods that have been run in both and with the methods that would have been run in the evaluated project
 * 
 * @author reichelt
 *
 */
public class EvaluationResult {
	private int equalMethods;
	private int overallMethods;

	public int getEqualMethods() {
		return equalMethods;
	}

	public void setEqualMethods(final int equalMethods) {
		this.equalMethods = equalMethods;
	}

	public int getOverallMethods() {
		return overallMethods;
	}

	public void setOverallMethods(final int overallMethods) {
		this.overallMethods = overallMethods;
	}
}
