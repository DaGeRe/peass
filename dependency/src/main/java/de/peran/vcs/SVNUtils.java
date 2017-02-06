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
import java.util.List;

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
import org.tmatesoft.svn.core.wc2.SvnTarget;

/**
 * Helps using git by calling svnkit.
 * 
 * @author reichelt
 *
 */
public class SVNUtils {
	private static Logger LOG = LogManager.getLogger(SVNUtils.class);
	
	private final static int TRIES_CHECKOUT = 100;

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
		LOG.info("LogEntries: {} Erster: {} ",logEntries.size(), logEntries.get(0).getRevision());

		return logEntries;
	}

	
	public void checkout(final String url, final File projectFolder, final SVNRevision revision) {
		final Runnable runnable = () -> {
			LOG.debug("Loading: " + revision.getNumber());
			final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
			try {
				final SvnCheckout checkout = svnOperationFactory.createCheckout();
				final Process p = Runtime.getRuntime().exec("svn revert -R .", null, projectFolder);
				p.waitFor();
				LOG.debug("Folder: {} Revision: {}", projectFolder.getAbsolutePath(), revision.getNumber());
				checkout.setSingleTarget(SvnTarget.fromFile(projectFolder));
				final SVNURL url2 = SVNURL.parseURIDecoded(url);
				checkout.setSource(SvnTarget.fromURL(url2));
				checkout.setRevision(revision);
				final long ergebnis = checkout.run();
				LOG.debug("Finished checkout: {}", ergebnis);
			} catch (final SVNException e) {
				e.printStackTrace();
			} catch (final IOException e) {
				e.printStackTrace();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			} finally {
				svnOperationFactory.dispose();
			}
		};
		final Thread checkoutThread = new Thread(runnable);
		checkoutThread.start();
		int tries = 0;
		while (checkoutThread.isAlive() && tries < TRIES_CHECKOUT) {
			tries++;
			LOG.debug("Waiting for checkout-end");
			try {
				checkoutThread.join(60000);
			} catch (final InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (tries == TRIES_CHECKOUT) {
			LOG.debug("Stopping waiting for checkout-end");
		}
	}

	public void checkout(final String url, final File folder, final long revision) {
		final SVNRevision revision2 = SVNRevision.create(revision);
		checkout(url, folder, revision2);
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

}
