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
import java.util.List;

import org.tmatesoft.svn.core.SVNLogEntry;

public class VersionIteratorSVN extends VersionIterator {

	private final List<SVNLogEntry> entries;
	private final String url;
	
	public VersionIteratorSVN(final File projectFolder, final List<SVNLogEntry> entries, final String url) {
		super(projectFolder);
		this.entries = entries;
		this.url = url;
	}

	@Override
	public int getSize() {
		return entries.size();
	}

	@Override
	public String getTag() {
		return ""+tagid;
	}

	@Override
	public boolean hasNextCommit() {
		return tagid < entries.size();
	}

	@Override
	public void goToNextCommit() {
		tagid++;
		final long startRevision = entries.get(tagid).getRevision();
		SVNUtils.getInstance().checkout(url, projectFolder, startRevision);
	}

	@Override
	public void goToFirstCommit() {
		final long startRevision = entries.get(0).getRevision();
		SVNUtils.getInstance().checkout(url, projectFolder, startRevision);
	}

}
