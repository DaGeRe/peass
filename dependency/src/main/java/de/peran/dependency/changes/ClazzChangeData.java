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
package de.peran.dependency.changes;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents the relevant data of changes between two versions, i.e. whether there was a change, whether the change only affected methods, and if so, which methods where affected.
 * 
 * @author reichelt
 *
 */
public class ClazzChangeData {
	private boolean isChange = true;
	private boolean isOnlyMethodChange = true;
	private final String clazz;
	private final Set<String> changedMethods = new HashSet<>();

	public ClazzChangeData(final String clazz) {
		this.clazz = clazz;
	}

	public boolean isChange() {
		return isChange;
	}

	public void setChange(final boolean isChange) {
		this.isChange = isChange;
	}

	public boolean isOnlyMethodChange() {
		return isOnlyMethodChange;
	}

	public void setOnlyMethodChange(final boolean isOnlyMethodChange) {
		this.isOnlyMethodChange = isOnlyMethodChange;
	}

	public String getClazz() {
		return clazz;
	}

	public Set<String> getChangedMethods() {
		return changedMethods;
	}
}