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
import java.io.IOException;

import de.peran.dependency.changes.VersionDiff;
import de.peran.utils.StreamGobbler;

/**
 * Helper class for loading diffs between versions in git versions.
 * @author reichelt
 *
 */
public class GitDiffLoader {

	private GitDiffLoader(){
		
	}
	
	/**
	 * Gets diff between current revision and previous revision of repo.
	 * 
	 * @param projectFolder Local working copy of the repo.
	 * @return
	 */
	public static VersionDiff getChangedClasses(final File projectFolder) {
		try {
			final Process p = Runtime.getRuntime().exec("git diff --name-only HEAD^ HEAD", null, projectFolder);
			return getDiffFromProcess(p);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static VersionDiff getDiffFromProcess(final Process p) {
		final VersionDiff diff = new VersionDiff();
		final String output = StreamGobbler.getFullProcess(p, false);
		for (final String line : output.split("\n")) {
			diff.addChange(line);
		}
		return diff;
	}
}
