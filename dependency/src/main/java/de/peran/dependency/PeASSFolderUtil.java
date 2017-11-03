package de.peran.dependency;

/*-
 * #%L
 * peran-dependency
 * %%
 * Copyright (C) 2017 DaGeRe
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


import java.io.File;

/**
 * Helps managing the PeASS-folders and their existance
 * @author reichelt
 *
 */
public class PeASSFolderUtil {
	static File projectFolder;
	static File resultFolder;
	static File fullResultFolder;
	static File tempResultFolder;
	static File logFolder;
	static File lastSourceFolder;
	static File measurementLogFolder;
	static File detailFolder;

	public static void setProjectFolder(final File folder) {
		projectFolder = folder;
		final File peassFolder = new File(projectFolder.getParentFile(), projectFolder.getName() + "_peass");
		if (!peassFolder.exists()) {
			peassFolder.mkdir();
		}
		resultFolder = new File(peassFolder, "results_kieker");
		resultFolder.mkdir();
		logFolder = new File(peassFolder, "logs");
		logFolder.mkdir();
		lastSourceFolder = new File(peassFolder, "lastSources");
		lastSourceFolder.mkdir();
		fullResultFolder = new File(peassFolder, "measurementsFull");
		fullResultFolder.mkdir();
//		measurementLogFolder = new File(fullResultFolder, "logs");
//		measurementLogFolder.mkdir();
		detailFolder = new File(fullResultFolder, "measurements");
		detailFolder.mkdir();
		tempResultFolder = new File(peassFolder, "measurementsTemp");
		tempResultFolder.mkdir();
	}

	public static File getResultFolder() {
		return resultFolder;
	}

	public static File getLogFolder() {
		return logFolder;
	}

	public static File getLastSources() {
		return lastSourceFolder;
	}

	public static File getFullMeasurementFolder() {
		return fullResultFolder;
	}

	public static File getTempMeasurementFolder() {
		return tempResultFolder;
	}

	public static File getDetailResultFolder() {
		return detailFolder;
	}

}
