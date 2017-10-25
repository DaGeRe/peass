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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

/**
 * Helps using svn by calling svnkit.
 * 
 * @author reichelt
 *
 */
public class SVNUtils {
	private static Logger LOG = LogManager.getLogger(SVNUtils.class);

	private static SVNUtils instance;

	private SVNUtils() {
		DAVRepositoryFactory.setup();
		SVNRepositoryFactoryImpl.setup();
		FSRepositoryFactory.setup();
	}

	public static SVNUtils getInstance() {
		if (instance == null) {
			instance = new SVNUtils();
		}
		return instance;
	}

	public long getWCRevision(final File folder) {
		if (!SvnOperationFactory.isVersionedDirectory(folder)) {
			LOG.error(folder.getAbsolutePath() + " ist kein von SVN verwaltetes Verzeichnis");
		}
		try {
			final SVNInfo info = SVNClientManager.newInstance().getWCClient().doInfo(folder, SVNRevision.WORKING);
			return info.getRevision().getNumber();
		} catch (final SVNException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	public String getWCURL(final File folder) {
		if (!SvnOperationFactory.isVersionedDirectory(folder)) {
			LOG.error(folder.getAbsolutePath() + " ist kein von SVN verwaltetes Verzeichnis");
		}
		try {
			final SVNInfo info = SVNClientManager.newInstance().getWCClient().doInfo(folder, SVNRevision.WORKING);
			return info.getURL().toDecodedString();
		} catch (final SVNException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * Returns all SVN-Versions of the given repository
	 * 
	 * @param url
	 * @return
	 */
	public List<SVNLogEntry> getVersions(final String url, final long startrevision) {
		SVNRepository repository = null;

		try {
			repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(url));
		} catch (final SVNException svne) {
			System.err.println("error while creating an SVNRepository for the location '" + url + "': " + svne.getMessage());
			System.exit(1);
		}

		List<SVNLogEntry> logEntries = null;
		try {
			final long endrevision = repository.getLatestRevision();
			logEntries = (List<SVNLogEntry>) repository.log(new String[] { "" }, null, startrevision, endrevision, false, true);
		} catch (final SVNException svne) {
			System.out.println("error while collecting log information for '" + url + "': " + svne.getMessage());
			System.exit(1);
		}
		LOG.info("LogEntries: " + logEntries.size());

		return logEntries;
	}

	/**
	 * Returns all SVN-Versions of the given repository
	 * 
	 * @param url
	 * @return
	 */
	public List<SVNLogEntry> getVersions(final String url, final long startrevision, final long endrevision) {
		SVNRepository repository = null;

		try {
			repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(url));
		} catch (final SVNException svne) {
			System.err.println("error while creating an SVNRepository for the location '" + url + "': " + svne.getMessage());
			System.exit(1);
		}

		List<SVNLogEntry> logEntries = null;
		try {
			logEntries = (List<SVNLogEntry>) repository.log(new String[] { "" }, null, startrevision, endrevision, false, true);
		} catch (final SVNException svne) {
			System.out.println("error while collecting log information for '" + url + "': " + svne.getMessage());
			System.exit(1);
		}
		LOG.info("LogEntries: " + logEntries.size());

		return logEntries;
	}

	/**
	 * Returns all SVN-Versions of the given repository
	 * 
	 * @param url
	 * @return
	 */
	public List<SVNLogEntry> getVersions(final String url) {
		SVNRepository repository = null;

		try {
			repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(url));
		} catch (final SVNException svne) {
			System.err.println("error while creating an SVNRepository for the location '" + url + "': " + svne.getMessage());
			System.exit(1);
		}

		List<SVNLogEntry> logEntries = null;
		try {
			final long endrevision = repository.getLatestRevision();
			logEntries = (List<SVNLogEntry>) repository.log(new String[] { "" }, null, 1, endrevision, true, true);

		} catch (final SVNException svne) {
			System.out.println("error while collecting log information for '" + url + "': " + svne.getMessage());
			System.exit(1);
		}
		LOG.info("LogEntries: {} Erster: {} ", logEntries.size(), logEntries.get(0).getRevision());

		return logEntries;
	}

	public boolean checkout(final String url, final File projectFolder, final SVNRevision revision) {
		LOG.debug("Loading: " + revision.getNumber());
		final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
		try {
			final SvnCheckout checkout = svnOperationFactory.createCheckout();
			final Process revertProcess = Runtime.getRuntime().exec("svn revert -R .", null, projectFolder);
			revertProcess.waitFor(60, TimeUnit.SECONDS);
			final Process p2 = Runtime.getRuntime().exec("svn update -r " + revision.getNumber(), null, projectFolder);
			String fullProcess = de.peran.utils.StreamGobbler.getFullProcess(p2, true);
			boolean success = p2.waitFor(60, TimeUnit.SECONDS);
			if (success) {
				int returncode = p2.waitFor();
				return returncode == 0;
			} else {
				return false;
			}

		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		} finally {
			svnOperationFactory.dispose();
		}
		return false;
	}

	public boolean checkout(final String url, final File folder, final long revision) {
		final SVNRevision revision2 = SVNRevision.create(revision);
		return checkout(url, folder, revision2);
	}

	public void revert(final File projectFolder) {
		try {
			Process p = Runtime.getRuntime().exec("svn cleanup", null, projectFolder);
			p.waitFor();
			p = Runtime.getRuntime().exec("svn revert -R .", null, projectFolder);
			p.waitFor();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

	}

	public List<SVNLogEntry> getVersions(File folder) {
		List<SVNLogEntry> entries = new LinkedList<>();
		try {
			final Process p = Runtime.getRuntime().exec("svn log", new String[0], folder);

			final BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = input.readLine()) != null) {
				if (line.startsWith("r") && line.contains("|") && line.substring(0, line.indexOf("|")).matches("r[0-9]+ ")) {
					LOG.info(line);
					int revision = Integer.parseInt(line.substring(1, line.indexOf(" ")));
					String author = line.substring(line.indexOf("|"), line.lastIndexOf("|"));
					entries.add(0, new SVNLogEntry(new HashMap<>(), revision, author, new Date(), ""));
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return entries;
	}

}
