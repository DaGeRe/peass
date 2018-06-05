package de.peran.dependency;

import java.io.File;

/**
 * Helps managing the PeASS-folders and their existance
 * @author reichelt
 *
 */
public class PeASSFolders {
	private final File projectFolder;
	private final File resultFolder;
	private final File fullResultFolder;
	private final File tempResultFolder, tempProjectFolder;
	private final File logFolder;
	private final File lastSourceFolder;
	private final File detailFolder;

	public PeASSFolders(final File folder) {
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
		detailFolder = new File(fullResultFolder, "measurements");
		detailFolder.mkdir();
		tempResultFolder = new File(peassFolder, "measurementsTemp");
		tempResultFolder.mkdir();
		tempProjectFolder = new File(peassFolder, "projectTemp");
		tempProjectFolder.mkdir();
	}

	public File getProjectFolder() {
	   return projectFolder;
	}
	
	public File getKiekerResultFolder() {
		return resultFolder;
	}

	public File getLogFolder() {
		return logFolder;
	}

	public File getLastSources() {
		return lastSourceFolder;
	}

	public File getFullMeasurementFolder() {
		return fullResultFolder;
	}

	public File getTempMeasurementFolder() {
		return tempResultFolder;
	}

	public File getDetailResultFolder() {
		return detailFolder;
	}

   public File getTempProjectFolder() {
      return tempProjectFolder;
   }

}
