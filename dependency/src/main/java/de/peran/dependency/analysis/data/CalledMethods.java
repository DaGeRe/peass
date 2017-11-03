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
