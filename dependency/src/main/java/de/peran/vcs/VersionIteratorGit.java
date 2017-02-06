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

public class VersionIteratorGit extends VersionIterator {
	
	private final List<GitCommit> entries;
	
	public VersionIteratorGit(final List<GitCommit> entries, final File projectFolder) {
		super(projectFolder);
		this.entries = entries;
	}
	
	@Override
	public void goToFirstCommit(){
		GitUtils.goToTag(entries.get(0).getTag(), projectFolder);
	}
	
	@Override
	public void goToNextCommit(){
		tagid++;
		GitUtils.goToTag(entries.get(tagid).getTag(), projectFolder);
	}
	
	@Override
	public boolean hasNextCommit(){
		return tagid < entries.size();
	}

	@Override
	public String getTag() {
		return entries.get(tagid).getTag();
	}

	@Override
	public int getSize() {
		return entries.size();
	}
	
	
}
