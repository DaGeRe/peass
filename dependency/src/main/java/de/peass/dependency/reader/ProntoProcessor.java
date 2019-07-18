package de.peass.dependency.reader;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.github.javaparser.ParseProblemException;

import de.peass.dependency.ChangeManager;
import de.peass.dependency.KiekerResultManager;
import de.peass.vcs.VersionIterator;
/**
 * Creates all, dependencyfile, executionfile and view-diff, by generating a trace and analysing it afterwards.
 * @author reichelt
 *
 */
public class ProntoProcessor extends DependencyReader {

	private static final Logger LOG = LogManager.getLogger(ProntoProcessor.class);

	public ProntoProcessor(final File projectFolder, final File dependencyFile, final String url, final VersionIterator iterator) {
		super(projectFolder, dependencyFile, url, iterator, 5000, VersionKeeper.INSTANCE, VersionKeeper.INSTANCE);
	}

	@Override
	public boolean readDependencies() {
		try {
			if (!init) {
				if (!readInitialVersion()) {
					return false;
				}
			}

			LOG.debug("Analysiere {} Einträge", iterator.getSize());

			int overallSize = 0, prunedSize = 0;
			prunedSize += dependencyManager.getDependencyMap().size();

			final ChangeManager changeManager = new ChangeManager(folders);
			changeManager.saveOldClasses();
			while (iterator.hasNextCommit()) {
				iterator.goToNextCommit();

				try {
					final int tests = analyseVersion(changeManager);
					DependencyReaderUtil.write(dependencyResult, dependencyFile);
					overallSize += dependencyManager.getDependencyMap().size();
					prunedSize += tests;
					
					LOG.info("Overall-tests: {} Executed tests with pruning: {}", overallSize, prunedSize);

	            dependencyManager.getExecutor().deleteTemporaryFiles();
	            
	            final File xmlTraceFolder = KiekerResultManager.getXMLFileFolder(folders, folders.getProjectFolder());
	            final File currentTraceFolder = new File(folders.getTempProjectFolder(), iterator.getTag().substring(0, 6));
	            
	            FileUtils.moveDirectory(xmlTraceFolder, currentTraceFolder);
				} catch (final ParseProblemException ppe) {
					ppe.printStackTrace();
				} catch (final XmlPullParserException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
            }
			}

			LOG.debug("Finished dependency-reading");

		} catch (IOException | InterruptedException | XmlPullParserException e) {
			e.printStackTrace();
		}
		return true;
	}
}
