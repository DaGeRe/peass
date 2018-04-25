package de.peran.dependency;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.github.javaparser.ParseException;

import de.peran.dependency.analysis.FileComparisonUtil;
import de.peran.dependency.analysis.data.ChangedEntity;
import de.peran.dependency.analysis.data.ClazzChangeData;
import de.peran.dependency.analysis.data.VersionDiff;
import de.peran.dependency.execution.MavenPomUtil;
import de.peran.vcs.GitUtils;
import de.peran.vcs.SVNDiffLoader;
import de.peran.vcs.VersionControlSystem;

/**
 * Determines whether a file has a change, and whether this change is class-wide or only affecting a method.
 * 
 * @author reichelt
 *
 */
public class ChangeManager {

	private static final Logger LOG = LogManager.getLogger(ChangeManager.class);

	private final File projectFolder;
	private final File lastSourcesFolder;
	private final VersionControlSystem vcs;

	public ChangeManager(final File projectFolder, final File moduleFolder) {
		this.projectFolder = projectFolder;
		// this.moduleFolder = moduleFolder;
		PeASSFolderUtil.setProjectFolder(projectFolder);
		vcs = VersionControlSystem.getVersionControlSystem(projectFolder);
		lastSourcesFolder = PeASSFolderUtil.getLastSources();
	}

	public ChangeManager(final File projectFolder) {
		this.projectFolder = projectFolder;
		// this.moduleFolder = projectFolder;
		PeASSFolderUtil.setProjectFolder(projectFolder);
		vcs = VersionControlSystem.getVersionControlSystem(projectFolder);
		lastSourcesFolder = PeASSFolderUtil.getLastSources();
	}

	/**
	 * Returns a set of the full qualified names of all classes that have been changed in the current revision.
	 * 
	 * @return full qualified names of all classes that have been changed in the current revision.
	 */
	private List<ChangedEntity> getChangedClasses() {
		final VersionDiff diff;
		if (vcs.equals(VersionControlSystem.SVN)) {
			diff = new SVNDiffLoader().getChangedClasses(projectFolder);
		} else if (vcs.equals(VersionControlSystem.GIT)) {
			diff = GitUtils.getChangedClasses(projectFolder);
		} else {
			throw new RuntimeException(".git or .svn not there - Can only happen if .git or .svn is deleted between constructor and method call ");
		}

		LOG.info("Changed classes: " + diff.getChangedClasses().size());
		return diff.getChangedClasses();
	}

	public void saveOldClasses() {
		try {
			if (lastSourcesFolder.exists()) {
				FileUtils.deleteDirectory(lastSourcesFolder);
			}
			lastSourcesFolder.mkdir();
			for (final File module : MavenPomUtil.getModules(new File(projectFolder, "pom.xml"))) {
				final String moduleName = module.getName();
				FileUtils.copyDirectory(new File(module, "src"), new File(lastSourcesFolder, moduleName + File.separator + "main"));
			}
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final XmlPullParserException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns all changed classes with the corresponding changed methods. If the set of a class is empty, the whole class was changed and all tests using any method of the class need to be
	 * re-evaluated.
	 * 
	 * @return
	 */
	public Map<ChangedEntity, Set<String>> getChanges() {
		final Map<ChangedEntity, Set<String>> changedClassesMethods = new TreeMap<>();
		final List<ChangedEntity> changedClasses = getChangedClasses();
		LOG.debug("Before Cleaning: {}", changedClasses);
		if (lastSourcesFolder.exists()) {
			for (final Iterator<ChangedEntity> clazzIterator = changedClasses.iterator(); clazzIterator.hasNext();) {
				final ChangedEntity clazz = clazzIterator.next();
				final String onlyClassName = clazz.getJavaClazzName().substring(clazz.getJavaClazzName().lastIndexOf(".") + 1);
				final File src;
				if (clazz.getModule().length() > 0) {
					final File moduleFolder = new File(projectFolder, clazz.getModule());
					LOG.debug("Module: {}", clazz.getModule());
					src = new File(moduleFolder, "src");
				} else {
					src = new File(projectFolder, "src");
				}

				LOG.debug("Suche nach {} in {}", clazz.getJavaClazzName(), src);
				try {
					final Iterator<File> newFileIterator = FileUtils.listFiles(src, new WildcardFileFilter(onlyClassName + ".java"), TrueFileFilter.INSTANCE).iterator();
					if (newFileIterator.hasNext()) {
						final File newFile = newFileIterator.next();
						final Iterator<File> oldFileIterator = FileUtils.listFiles(lastSourcesFolder, new WildcardFileFilter(onlyClassName + ".java"), TrueFileFilter.INSTANCE)
								.iterator();
						if (oldFileIterator.hasNext()) {
							final File oldFile = oldFileIterator.next();
							LOG.info("Vergleiche {}", newFile, oldFile);
							if (newFile.exists()) {
								final ClazzChangeData changeData = FileComparisonUtil.getChangedMethods(newFile, oldFile);
								if (!changeData.isChange()) {
									clazzIterator.remove();
									LOG.debug("Dateien gleich: {}", clazz);
								} else {
									if (changeData.isOnlyMethodChange()) {
										changedClassesMethods.put(clazz, changeData.getChangedMethods());
									} else {
										changedClassesMethods.put(clazz, new HashSet<>());
									}
								}
							}
						} else {
							LOG.info("Class did not exist before: {}", clazz);
							changedClassesMethods.put(clazz, new HashSet<>());
						}
					}

				} catch (final ParseException pe) {
					LOG.info("Class is unparsable for java parser, so to be sure it is added to the changed classes: {}", clazz);
					changedClassesMethods.put(clazz, new HashSet<>());
					pe.printStackTrace();
				} catch (final IOException e) {
					LOG.info("Class is unparsable for java parser, so to be sure it is added to the changed classes: {}", clazz);
					changedClassesMethods.put(clazz, new HashSet<>());
					e.printStackTrace();
				}
			}
		} else {
			LOG.info("Kein Ordner für alte Dateien vorhanden");
		}
		LOG.debug("Nach dem Bereinigen: {}", changedClassesMethods);

		return changedClassesMethods;
	}

}
