package de.peran;

/*-
 * #%L
 * peran-measurement
 * %%
 * Copyright (C) 2015 - 2017 DaGeRe
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */


import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;

import de.peran.dependencyprocessors.VersionProcessor;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.utils.StreamGobbler;
import de.peran.vcs.GitUtils;

/**
 * Starts running every version just for downloading the dependencies. After all dependencies have been downloaded, most operations can be run locally (as long as the version control system can
 * operate locally).
 * 
 * @author reichelt
 *
 */
public class VersionRunStarter extends VersionProcessor {

	VersionRunStarter(final String[] args) throws ParseException, JAXBException {
		super(args);
	}

	@Override
	protected void processVersion(final Version version) {
		GitUtils.goToTag(version.getVersion(), projectFolder);
		try {
			final Process p = Runtime.getRuntime().exec("mvn clean package -DskipTests=true", null, projectFolder);
			StreamGobbler.showFullProcess(p);
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(final String[] args) throws ParseException, JAXBException {
		final VersionRunStarter vr = new VersionRunStarter(args);
		vr.processCommandline();
	}
}
