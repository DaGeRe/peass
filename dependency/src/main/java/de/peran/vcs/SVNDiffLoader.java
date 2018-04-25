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
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.peran.dependency.analysis.data.VersionDiff;
import de.peran.utils.StreamGobbler;

/**
 * Helper class for getting diffs between svn versions.
 * @author reichelt
 *
 */
public class SVNDiffLoader {

	private static final Logger LOG = LogManager.getLogger(SVNDiffLoader.class);

	public static final String URL = "url";
	public static final String REVISIONS = "revisions";

	/**
	 * Gets diff between current revision and previous revision of repo.
	 * 
	 * @param projectFolder Local working copy of the repo.
	 * @return
	 */
	public VersionDiff getChangedClasses(final File projectFolder) {
		try {
			final Process p = Runtime.getRuntime().exec("svn diff --summarize --xml -r PREV:BASE", null, projectFolder);
			return getDiffFromProcess(p);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Determines which classes are changed in an svn repo between two revisions. Furthermore determines weather the pom.xml has changed.
	 * 
	 * @param url
	 * @param revisionString
	 * @return
	 */
	public VersionDiff getChangedClasses(final String url, final String revisionStart, final String revisionDest) {
		try {
			final Process p = Runtime.getRuntime().exec("svn diff --summarize --xml " + url + "@" + revisionStart + " " + url + "@" + revisionDest);
			return getDiffFromProcess(p);

		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private VersionDiff getDiffFromProcess(final Process p) {
		final VersionDiff diff = new VersionDiff(new LinkedList<>());//TODO Für multmodulprojekte anpassen
		final String output = StreamGobbler.getFullProcess(p, false);

		final InputSource is = new InputSource(new StringReader(output));

		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.parse(is);
			final NodeList nodeList = doc.getChildNodes();

			final Node diffItem = nodeList.item(0);
			final Node pathsItem = diffItem.getChildNodes().item(1);

			for (int pathIndex = 0; pathIndex < pathsItem.getChildNodes().getLength(); pathIndex++) {
				final Node pathNode = pathsItem.getChildNodes().item(pathIndex);
				if (pathNode.getChildNodes().getLength() > 0) {
					final String currentFileName = pathNode.getChildNodes().item(0).getNodeValue();
					diff.addChange(currentFileName);
				}
			}
		} catch (final ParserConfigurationException e) {
			e.printStackTrace();
		} catch (final SAXException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return diff;
	}

}
