package de.peass.dependency;

import java.io.File;

import de.peass.dependency.execution.GradleParseUtil;

/**
 * Helps managing the PeASS-folders and their existance
 * @author reichelt
 *
 */
public class PeASSFolders {
	protected final File projectFolder;
//	private final File resultFolder;
	private final File fullResultFolder;
	private final File tempResultFolder, tempProjectFolder, tempFolder;;
	private final File logFolder;
	private final File oldSourceFolder;
	private final File detailFolder;
	private final File cleanFolder;
	private File gradleHome;
	
	protected final File peassFolder;

   public PeASSFolders(final File folder) {
		projectFolder = folder;
		peassFolder = new File(projectFolder.getParentFile(), projectFolder.getName() + "_peass");
		if (!peassFolder.exists()) {
			peassFolder.mkdir();
		}
		logFolder = new File(peassFolder, "logs");
		logFolder.mkdir();
		oldSourceFolder = new File(peassFolder, "lastSources");
		oldSourceFolder.mkdir();
		fullResultFolder = new File(peassFolder, "measurementsFull");
		fullResultFolder.mkdir();
		tempFolder = new File(peassFolder, "temp");
		tempFolder.mkdir();
		cleanFolder = new File(peassFolder, "clean");
//		cleanFolder.mkdir();
		detailFolder = new File(fullResultFolder, "measurements");
		detailFolder.mkdir();
		tempResultFolder = new File(peassFolder, "measurementsTemp");
		tempResultFolder.mkdir();
		tempProjectFolder = new File(peassFolder, "projectTemp");
//		tempProjectFolder.mkdir();
	}
   
   public File getGradleHome() {
      if (gradleHome == null) {
         final File peassFolder = new File(projectFolder.getParentFile(), projectFolder.getName() + "_peass");
         gradleHome = new File(peassFolder, "gradleHome");
         gradleHome.mkdir();
         final File init = new File(gradleHome, "init.gradle");
         GradleParseUtil.writeInitGradle(init);
      }
      return gradleHome;
   }

	public File getProjectFolder() {
	   return projectFolder;
	}
	
	public File getCleanFolder() {
      return cleanFolder;
   }
	
	public File getLogFolder() {
		return logFolder;
	}

	public File getOldSources() {
		return oldSourceFolder;
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
      if (!tempProjectFolder.exists()) {
         tempProjectFolder.mkdir();
      }
      return tempProjectFolder;
   }

   public File getTempDir() {
      return tempFolder;
   }

}
