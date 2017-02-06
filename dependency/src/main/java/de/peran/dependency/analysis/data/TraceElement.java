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
package de.peran.dependency.analysis.data;

import java.util.Arrays;

public class TraceElement {
	private String clazz, method;
	
	private boolean isStatic = false;
	
	private String[] parameterTypes = null;
	
	private int depth;

	public TraceElement(final String clazz, final String method, final int depth) {
		super();
		this.clazz = clazz;
		this.method = method;
		this.depth = depth;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(final int depth) {
		this.depth = depth;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public void setStatic(final boolean isStatic) {
		this.isStatic = isStatic;
	}

	public String[] getParameterTypes() {
		return parameterTypes;
	}

	public void setParameterTypes(final String[] parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	public String getClazz() {
		return clazz;
	}

	public void setClazz(final String clazz) {
		this.clazz = clazz;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(final String method) {
		this.method = method;
	}
	
	@Override
	public String toString() {
		return clazz + "." + method + " (" + (parameterTypes != null ? Arrays.toString(parameterTypes) : "") + ")";
	}
}
