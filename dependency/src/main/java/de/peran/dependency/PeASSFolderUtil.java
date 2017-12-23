package de.peran.dependency;

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

	public static File getKiekerResultFolder() {
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
