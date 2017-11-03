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


/**
 * Represents a testcase with its class and its method. If no method is given, the whole class with all methods is represented.
 * 
 * @author reichelt
 *
 */
public class TestCase {
	private final String clazz;
	private final String method;

	public TestCase(final String clazz, final String method) {
		super();
		this.clazz = clazz;
		this.method = method;
	}

	public TestCase(final String testcase) {
		final int index = testcase.lastIndexOf("#");
		if (index == -1) {
			clazz = testcase;
			method = null;
			// final int indexDot = testcase.lastIndexOf(".");
			// clazz = testcase.substring(0, indexDot);
			// method = testcase.substring(indexDot + 1);
		} else {
			clazz = testcase.substring(0, index);
			method = testcase.substring(index + 1);
		}
	}

	public String getClazz() {
		return clazz;
	}

	public String getMethod() {
		return method;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final TestCase other = (TestCase) obj;
		if (clazz == null) {
			if (other.clazz != null) {
				return false;
			}
		} else if (!clazz.equals(other.clazz)) {
			return false;
		}
		if (method == null) {
			if (other.method != null) {
				return false;
			}
		} else if (!method.equals(other.method)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "TestCase [clazz=" + clazz + ", method=" + method + "]";
	}

}
