package de.peass.ci;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.reader.DependencyReader;
import de.peass.dependency.reader.VersionKeeper;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.Constants;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionIterator;

public class ContinuousDependencyReader {

   private static final Logger LOG = LogManager.getLogger(ContinuousDependencyReader.class);

   private final String version;
   private final File projectFolder, dependencyFile;

   public ContinuousDependencyReader(String version, File projectFolder, File dependencyFile) {
      this.version = version;
      this.projectFolder = projectFolder;
      this.dependencyFile = dependencyFile;
   }

   Dependencies getDependencies(VersionIterator iterator)
         throws JAXBException, JsonParseException, JsonMappingException, IOException, InterruptedException, XmlPullParserException {
      Dependencies dependencies;

      final String url = GitUtils.getURL(projectFolder);
      
      boolean needToLoad = false;

      final VersionKeeper nonRunning = new VersionKeeper(new File(dependencyFile.getParentFile(), "nonRunning_" + projectFolder.getName() + ".json"));
      final VersionKeeper nonChanges = new VersionKeeper(new File(dependencyFile.getParentFile(), "nonChanges_" + projectFolder.getName() + ".json"));

      if (!dependencyFile.exists()) {
         dependencies = fullyLoadDependencies(url, iterator, nonChanges);
      } else {
         dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
         VersionComparator.setDependencies(dependencies);

         if (dependencies.getVersions().size() > 0) {
            final String versionName = dependencies.getVersionNames()[dependencies.getVersions().size() - 1];
            if (!versionName.equals(version)) {
               needToLoad = true;
            }
         } else {
            needToLoad = true;
         }
         if (needToLoad) {
            // TODO Continuous Dependency Reading
         }
      }

      return dependencies;
   }

   

   private Dependencies fullyLoadDependencies(final String url, final VersionIterator iterator, final VersionKeeper nonChanges)
         throws IOException, InterruptedException, XmlPullParserException, JsonParseException, JsonMappingException {
      Dependencies dependencies;
      final DependencyReader reader = new DependencyReader(projectFolder, dependencyFile, url, iterator, 10, nonChanges);
      iterator.goToPreviousCommit();
      if (!reader.readInitialVersion()) {
         LOG.error("Analyzing first version was not possible");
      } else {
         reader.readDependencies();
      }
      dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
      return dependencies;
   }
}
