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
package de.peran.vcs;

import java.io.File;

/**
 * Represents the used version control system
 * @author reichelt
 *
 */
public enum VersionControlSystem {
	SVN, GIT;

	public static VersionControlSystem getVersionControlSystem(final File projectFolder) {
		if (new File(projectFolder, ".svn").exists()) {
			return SVN;
		} else if (new File(projectFolder, ".git").exists()) {
			return GIT;
		} else {
			throw new RuntimeException("Unbekannter Versionskontrollsystemtyp - .git und .svn nicht gefunden");
		}
	}
}
