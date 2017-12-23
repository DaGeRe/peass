package de.peran.dependencytests;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

import de.peran.vcs.VersionIterator;

class FakeIterator extends VersionIterator {
	
	List<File> commits;

	public FakeIterator(final File folder, final List<File> commits) {
		super(folder);
		this.commits = commits;
	}

	int tag = 0;

	@Override
	public int getSize() {
		return commits.size();
	}

	@Override
	public String getTag() {
		return "" + tag;
	}

	@Override
	public boolean hasNextCommit() {
		return tag < commits.size() + 1;
	}

	@Override
	public boolean goToNextCommit() {
		tag++;
		try {
			FileUtils.deleteDirectory(projectFolder);
			FileUtils.copyDirectory(commits.get(tag - 1), projectFolder);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public boolean goToFirstCommit() {
		tag = 0;
		return true;
	}

	@Override
	public boolean goTo0thCommit() {
		throw new RuntimeException("Not implemented on purpose.");
	}
}