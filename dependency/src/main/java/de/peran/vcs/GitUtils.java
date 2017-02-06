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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peran.utils.StreamGobbler;

/**
 * Helps using git from java with CLI calls to git.
 * 
 * @author reichelt
 *
 */
public class GitUtils {

	private static Logger LOG = LogManager.getLogger(GitUtils.class);

	/**
	 * Only utility-clazz, no instantiation needed.
	 */
	private GitUtils() {

	}

	/**
	 * Downloads a project to the given folder
	 * 
	 * @param url
	 * @param folder
	 */
	public void downloadProject(final String url, final File folder) {
		final String command = "git clone " + url + " " + folder.getAbsolutePath();
		Process p;
		try {
			LOG.debug("Command: " + command);
			p = Runtime.getRuntime().exec(command);
			StreamGobbler.showFullProcess(p);
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Removes all commits from a list that are before the given start version or after the given end version.
	 * 
	 * @param startversion
	 * @param endversion
	 * @param commits
	 */
	public static void filterList(final String startversion, final String endversion, final List<GitCommit> commits) {
		LOG.info("Count of Commits: {}", commits.size());
		boolean inRange = startversion == null ? true : false;
		final List<GitCommit> notRelevantCommits = new LinkedList<>();
		for (final GitCommit commit : commits) {
			LOG.trace("Processing " + commit.getTag() + " " + commit.getDate() + " " + inRange);
			if (startversion != null && commit.getTag().equals(startversion)) {
				inRange = true;
			}
			if (!inRange) {
				notRelevantCommits.add(commit);
			}
			if (endversion != null && commit.getTag().equals(endversion)) {
				inRange = false;
			}
		}
		commits.removeAll(notRelevantCommits);
	}

	/**
	 * Returns the commits of the git repo in ascending order.
	 * 
	 * @param ordner
	 * @return
	 */
	public static List<GitCommit> getCommits(final File ordner) {
		final List<GitCommit> commits = new LinkedList<>();
		try {
			final Process p = Runtime.getRuntime().exec("git log --all",
					new String[0], ordner);
			final BufferedReader input = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String line;
			while ((line = input.readLine()) != null) {
				if (line.startsWith("commit")) {
					final String tag = line.substring(7);
					line = input.readLine();
					if (line.startsWith("Merge: ")) {
						line = input.readLine();
					}
					if (line.startsWith("Author:")) {
						final String author = line.substring(8);
						line = input.readLine();
						if (line.startsWith("Date: ")) {
							final String date = line.substring(8);
							// log.debug("Git Commit: {}", tag);
							final GitCommit gc = new GitCommit(tag, author, date, "");
							commits.add(0, gc);
						}
					} else {
						LOG.error("Achtung, falsche Zeile Autor: " + line);
					}
				}
			}
			p.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println("Commits: " + commits.size());
		return commits;
	}

	/**
	 * Lets the project go to the given state by resetting it to revert potential changes and by checking out the given version.
	 * 
	 * @param tag
	 * @param projectFolder
	 */
	public static void goToTag(final String tag, final File projectFolder) {
		try {
			LOG.info("Gehe zu Tag: " + tag + " Ordner: "
					+ projectFolder.getAbsolutePath());
			Process p = Runtime.getRuntime().exec("git reset --hard", new String[0], projectFolder);
			StreamGobbler.showFullProcess(p);
			p.waitFor();

			final String gitCommand = "git checkout " + tag;
			LOG.info(gitCommand);
			p = Runtime.getRuntime().exec(gitCommand, new String[0], projectFolder);
			StreamGobbler.showFullProcess(p);
			p.waitFor();
			LOG.trace("Ausführung beendet");
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

}
