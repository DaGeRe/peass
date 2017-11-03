package de.peran.dependency.traces;

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


import java.util.ArrayList;
import java.util.List;

import de.peran.dependency.analysis.data.TraceElement;

/**
 * Represents a trace, i.e. a sorted list of all calls of a testcase, and the source code of the calls.
 * @author reichelt
 *
 */
public class TraceWithMethods {

	private final List<TraceElement> elements = new ArrayList<>();
	private final List<String> methods = new ArrayList<>();

	public void addElement(final TraceElement traceElement, final String method) {
		elements.add(traceElement);
		methods.add(method);
	}

	public TraceElement getTraceElement(final int position) {
		return elements.get(position);
	}

	public String getMethod(final int position) {
		return methods.get(position);
	}

	public int getLength() {
		return elements.size();
	}

	public String getWholeTrace() {
		String result = "";
		for (int i = 0; i < elements.size(); i++) {
			result += elements.get(i) != null ? elements.get(i) + "\n" : "";
			result += methods.get(i) + "\n";
		}
		return result;
	}

	public void removeElement(int removeIndex) {
		elements.remove(removeIndex);
		methods.remove(removeIndex);
	}

	public String getTraceMethods() {
		String result = "";
		for (int i = 0; i < elements.size(); i++) {
			for (int spaceCount = 0; spaceCount < elements.get(i).getDepth(); spaceCount++) {
				result += " ";
			}
			result += elements.get(i).getClazz() + "." + elements.get(i).getMethod() + "\n";
		}
		return result;
	}
	
	public String toString(){
		return elements.toString();
	}
}
