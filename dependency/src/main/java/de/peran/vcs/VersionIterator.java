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
package de.peran.vcs;

import java.io.File;

public abstract class VersionIterator {
	
	protected final File projectFolder;
	protected int tagid = 0;

	public VersionIterator(final File projectFolder) {
		this.projectFolder = projectFolder;
	}

	/**
	 * Returns the count of commits
	 * @return count of commits
	 */
	public abstract int getSize();

	/**
	 * Returns the current tag
	 * @return current tag
	 */
	public abstract String getTag();

	public abstract boolean hasNextCommit();

	public abstract void goToNextCommit();

	public abstract void goToFirstCommit();

}