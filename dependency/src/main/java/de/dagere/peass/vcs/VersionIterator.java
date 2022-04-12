/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.dagere.peass.vcs;

import java.io.File;
import java.util.List;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.VersionDiff;

/**
 * Iterator for VCS, moving always the position of the iterator alongside with the version saved in the folder.
 * @author reichelt
 *
 */
public abstract class VersionIterator {

	protected final File projectFolder;
	protected int tagid = 0;

	public VersionIterator(final File projectFolder) {
		this.projectFolder = projectFolder;
	}

	/**
	 * Returns the count of commits
	 * 
	 * @return count of commits
	 */
	public abstract int getSize();
	
	public int getRemainingSize() {
	   return -1;
	}
	
	/**
	 * Returns the current tag
	 * 
	 * @return current tag
	 */
	public abstract String getTag();
	
	/**
	 * Returns the predecessor tag
	 * @return
	 */
	public abstract String getPredecessor();

	/**
	 * Whether a next commit is present
	 * @return True, if a next commit is present, false otherwise
	 */
	public abstract boolean hasNextCommit();

	/**
	 * Goes to next commit, also checking out next version in the folder
	 * @return True for success, false otherwise
	 */
	public abstract boolean goToNextCommit();
	
	/**
	 * Goes to first commit, both in the iterator and the folder
	 * @return True for success, false otherwise
	 */
	public abstract boolean goToFirstCommit();

	/**
	 * Checkout the Commit before the start (if no one is given, just move the iterator and do not change folder state)
	 * @return Whether the 0th commit is not equal to the current commit
	 */
	public abstract boolean goTo0thCommit();

   public abstract boolean isPredecessor(String lastRunningVersion);

   public abstract boolean goToPreviousCommit();

   public abstract VersionDiff getChangedClasses(File projectFolder, List<File> genericModules, String lastVersion, ExecutionConfig config);

   

}